package pt.unl.fct.di.adc.firstwebapp.util;

public enum ErrorCode {

    INVALID_CREDENTIALS("9900"),
    USER_ALREADY_EXISTS("9901"),
    USER_NOT_FOUND("9902"),
    INVALID_TOKEN("9903"),
    TOKEN_EXPIRED("9904"),
    UNAUTHORIZED("9905"),
    INVALID_INPUT("9906"),
    FORBIDDEN("9907");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
