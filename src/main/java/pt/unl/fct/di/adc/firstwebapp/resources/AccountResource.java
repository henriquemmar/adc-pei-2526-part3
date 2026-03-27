package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import com.google.gson.GsonBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import jakarta.ws.rs.Produces;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.*;
import pt.unl.fct.di.adc.firstwebapp.util.data.input_data.ChangePasswordData;
import pt.unl.fct.di.adc.firstwebapp.util.data.output_data.MessageData;
import pt.unl.fct.di.adc.firstwebapp.util.data.input_data.ModifyAccountData;

@Path("/")
public class AccountResource {
    private static final String MESSAGE_SUCCESS_PASSWORD_CHANGE = "Password changed successfully";
    private static final String MESSAGE_SUCCESS_UPDATE = "Updated successfully";
    private static final Logger LOG = Logger.getLogger(AccountResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private final Gson g = new GsonBuilder().setPrettyPrinting().create();
    @POST
    @Path("/modaccount")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response modifyAccount(RequestWrapper<ModifyAccountData> request) {

        if (request == null || !request.isInputValid() || !request.isTokenValid() || !request.input.isUsernameValid()
                || ((request.input.phone == null || request.input.phone.isBlank()) &&
                (request.input.address == null || request.input.address.isBlank()))) {
            return ErrorHandler.error(g, ErrorCode.INVALID_INPUT);
        }

        Response error = TokenValidator.validateToken(request.token);
        if (error != null) return error;

        Entity token = TokenValidator.getToken(request.token);

        String requesterUsername = token.getString("username");

        String requesterRole = token.getString("role");

        Key targetUserKey = userKeyFactory.newKey(request.input.username);
        Entity targetUser = datastore.get(targetUserKey);

        if (targetUser == null) {
            return ErrorHandler.error(g, ErrorCode.USER_NOT_FOUND);
        }

        if (!canModifyAccount(requesterUsername, requesterRole, targetUser)) {
            return ErrorHandler.error(g, ErrorCode.UNAUTHORIZED);
        }

        Transaction txn = datastore.newTransaction();
        try {
            Entity freshTargetUser = txn.get(targetUserKey);

            if (freshTargetUser == null) {
                txn.rollback();
                return ErrorHandler.error(g, ErrorCode.USER_NOT_FOUND);
            }

            Entity.Builder builder = Entity.newBuilder(freshTargetUser);

            if (request.input.phone != null && !request.input.phone.isBlank()) {
                builder.set("phone", request.input.phone);
            }

            if (request.input.address != null && !request.input.address.isBlank()) {
                builder.set("address", request.input.address);
            }

            txn.put(builder.build());
            txn.commit();

            LOG.info("Account modified: " + request.input.username + " by " + requesterUsername);

            ResponseWrapper<MessageData> response = new ResponseWrapper<>(new MessageData(MESSAGE_SUCCESS_UPDATE));

            return Response.ok(g.toJson(response)).build();

        } catch (Exception e) {
            LOG.severe("Error modifying account: " + e.getMessage());
            return ErrorHandler.error(g, ErrorCode.FORBIDDEN);
        } finally {
            if(txn.isActive()){
                txn.rollback();
            }
        }
    }
    @POST
    @Path("/changeuserpwd")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeUserPassword(RequestWrapper<ChangePasswordData> request) {

        if (request == null || !request.isInputValid() || !request.isTokenValid() ||
                !request.input.isDataValid()) {
            return ErrorHandler.error(g, ErrorCode.INVALID_INPUT);
        }
        Transaction txn = datastore.newTransaction();
        try {
            Response error = TokenValidator.validateToken(request.token);
            if (error != null) return error;

            Entity token = TokenValidator.getToken(request.token);
            String tokenUsername = token.getString("username");

            if (!tokenUsername.equals(request.input.username)) {
                return ErrorHandler.error(g, ErrorCode.UNAUTHORIZED);
            }

            Key userKey = userKeyFactory.newKey(request.input.username);
            Entity user = txn.get(userKey);

            if (user == null) {
                txn.rollback();
                return ErrorHandler.error(g, ErrorCode.USER_NOT_FOUND);
            }

            String storedPasswordHash = user.getString("password_hash");
            String oldPasswordHash = DigestUtils.sha512Hex(request.input.oldPassword);

            if (!storedPasswordHash.equals(oldPasswordHash)) {
                return ErrorHandler.error(g, ErrorCode.INVALID_CREDENTIALS);
            }

            if (request.input.oldPassword.equals(request.input.newPassword)) {
                return ErrorHandler.error(g, ErrorCode.INVALID_INPUT);
            }

            Entity updatedUser = Entity.newBuilder(user)
                    .set("password_hash", DigestUtils.sha512Hex(request.input.newPassword))
                    .build();

            txn.update(updatedUser);
            txn.commit();

            ResponseWrapper<MessageData> response = new ResponseWrapper<>(new MessageData(MESSAGE_SUCCESS_PASSWORD_CHANGE));

            return Response.ok(g.toJson(response)).build();

        } catch (Exception e) {
            LOG.severe("Error changing password: " + e.getMessage());
            return ErrorHandler.error(g, ErrorCode.FORBIDDEN);
        } finally {
            if(txn.isActive()){
                txn.rollback();
            }
        }
    }
    private boolean canModifyAccount(String requesterUsername, String requesterRole, Entity targetUser) {

        String targetUsername = targetUser.getString("username");
        String targetRole = targetUser.getString("role");

        if ("ADMIN".equals(requesterRole)) {
            return true;
        }

        if ("USER".equals(requesterRole)) {
            return requesterUsername.equals(targetUsername);
        }

        if ("BOFFICER".equals(requesterRole)) {
            if (requesterUsername.equals(targetUsername)) {
                return true; // pode modificar a própria conta
            }

            return "USER".equals(targetRole); // pode modificar qualquer USER
        }

        return false;
    }
}

