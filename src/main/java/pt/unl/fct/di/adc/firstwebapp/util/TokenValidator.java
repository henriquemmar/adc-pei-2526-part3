package pt.unl.fct.di.adc.firstwebapp.util;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.ws.rs.core.Response;

public class TokenValidator {
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private static final Gson g = new GsonBuilder().setPrettyPrinting().create();
    public TokenValidator(){}

    public static Entity getToken(String tokenId){
        Key tokenKey = tokenKeyFactory.newKey(tokenId);
        return datastore.get(tokenKey);
    }

    public static Response validateRoles(Entity token, UserRole... roles) {

        String tokenRole = token.getString("role");

        for (UserRole role : roles) {
            if (role.name().equals(tokenRole)) {
                return null;
            }
        }

        return ErrorHandler.error(g, ErrorCode.UNAUTHORIZED);
    }

    public static Response validateToken(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return ErrorHandler.error(g, ErrorCode.INVALID_INPUT);
        }
        Entity token = getToken(tokenId);

        if (token == null) {
            return ErrorHandler.error(g, ErrorCode.INVALID_TOKEN);
        }
        long expiresAt = token.getLong("expiresAt");
        long now = System.currentTimeMillis() / 1000;

        if (expiresAt < now) {
            return ErrorHandler.error(g, ErrorCode.TOKEN_EXPIRED);
        }

        return null;
    }

    public static Response validateTokenAndCheckRoles(String tokenId,UserRole... roles){

        Response error = TokenValidator.validateToken(tokenId);
        if (error != null) return error;

        Entity token = TokenValidator.getToken(tokenId);

        error = TokenValidator.validateRoles(token, roles);
        if (error != null) return error;

        return null;
    }
}
