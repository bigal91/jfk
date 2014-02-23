package model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class User {
	private String userName;
	
	private String userPassword;
	
	private String userMail;

	public User(String userName, String userPassword, String userMail){
		this.userName = userName;
		this.userPassword = userPassword;
		this.userMail = userMail;
	}
	
	@Id
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserPassword() {
		return userPassword;
	}

	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}

	public String getUserMail() {
		return userMail;
	}

	public void setUserMail(String userMail) {
		this.userMail = userMail;
	}
}
