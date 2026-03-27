package pt.unl.fct.di.adc.firstwebapp.util.data.output_data;

import pt.unl.fct.di.adc.firstwebapp.util.UserRole;

public class UserRoleData {
    public String username;
    public UserRole role;
    public UserRoleData(String username, UserRole role){
        this.username = username;
        this.role = role;
    }
}
