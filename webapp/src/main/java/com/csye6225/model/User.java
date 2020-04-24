package com.csye6225.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name="userdatatable")
public class User {
	
	@Id 
	@GeneratedValue(generator =  "UUID")
	@GenericGenerator(name="UUID", strategy = "org.hibernate.id.UUIDGenerator")
	private String id;
	

	@JsonProperty("first_name")
	private String firstname;
	
	
	@JsonProperty("last_name")
	private String lastname;
	
	
	@JsonProperty("email_address")
	private String email;
	
	
	@JsonInclude(Include.NON_NULL)
	private String password;
	
	@JsonProperty("account_created")
	private String account_created;
	
	@JsonProperty("account_updated")
	private String account_updated;

	public User(String id, String email, String password) {
		this.id=id;
		this.email=email;
		this.password=password;
		
	}
	
	public User() {}
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAccount_created() {
		return account_created;
	}

	public void setAccount_created(String account_created) {
		this.account_created = account_created;
	}

	public String getAccount_updated() {
		return account_updated;
	}

	public void setAccount_updated(String account_updated) {
		this.account_updated = account_updated;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", first_name=" + firstname + ", last_name=" + lastname + ", email_address=" + email
				+ ", account_created=" + account_created + ", account_updated=" + account_updated + "]";
	}
	
	
	

}



