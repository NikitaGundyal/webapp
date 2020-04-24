package com.csye6225.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

import org.hibernate.annotations.GenericGenerator;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name="billdatatable")
public class Bill {
	
	@Id 
	@GeneratedValue(generator =  "UUID")
	@GenericGenerator(name="UUID", strategy = "org.hibernate.id.UUIDGenerator")
	private String id;
	
	@JsonProperty("created_ts")
	private String created_ts;
	
	@JsonProperty("updated_ts")
    private String updated_ts;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    @JsonIgnore
    private User user;
	
	

	@JsonProperty("owner_id")
    private String owner_id;
    
   
    @JsonProperty("vendor")
    private String vendor;
    
    
    @JsonProperty("bill_date")
    private String bill_date;
    
    
    @JsonProperty("amount_due")
    private double amount_due;
    
    
    
    @JsonProperty("due_date")
    private String due_date;
    
    
    @ElementCollection
    @JsonProperty("categories")
    private Set<String> categories;// = new ArrayList<String>();
    
   
    @JsonProperty("paymentStatus")
    @Enumerated(EnumType.STRING)
    private PaymentType paymentStatus;

    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name="attachment")
    @JsonProperty("attachment")
    private FileAttachment file;
    
    

	public FileAttachment getFile() {
		return file;
	}

	public void setFile(FileAttachment file) {
		this.file = file;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCreated_ts() {
		return created_ts;
	}

	public void setCreated_ts(String created_ts) {
		this.created_ts = created_ts;
	}

	public String getUpdated_ts() {
		return updated_ts;
	}

	public void setUpdated_ts(String updated_ts) {
		this.updated_ts = updated_ts;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getBill_date() {
		return bill_date;
	}

	public void setBill_date(String bill_date) {
		this.bill_date = bill_date;
	}

	public double getAmount_due() {
		return amount_due;
	}

	public void setAmount_due(double amount_due) {
		this.amount_due = amount_due;
	}

	public String getDue_date() {
		return due_date;
	}

	public void setDue_date(String due_date) {
		this.due_date = due_date;
	}

	public Set<String> getCategories() {
		return categories;
	}

	public void setCategories(Set<String> categories) {
		this.categories = categories;
	}

	public PaymentType getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(PaymentType paymentStatus) {
		this.paymentStatus = paymentStatus;
	}
    
	public String getOwner_id() {
		return owner_id;
	}

	public void setOwner_id(String owner_id) {
		this.owner_id = owner_id;
	}
    
    
    
}


