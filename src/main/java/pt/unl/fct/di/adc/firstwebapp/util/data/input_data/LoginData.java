package pt.unl.fct.di.adc.firstwebapp.util.data.input_data;

public class LoginData {
	
	public String username;
	public String password;
	
	public LoginData() { }
	
	public LoginData(String username, String password) {
		this.username = username;
		this.password = password;
	}
	public boolean isDataValid(){
		return username != null && !username.isBlank() &&
				password != null && !password.isBlank();
	}
	
}
