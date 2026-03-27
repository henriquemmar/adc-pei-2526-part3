package pt.unl.fct.di.adc.firstwebapp.util.data.output_data;

import pt.unl.fct.di.adc.firstwebapp.util.UserRole;

import java.util.UUID;

public class AuthToken {
	
	public String username;
	public String tokenId;
	public UserRole role;
	public long issuedAt;
	public long expiresAt;
	
	public AuthToken() { }
	
	public AuthToken(String username, UserRole role) {
		this.username = username;
		this.tokenId = UUID.randomUUID().toString();
		this.role = role;
		this.issuedAt =  System.currentTimeMillis() / 1000;
		this.expiresAt = issuedAt + 900; // 15 mins
	}
	
}
