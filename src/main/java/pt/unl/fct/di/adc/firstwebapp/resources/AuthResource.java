package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.ws.rs.Produces;
import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.google.cloud.Timestamp;

import pt.unl.fct.di.adc.firstwebapp.util.*;
import pt.unl.fct.di.adc.firstwebapp.util.data.input_data.LoginData;
import pt.unl.fct.di.adc.firstwebapp.util.data.output_data.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.data.input_data.CreateAccountData;
import pt.unl.fct.di.adc.firstwebapp.util.data.output_data.LoginResponse;
import pt.unl.fct.di.adc.firstwebapp.util.data.output_data.MessageData;
import pt.unl.fct.di.adc.firstwebapp.util.data.output_data.UserRoleData;
import pt.unl.fct.di.adc.firstwebapp.util.data.input_data.UsernameData;

@Path("/")
public class AuthResource {
    private static final String MESSAGE_INVALID_CREDENTIALS = "Incorrect username or password.";
    private static final String MESSAGE_NEXT_PARAMETER_INVALID = "Request parameter 'next' must be greater or equal to 0.";


    private static final String LOG_MESSAGE_LOGIN_ATTEMPT = "Login attempt by user: ";
    private static final String LOG_MESSAGE_LOGIN_SUCCESSFUL = "Login successful by user: ";
    private static final String LOG_MESSAGE_WRONG_PASSWORD = "Wrong password for: ";
    private static final String LOG_MESSAGE_UNKNOW_USER = "Failed login attempt for username: ";

    private static final String USER_PWD = "user_pwd";
    private static final String USER_LOGIN_TIME = "user_login_time";
    private static final Logger LOG = Logger.getLogger(AuthResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private final Gson g = new GsonBuilder().setPrettyPrinting().create();
    @POST
    @Path("/createaccount")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAccount(RequestWrapper<CreateAccountData> request) {

        if(request == null || !request.isInputValid() || request.token != null ||
                !request.input.isValidRegistration() || !request.input.passwordsMatch() ||
                request.input.role == null) {
            return ErrorHandler.error(g, ErrorCode.INVALID_INPUT);
        }

        Transaction txn = datastore.newTransaction();
        try {

            Key userKey = datastore.newKeyFactory().setKind("User").newKey(request.input.username);
            Entity user = txn.get(userKey);

            if(user != null) {
                return ErrorHandler.error(g, ErrorCode.USER_ALREADY_EXISTS);
            }
            else {

                user = Entity.newBuilder(userKey)
                        .set("username", request.input.username)
                        .set("password_hash", DigestUtils.sha512Hex(request.input.password))
                        .set("phone", request.input.phone == null ? "" : request.input.phone)
                        .set("address", request.input.address == null ? "" : request.input.address)
                        .set("role", request.input.role.name().toUpperCase())
                        .set("creation_time", Timestamp.now())
                        .build();

                txn.put(user);
                txn.commit();

                ResponseWrapper<UserRoleData> response = new ResponseWrapper<>(new UserRoleData(request.input.username, request.input.role));

                LOG.info("User registered " + request.input.username);
                return Response.ok(g.toJson(response)).build();

            }
        } catch (Exception e) {
            LOG.severe("Error registering user: " + e.getMessage());
            return ErrorHandler.error(g, ErrorCode.FORBIDDEN);
        } finally{
            if(txn.isActive()){
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response loginUser(RequestWrapper<LoginData> request) {

        if (request == null || request.token != null || !request.isInputValid() || !request.input.isDataValid()) {
            return ErrorHandler.error(g, ErrorCode.INVALID_INPUT);
        }
        try {
            Key userKey = userKeyFactory.newKey(request.input.username);
            Entity user = datastore.get(userKey);

            if (user == null) {
                return ErrorHandler.error(g, ErrorCode.USER_NOT_FOUND);
            }

            String storedPasswordHash = user.getString("password_hash");
            String givenPasswordHash = DigestUtils.sha512Hex(request.input.password);

            if (!storedPasswordHash.equals(givenPasswordHash)) {
                LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + request.input.username);
                return ErrorHandler.error(g, ErrorCode.INVALID_CREDENTIALS);
            }

            AuthToken token = new AuthToken(request.input.username, UserRole.valueOf(user.getString("role")));
            Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(token.tokenId);

            Entity tokenEntity = Entity.newBuilder(tokenKey)
                    .set("tokenId", token.tokenId)
                    .set("username", token.username)
                    .set("role", token.role.name())
                    .set("issuedAt", token.issuedAt)
                    .set("expiresAt", token.expiresAt)
                    .build();

            datastore.put(tokenEntity);

            ResponseWrapper<LoginResponse> response = new ResponseWrapper<>(new LoginResponse(token));

            LOG.info(LOG_MESSAGE_LOGIN_SUCCESSFUL + request.input.username);
            return Response.ok(g.toJson(response)).build();

        } catch (Exception e) {
            LOG.severe(LOG_MESSAGE_LOGIN_ATTEMPT + e.getMessage());
            return ErrorHandler.error(g, ErrorCode.FORBIDDEN);
        }
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(RequestWrapper<UsernameData> request) {

        if (request == null || !request.isInputValid() || !request.isTokenValid()
                || !request.input.isDataValid()) {
            return ErrorHandler.error(g, ErrorCode.INVALID_INPUT);
        }
        Transaction txn = datastore.newTransaction();
        try {
            Response error = TokenValidator.validateToken(request.token);
            if (error != null) return error;

            Entity requesterToken = TokenValidator.getToken(request.token);
            String requesterUsername = requesterToken.getString("username");

            String requesterRole = requesterToken.getString("role");

            if (!UserRole.ADMIN.name().equals(requesterRole) &&
                    !requesterUsername.equals(request.input.username)) {
                return ErrorHandler.error(g, ErrorCode.UNAUTHORIZED);
            }

            deleteUserTokens(txn, request.input.username);
            txn.commit();

            LOG.info("Logout executed for user: " + request.input.username);

            ResponseWrapper<MessageData> response = new ResponseWrapper<>(new MessageData("Logout successful"));

            return Response.ok(g.toJson(response)).build();

        } catch (Exception e) {
            LOG.severe("Error during logout: " + e.getMessage());
            return ErrorHandler.error(g, ErrorCode.FORBIDDEN);
        }
    }
    public static void deleteUserTokens(Transaction txn, String username) {

        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Token")
                .setFilter(StructuredQuery.PropertyFilter.eq("username", username))
                .build();

        QueryResults<Entity> results = datastore.run(query);

        while (results.hasNext()) {
            txn.delete(results.next().getKey());
        }
    }

}
