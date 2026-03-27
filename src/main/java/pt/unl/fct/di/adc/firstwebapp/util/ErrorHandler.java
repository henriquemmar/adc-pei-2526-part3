package pt.unl.fct.di.adc.firstwebapp.util;

import com.google.gson.Gson;
import jakarta.ws.rs.core.Response;

public class ErrorHandler {
    public static Response error(Gson g, ErrorCode code) {
        String message;
        switch (code){
            case INVALID_CREDENTIALS -> message = "The username-password pair is not valid";
            case USER_ALREADY_EXISTS -> message = "Error in creating an account because the username already exists";
            case USER_NOT_FOUND -> message = "The username referred in the operation doesn’t exist in registered accounts";
            case INVALID_TOKEN -> message = "The operation is called with an invalid token (wrong format for example)";
            case TOKEN_EXPIRED -> message = "The operation is called with a token that is expired";
            case UNAUTHORIZED -> message = "The operation is not allowed for the user role";
            case INVALID_INPUT -> message = "The call is using input data not following the correct specification";
            case FORBIDDEN -> message = "The operation generated a forbidden error by other reason";
            default -> message = "Unknown error";
        }
        ErrorResponse error = new ErrorResponse(code.getCode(), message);
        return Response.ok(g.toJson(error)).build();
    }
}
