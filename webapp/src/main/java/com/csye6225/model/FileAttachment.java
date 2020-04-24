
package com.csye6225.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name="filetable")
@ConfigurationProperties
public class FileAttachment {
	
	@Id 
	@GeneratedValue(generator =  "UUID")
	@GenericGenerator(name="UUID", strategy = "org.hibernate.id.UUIDGenerator")
	private String id;
	
	@JsonProperty
	private String file_name;
	
	@JsonProperty
	private String url;
	
	@JsonProperty
	private String upload_date;
	
	@JsonIgnore
	private String bill_id;
	
	@JsonIgnore
	private Long file_size;
			
	@JsonIgnore
	private String lastModified_time;
	
	@JsonIgnore
	private String content_type;
	
	@JsonIgnore
	private String md5Content;
	
	
	
	public String getMd5Content() {
		return md5Content;
	}

	public void setMd5Content(String md5Content) {
		this.md5Content = md5Content;
	}

	public String getContent_type() {
		return content_type;
	}

	public void setContent_type(String content_type) {
		this.content_type = content_type;
	}

	public String getLastModified_time() {
		return lastModified_time;
	}

	public void setLastModified_time(String lastModified_time) {
		this.lastModified_time = lastModified_time;
	}

	

	public String getBill_id() {
		return bill_id;
	}

	public void setBill_id(String bill_id) {
		this.bill_id = bill_id;
	}

	public Long getFile_size() {
		return file_size;
	}

	public void setFile_size(Long file_size) {
		this.file_size = file_size;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFile_name() {
		return file_name;
	}

	public void setFile_name(String file_name) {
		this.file_name = file_name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUpload_date() {
		return upload_date;
	}

	public void setUpload_date(String upload_date) {
		this.upload_date = upload_date;
	}
	
	
	
	
	

}
