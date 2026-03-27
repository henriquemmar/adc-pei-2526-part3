package pt.unl.fct.di.adc.firstwebapp.util.data.input_data;

import pt.unl.fct.di.adc.firstwebapp.util.UserRole;

public class ChangeRoleData {
    public String username;
    public UserRole newRole;
    public boolean isDataValid(){
        return username != null && !username.isBlank() &&
                newRole != null && !newRole.name().isBlank();
    }
}
