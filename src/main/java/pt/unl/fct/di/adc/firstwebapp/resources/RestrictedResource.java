package pt.unl.fct.di.adc.firstwebapp.resources;

import com.google.gson.GsonBuilder;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.*;
import pt.unl.fct.di.adc.firstwebapp.util.data.output_data.UserInfo;
import pt.unl.fct.di.adc.firstwebapp.util.data.input_data.ChangeRoleData;
import pt.unl.fct.di.adc.firstwebapp.util.data.input_data.EmptyInput;
import pt.unl.fct.di.adc.firstwebapp.util.data.output_data.*;
import pt.unl.fct.di.adc.firstwebapp.util.data.input_data.UsernameData;

@Path("/")
public class RestrictedResource {
    private static final Logger LOG = Logger.getLogger(RestrictedResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private final Gson g = new GsonBuilder().setPrettyPrinting().create();
    @POST
    @Path("/showusers")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response showUsers(RequestWrapper<EmptyInput> request) {

        if (request == null || !request.isInputValid() || !request.isTokenValid()) {
            return ErrorHandler.error(g, ErrorCode.INVALID_INPUT);
        }

        try {
            Response error = TokenValidator.validateTokenAndCheckRoles(request.token, UserRole.ADMIN, UserRole.BOFFICER);
            if (error != null) return error;

            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .build();

            QueryResults<Entity> results = datastore.run(query);

            List<UserInfo> users = new ArrayList<>();
            while (results.hasNext()) {
                Entity user = results.next();

                String username = user.getString("username");
                String userRole = user.getString("role");

                users.add(new UserInfo(username, userRole));
            }

            ResponseWrapper<UsersData> response =
                    new ResponseWrapper<>(new UsersData(users));

            LOG.info("Admin listed all users.");
            return Response.ok(g.toJson(response)).build();

        } catch (Exception e) {
            LOG.severe("Error showing users: " + e.getMessage());
            return ErrorHandler.error(g, ErrorCode.FORBIDDEN);
        }
    }
    @POST
    @Path("/deleteaccount")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAccount(RequestWrapper<UsernameData> request) {

        if (request == null || !request.isInputValid() || !request.isTokenValid() || !request.input.isDataValid()) {
            return ErrorHandler.error(g, ErrorCode.INVALID_INPUT);
        }
        Transaction txn = datastore.newTransaction();
        try{
            Response error = TokenValidator.validateTokenAndCheckRoles(request.token, UserRole.ADMIN);
            if (error != null) return error;

            Key userKey = userKeyFactory.newKey(request.input.username);
            Entity user = txn.get(userKey);


            if (user == null) {
                txn.rollback();
                return ErrorHandler.error(g, ErrorCode.USER_NOT_FOUND);
            }

            AuthResource.deleteUserTokens(txn, request.input.username);

            txn.delete(userKey);
            txn.commit();

            ResponseWrapper<MessageData> response =
                    new ResponseWrapper<>(new MessageData("Account deleted successfully"));

            LOG.info("Account deleted: " + request.input.username);
            return Response.ok(g.toJson(response)).build();

        }catch (Exception e) {
            LOG.severe("Error deleting account." + e.getMessage());
            return ErrorHandler.error(g, ErrorCode.FORBIDDEN);
        } finally {
            if(txn.isActive()){
                txn.rollback();
            }
        }
    }
    @POST
    @Path("/showauthsessions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response showAuthSessions(RequestWrapper<EmptyInput> request) {
        if (request == null || !request.isInputValid() || !request.isTokenValid()) {
            return ErrorHandler.error(g, ErrorCode.INVALID_INPUT);
        }

        try {
            Response error = TokenValidator.validateTokenAndCheckRoles(request.token, UserRole.ADMIN, UserRole.BOFFICER);
            if (error != null) return error;

            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("Token")
                    .build();

            QueryResults<Entity> results = datastore.run(query);

            List<ShowAuthSessionsData> authSessions = new ArrayList<>();
            while (results.hasNext()) {
                Entity authSession = results.next();

                String tokenId = authSession.getString("tokenId");
                String username = authSession.getString("username");

                String role = authSession.getString("role");

                long expiresAt = authSession.getLong("expiresAt");

                authSessions.add(new ShowAuthSessionsData(tokenId, username, role, expiresAt));
            }

            ResponseWrapper<SessionsData> response =
                    new ResponseWrapper<>(new SessionsData(authSessions));

            LOG.info("Admin listed all authenticated sessions.");
            return Response.ok(g.toJson(response)).build();

        } catch (Exception e) {
            LOG.severe("Error showing sessions: " + e.getMessage());
            return ErrorHandler.error(g, ErrorCode.FORBIDDEN);
        }
    }

    @POST
    @Path("/showuserrole")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response showUserRole(RequestWrapper<UsernameData> request){

        if (request == null || !request.isInputValid() || !request.input.isDataValid() || !request.isTokenValid()) {

            return ErrorHandler.error(g, ErrorCode.INVALID_INPUT);

        }
        try{
            Response error = TokenValidator.validateTokenAndCheckRoles(request.token, UserRole.ADMIN, UserRole.BOFFICER);
            if (error != null) return error;

            Key userKey = userKeyFactory.newKey(request.input.username);
            Entity user = datastore.get(userKey);

            if (user == null) {
                return ErrorHandler.error(g, ErrorCode.USER_NOT_FOUND);
            }

            UserRole userRole = UserRole.valueOf(user.getString("role"));

            ResponseWrapper<UserRoleData> response =
                    new ResponseWrapper<>(new UserRoleData(request.input.username, userRole));

            LOG.info("User role retrieved for: " + request.input.username);

            return Response.ok(g.toJson(response)).build();

        } catch (Exception e) {
            LOG.severe("Error retrieving user role: " + e.getMessage());
            return ErrorHandler.error(g, ErrorCode.FORBIDDEN);
        }
    }
    @POST
    @Path("/changeuserrole")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeUserRole(RequestWrapper<ChangeRoleData> request) {

        if (request == null || !request.isTokenValid() || !request.isInputValid() ||
                !request.input.isDataValid()) {
            return ErrorHandler.error(g, ErrorCode.INVALID_INPUT);
        }

        Transaction txn = datastore.newTransaction();

        try {
            Response error = TokenValidator.validateTokenAndCheckRoles(request.token, UserRole.ADMIN);
            if (error != null) return error;

            Key userKey = userKeyFactory.newKey(request.input.username);
            Entity user = txn.get(userKey);

            if (user == null) {
                txn.rollback();
                return ErrorHandler.error(g, ErrorCode.USER_NOT_FOUND);
            }
            if (request.input.newRole != UserRole.USER &&
                    request.input.newRole != UserRole.BOFFICER &&
                    request.input.newRole != UserRole.ADMIN) {
                txn.rollback();
                return ErrorHandler.error(g, ErrorCode.INVALID_INPUT);
            }

            Entity updatedUser = Entity.newBuilder(user)
                    .set("role", request.input.newRole.name())
                    .build();

            txn.update(updatedUser);
            updateUserTokensRole(txn, request.input.username, request.input.newRole);
            txn.commit();

            ResponseWrapper<MessageData> response =
                    new ResponseWrapper<>(new MessageData("User role updated successfully."));

            LOG.info("Role changed for user " + request.input.username + " to " + request.input.newRole.name());

            return Response.ok(g.toJson(response)).build();

        } catch (Exception e) {
            LOG.severe("Error changing user role: " + e.getMessage());
            return ErrorHandler.error(g, ErrorCode.FORBIDDEN);
        } finally {
            if(txn.isActive()){
                txn.rollback();
            }
        }
    }
    public static void updateUserTokensRole(Transaction txn, String username, UserRole newRole) {

        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Token")
                .setFilter(StructuredQuery.PropertyFilter.eq("username", username))
                .build();

        QueryResults<Entity> results = datastore.run(query);

        while (results.hasNext()) {
            Entity token = results.next();

            Entity updatedToken = Entity.newBuilder(token)
                    .set("role", newRole.name())
                    .build();

            txn.update(updatedToken);
        }
    }

}
