package pt.unl.fct.di.adc.firstwebapp.util.data.input_data;

public class UsernameData {
    public String username;
    public boolean isDataValid(){
        return username != null && !username.isBlank();
    }
}
