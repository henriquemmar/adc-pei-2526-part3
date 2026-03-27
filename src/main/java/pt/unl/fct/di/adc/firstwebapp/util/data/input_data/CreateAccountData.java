package pt.unl.fct.di.adc.firstwebapp.util.data.input_data;

import pt.unl.fct.di.adc.firstwebapp.util.UserRole;

import java.util.Objects;

public class CreateAccountData {
    public String username;
    public String password;
    public String confirmation;
    public String phone;
    public UserRole role;
    public String address;
    public boolean isValidRegistration() {
        return username != null && password != null && role != null &&
                !username.isBlank() && !password.isBlank() && confirmation != null && !confirmation.isBlank();
    }
    public boolean passwordsMatch() {
        return Objects.equals(password, confirmation);
    }
}