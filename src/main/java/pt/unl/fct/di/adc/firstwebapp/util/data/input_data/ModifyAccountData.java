package pt.unl.fct.di.adc.firstwebapp.util.data.input_data;

public class ModifyAccountData {
    public String username;
    public String phone;
    public String address;
    public boolean isUsernameValid(){
        return username != null && !username.isBlank();
    }
}
