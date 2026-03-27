package pt.unl.fct.di.adc.firstwebapp.util.data.output_data;

public class ShowAuthSessionsData {
    public String tokenID;
    public String username;
    public String role;
    public int expiresAt;

    public ShowAuthSessionsData(String tokenID, String username, String role, int expiresAt) {
        this.tokenID = tokenID;
        this.username = username;
        this.role = role;
        this.expiresAt = expiresAt;
    }
}
