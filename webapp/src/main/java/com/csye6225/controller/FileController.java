package com.csye6225.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.csye6225.model.Bill;
import com.csye6225.model.FileAttachment;
import com.csye6225.model.User;
import com.csye6225.repository.BillRepository;
import com.csye6225.repository.FileRepository;
import com.csye6225.repository.UserRepository;
import com.fasterxml.jackson.databind.introspect.TypeResolutionContext.Basic;
import com.google.gson.Gson;


@RestController
@Profile("default")
public class FileController {
	
	@Autowired
	BillRepository billRepository;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	FileRepository fileRepository;
	
	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;
	
	private static final String USER_HOME=System.getProperty("user.home");
	private static final String USER_DIR=System.getProperty("user.dir");
	
	@SuppressWarnings("unused")
	@PostMapping(path ="/v1/bill/{id}/file", produces = "application/json")
	public ResponseEntity<?> uploadFile(HttpServletRequest request,@PathVariable(value = "id") String billId,@RequestParam("file") MultipartFile file) throws Exception{
		System.out.println("Inside POST - FILE");
		String token = request.getHeader("Authorization");
	    System.out.println("User Directory: "+USER_DIR);
		 System.out.println("Token is :"+token);
		 Gson gson = new Gson();
		 
		 
		 if(token!=null)
		 {
			 token= token.replaceFirst("Basic ", "");
			 System.out.println("Token with basic is :"+token);
			 byte[] credDecoded=Base64.getDecoder().decode(token);
			 String credentials=new String(credDecoded,StandardCharsets.UTF_8);
			 System.out.println("Credentials: "+ credentials);
			 String[] userDetails=credentials.split(":",2);
			 String emailId = userDetails[0];
			 System.out.println("Email id: "+emailId);
			 User user = userRepository.findByemail(emailId);
			 String password = userDetails[1];
			 System.out.println("Password from url: "+password);
			 String json = gson.toJson(user);
	
			 if(user==null) {
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The user with emailid: "+emailId+" does not exist. Try again");
			 }
			 else if (user!=null && !bCryptPasswordEncoder.matches(password, user.getPassword())) {
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The password is incorrect. Try again");
			 }
			 else 
			 {
				 UUID uuid = null;
				 try {
					uuid = UUID.fromString(billId);
				 }
				 catch(Exception e) {
					 
					 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id not found. Enter correct bill id");
				 }
				 Bill b = new Bill(); 
				  
				 List<Bill> bills = billRepository.findByUserId(user.getId());
				 
				
				 if(bills.size() > 0) 
				 {
					 Optional<Bill> optionalBill = billRepository.findById(billId);
					 if(optionalBill.isPresent()) {
						 b = optionalBill.get();
						 //System.out.println("Bill is :"+b);
					 }
					 if(b==null) 
					 {
						 
						 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id is not present");
						 
					 }
					 if(bills.contains(b)) 
					 {
						
						 if(!(b.getFile()!=null)) 
						 {   
							 if(!file.isEmpty())
							 {
								 if(file.getContentType().contains("image/png") || file.getContentType().contains("image/jpg") || file.getContentType().contains("image/jpeg") || file.getContentType().contains("application/pdf"))
								 {
									 String path = USER_HOME + "/Uploads/files/";
									 String fileName = file.getOriginalFilename();
									 String filePath = path+fileName;
									 FileAttachment new_file = new FileAttachment();
									 new_file.setFile_name(fileName);
									 SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd"); 
									 Date upload_date = new Date();
								     new_file.setUpload_date(formatter.format(upload_date).toString());
								     String url = filePath+"_"+b.getId();
								     new_file.setUrl(url);
								     new_file.setBill_id(billId);
								     try {
								    	 File f = new File(filePath+"_"+b.getId());
								         FileOutputStream fos = new FileOutputStream(f);
								         fos.write(file.getBytes());
								         fos.close();
									} 
								        catch (Exception e) {
										System.out.println("Error in uploading");
										e.printStackTrace();
										return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error in uploading file. Key file should be present");
										
									}
								     Path file_path = Paths.get(url);
								     BasicFileAttributes attributes = Files.readAttributes(file_path,BasicFileAttributes.class);
								     UserPrincipal owner=Files.getOwner(file_path);
								     SimpleDateFormat formatterdate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); 
								    	String creation_time = formatterdate.format(attributes.creationTime().toMillis());
								    	String lastAccess_time = formatterdate.format(attributes.lastAccessTime().toMillis());
								    	String lastModified_time = formatterdate.format(attributes.lastModifiedTime().toMillis());
								    	Long size= attributes.size();
								    	MessageDigest md = MessageDigest.getInstance("MD5");
									    String new_FilePath = url;
									    byte[] hashInBytes = checksum(new_FilePath, md);
									    String hashedFile=bytesToHex(hashInBytes);
									    new_file.setLastModified_time(lastModified_time);
									    new_file.setContent_type(file.getContentType());
								     fileRepository.save(new_file);
								     b.setFile(new_file);
								     billRepository.save(b);
								     return ResponseEntity.status(HttpStatus.CREATED).body(new_file);
								 }
								 
								 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File Attachment should be of type png, jpg, jpeg or pdf only");
							 }
							 
							 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Attachment cannot be null. Please select one");
							 
						 }
						 
						 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Attachment already exists");
						   
						 
					 }
                     else  {
						 
						 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You cannot view any other user's bill information");
					 }
 
				 }
				 			   
				  return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id not found");
			 }
				 
	   }
		 
		 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
}
	
	
	   
	   @GetMapping(path = "/v1/bill/{billId}/file/{fileId}", produces = "application/json")
	   public ResponseEntity<?> getUploadedFile(HttpServletRequest request, @PathVariable(value = "billId") String billId, @PathVariable(value = "fileId") String fileId){
		   System.out.println("Inside GET - FILE");
		   String token = request.getHeader("Authorization");
		   System.out.println("Token is :"+token);
		   Gson gson = new Gson(); 
		   if(token!=null) {
				 token= token.replaceFirst("Basic ", "");
				 System.out.println("Token with basic is :"+token);
				 byte[] credDecoded=Base64.getDecoder().decode(token);
				 String credentials=new String(credDecoded,StandardCharsets.UTF_8);
				 System.out.println("Credentials: "+ credentials);
				 String[] userDetails=credentials.split(":",2);
				 String emailId = userDetails[0];
				 System.out.println("Email id: "+emailId);
				 User user = userRepository.findByemail(emailId);
				 String password = userDetails[1];
				 System.out.println("Password from url: "+password);
				 String json = gson.toJson(user);
		
				 if(user==null) {
					 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The user with emailid:"+emailId+" does not exist. Try again");
				 }
				 else if (user!=null && !bCryptPasswordEncoder.matches(password, user.getPassword())) {
					 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The password is incorrect. Try again");
				 }
				 
				 else {
					   

					 UUID uuid = null;
					 try {
						uuid = UUID.fromString(billId);
					 }
					 catch(Exception e) {
						 
						 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id not found. Enter correct bill id");
					 }
					 Bill b = new Bill(); 
					 FileAttachment f = new FileAttachment();
					 List<Bill> bills = billRepository.findByUserId(user.getId());
					 
					
					 if(bills.size() > 0) 
					 {
						 Optional<Bill> optionalBill = billRepository.findById(billId);
						 if(optionalBill.isPresent()) {
							 b = optionalBill.get();
							 //System.out.println("Bill is :"+b);
						 }
						 if(b==null) 
						 {
							 
							 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id is not present");
							 
						 }
						 if(bills.contains(b)) 
						 {
							  Optional<FileAttachment> optionalFile = fileRepository.findById(fileId);
							  if(!optionalFile.isPresent()) {
								  return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified file id is not present");
							  }
							  f =optionalFile.get();
							  
							  try 
							  {
								  if(b.getFile().getId().equals(f.getId()) ) {
									  return ResponseEntity.status(HttpStatus.OK).body(f);
								  }
							  }catch(Exception e) {
								  return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The file does not exist");
							  }	 
							  return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The file specified does not belong to the bill");
						 }
	                     else  {
							 
							 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You cannot view any other user's bill information");
						 }
	 
					 }
					 			   
					  return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id not found");

				 }
			 }
			 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
		   
	   }
	   
	   
	   @DeleteMapping(path = "/v1/bill/{billId}/file/{fileId}", produces = "application/json")
		public ResponseEntity<?> deleteBill(HttpServletRequest request, @PathVariable(value = "billId") String billId, @PathVariable(value = "fileId") String fileId){
			System.out.println("Inside DELETE - FILE");
			String token = request.getHeader("Authorization");
			 if(token!=null) {
				 token= token.replaceFirst("Basic ", "");
				 String decodedBase64String = new String(Base64.getDecoder().decode(token.getBytes()));
				 String[] userDetails = decodedBase64String.split(":",2);
				 String emailId = userDetails[0];
				 User user = userRepository.findByemail(emailId);
				 String password = userDetails[1];
				 if(user==null) {
					 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The user with emailid:"+emailId+" does not exist. Try again");
				 }
				 else if (user!=null && !bCryptPasswordEncoder.matches(password, user.getPassword())) {
					 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The password is incorrect. Try again");
				 }
				 else {
					 
					 UUID uuid = null;
					 try {
						uuid = UUID.fromString(billId);
					 }
					 catch(Exception e) {
						 
						 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id not found. Enter correct bill id");
					 }
					
					 Bill b = new Bill(); 
					 FileAttachment f = new FileAttachment();
					 List<Bill> bills = billRepository.findByUserId(user.getId());
					 
					 
					 if(bills.size() > 0) 
					 {
						 
						 Optional<Bill> optionalBill = billRepository.findById(billId);
						 if(optionalBill.isPresent()) {
							 b = optionalBill.get();
						 }
						 if(b==null) 
						 {
							 
							 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id is not present");
							 
						 }
						 if(bills.contains(b)) 
						 {
							 Optional<FileAttachment> optionalFile = fileRepository.findById(fileId);
							  if(!optionalFile.isPresent()) {
								  return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified file id is not present");
							  }
							  f = optionalFile.get();
							  try 
							  {
								if(b.getFile().getId().equals(fileId))
								 {
									File new_file = new File(f.getUrl());
									new_file.delete();
									b.setFile(null);
									fileRepository.delete(f);
									return ResponseEntity.status(HttpStatus.NO_CONTENT).body("The file attachment is deleted"); 
								}
							}catch(Exception e) {
								
								return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The file does not exist");
							}
							
						   
							 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The file specified does not belong to the bill");
							   
						 }
	                     else  {
							 
							 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You cannot delete any other user's bill information");
						 }
						 
					 }
					 
					 else  {
						 
						 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id is not present");
					 }
					
						 	 
			      }
			 }
			 
			 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
			
		}
	   
	   
	   private static byte[] checksum(String filepath, MessageDigest md) throws IOException {

	        try (DigestInputStream dis = new DigestInputStream(new FileInputStream(filepath), md)) {
	            while (dis.read() != -1) ; //empty loop to clear the data
	            md = dis.getMessageDigest();
	        }

	        return md.digest();

	    }

	    private static String bytesToHex(byte[] hashInBytes) {

	        StringBuilder sb = new StringBuilder();
	        for (byte b : hashInBytes) {
	            sb.append(String.format("%02x", b));
	        }
	        return sb.toString();

	    }
	    


}

