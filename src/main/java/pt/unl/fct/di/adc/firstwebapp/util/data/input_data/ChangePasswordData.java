package pt.unl.fct.di.adc.firstwebapp.util.data.input_data;

public class ChangePasswordData {
    public String username;
    public String oldPassword;
    public String newPassword;
    public boolean isDataValid(){
        return username != null && !username.isBlank() &&
                oldPassword != null && !oldPassword.isBlank() &&
                newPassword != null && !newPassword.isBlank();
    }

}