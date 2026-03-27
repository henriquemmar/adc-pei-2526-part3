package pt.unl.fct.di.adc.firstwebapp.util.data.output_data;

public class UserInfo {
    public String username;
    public String role;

    public UserInfo(String userId, String role) {
        this.username = userId;
        this.role = role;
    }
}