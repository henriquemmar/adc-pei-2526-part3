package pt.unl.fct.di.adc.firstwebapp.util.data.output_data;

public class LoginResponse {
    public AuthToken token;

    public LoginResponse(AuthToken token) {
        this.token = token;
    }
}
