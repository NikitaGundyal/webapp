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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.storagegateway.model.ObjectACL;
import com.csye6225.model.Bill;
import com.csye6225.model.FileAttachment;
import com.csye6225.model.User;
import com.csye6225.repository.BillRepository;
import com.csye6225.repository.FileRepository;
import com.csye6225.repository.UserRepository;
import com.fasterxml.jackson.databind.introspect.TypeResolutionContext.Basic;
import com.google.gson.Gson;
import com.timgroup.statsd.StatsDClient;


@RestController
@Profile("dev")
public class FileControllerAWSBucket {
	
	@Autowired
	BillRepository billRepository;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	FileRepository fileRepository;
	
	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private StatsDClient statsDClient;
	
	
	private final static Logger logger= LoggerFactory.getLogger(FileControllerAWSBucket.class);
	
	@Value("${bucket.name}")
	String bucketName;
	
	private static final String USER_HOME=System.getProperty("user.home");
	private static final String USER_DIR=System.getProperty("user.dir");
    //private String bucketName = System.getProperty("bucket.name");
	private String clientRegion = "us-east-1";
    private String endPointUrl="https://s3.amazonaws.com";
	
	@SuppressWarnings("unused")
	@PostMapping(path ="/v1/bill/{id}/file", produces = "application/json")
	public ResponseEntity<?> uploadFile(HttpServletRequest request,@PathVariable(value = "id") String billId,@RequestParam("file") MultipartFile file) throws Exception{
		statsDClient.incrementCounter("Post_File_API");
		 long startTimePostFileApi =  System.currentTimeMillis();
		logger.info("Inside Post_File_API");
		String token = request.getHeader("Authorization");
		logger.info("User Directory: "+USER_DIR);
		logger.info("User Home: "+USER_HOME);
		logger.info("Bucket Name: "+bucketName);
		Gson gson = new Gson();
		 if(token!=null)
		 {
			 token= token.replaceFirst("Basic ", "");
			 
			 byte[] credDecoded=Base64.getDecoder().decode(token);
			 String credentials=new String(credDecoded,StandardCharsets.UTF_8);
			 String[] userDetails=credentials.split(":",2);
			 String emailId = userDetails[0];
			 User user = userRepository.findByemail(emailId);
			 String password = userDetails[1];
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
									 //String path = USER_DIR + "/Uploads/files/";
									 String fileName = file.getOriginalFilename();
									 String filePath = path+fileName;
									 FileAttachment new_file = new FileAttachment();
									 new_file.setFile_name(fileName);
									 SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd"); 
									 Date upload_date = new Date();
								     new_file.setUpload_date(formatter.format(upload_date).toString());
								     //String url = filePath+"_"+b.getId();
								     //new_file.setUrl(url);
								     new_file.setBill_id(billId);
								     String fileUrl="";
								     try {
								    	 
								    	 //File f= convertMultiPartToFile(file);
								    	 //String new_fileName=generateFileName(fileName,billId);
								    	 String new_fileName=fileName+"_"+b.getId();
							    	     AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
							    	     
							    	     fileUrl = endPointUrl + "/" + bucketName + "/" + new_fileName;
							    	     long startTimeS3Upload =  System.currentTimeMillis();
							    	     ObjectMetadata meta = new ObjectMetadata();
							             meta.setContentType(file.getContentType());
							             meta.setContentLength(file.getSize());
				                         PutObjectRequest p = new PutObjectRequest(this.bucketName , new_fileName , file.getInputStream(), meta);
				                         p.withCannedAcl(CannedAccessControlList.Private);
				                         PutObjectResult putResult = s3Client.putObject(p);
				                         //s3Client.putObject(p);
				                         new PutObjectResult();
				                         long stopTimeS3Upload= System.currentTimeMillis();
				                         long durationS3Upload=(stopTimeS3Upload - startTimeS3Upload);
					    		          statsDClient.recordExecutionTime("S3FileUploadTime",durationS3Upload);
				                         ObjectMetadata obj = s3Client.getObjectMetadata(this.bucketName,new_fileName);
				                         new_file.setContent_type(obj.getContentType());
				                         new_file.setFile_size(obj.getContentLength());
				                         new_file.setMd5Content(putResult.getContentMd5());
				                         new_file.setLastModified_time(obj.getLastModified().toString());
				                         
									} 
								        catch (AmazonServiceException e) {
										System.out.println("Error in uploading");
										e.printStackTrace();
										return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error in uploading file. Key file should be present");
										
									}
									    new_file.setUrl(fileUrl);
									    long startTimeFileSave =  System.currentTimeMillis();
								     fileRepository.save(new_file);
								     b.setFile(new_file);
								     billRepository.save(b);
								     long stopTimeFileSave =  System.currentTimeMillis();
										long durationFileSave = (stopTimeFileSave - startTimeFileSave);
				    		            statsDClient.recordExecutionTime("dbFileSaveTime",durationFileSave);
								     long stopTimePostFileApi =  System.currentTimeMillis();
								     long durationFilePostApi = (stopTimePostFileApi - startTimePostFileApi);
							          statsDClient.recordExecutionTime("APIBillPostTime",durationFilePostApi);
							            logger.info("File has been uploaded to S3 "+durationFilePostApi+"");
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
		    statsDClient.incrementCounter("Get_File_API");
		    long startTimeGetFileApi =  System.currentTimeMillis();
			logger.info("Inside Get_File_API");
		     String token = request.getHeader("Authorization");
		   System.out.println("Token is :"+token);
		   Gson gson = new Gson(); 
		   if(token!=null) {
				 token= token.replaceFirst("Basic ", "");
				 byte[] credDecoded=Base64.getDecoder().decode(token);
				 String credentials=new String(credDecoded,StandardCharsets.UTF_8);
				String[] userDetails=credentials.split(":",2);
				 
				 //String decodedBase64String = new String(Base64.getDecoder().decode(token.getBytes()));
				 //String[] userDetails = decodc6dd561a-e04c-47db-b406-eb2968ac1862edBase64String.split(":");
				 String emailId = userDetails[0];
				 User user = userRepository.findByemail(emailId);
				 String password = userDetails[1];
				// System.out.println("Password from repo: "+user.getPassword());
				
				// user.setPassword("null");
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
							 long startTimeFileGet =  System.currentTimeMillis();
							  Optional<FileAttachment> optionalFile = fileRepository.findById(fileId);
							  long stopTimeFileGet =  System.currentTimeMillis();
							  long durationFileGet = (stopTimeFileGet - startTimeFileGet);
		    		         statsDClient.recordExecutionTime("dbFileGetTime",durationFileGet);
		    		         logger.info("Duration to get file is "+durationFileGet);
							  if(!optionalFile.isPresent()) {
								  return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified file id is not present");
							  }
							  f =optionalFile.get();
							  
							  try 
							  {
								  if(b.getFile().getId().equals(f.getId()) ) {
									  long stopTimeGetFileApi =  System.currentTimeMillis();
									  long durationFileGetApi = (stopTimeGetFileApi - startTimeGetFileApi);
								      statsDClient.recordExecutionTime("APIFileGetTime",durationFileGetApi);
								      logger.info("File data has been retrieved in "+durationFileGetApi+" milliseconds");
									  return ResponseEntity.status(HttpStatus.OK).body(f);
								  }
							  }catch(Exception e) {
								  return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The file specified does not belong to the bill");
							  }	 
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
		   statsDClient.incrementCounter("Delete_File_API");
		   long startTimeDeleteFileApi =  System.currentTimeMillis();
			logger.info("Inside Delete_File_API");
		  	String token = request.getHeader("Authorization");
			 if(token!=null) {
				 token= token.replaceFirst("Basic ", "");
				 String decodedBase64String = new String(Base64.getDecoder().decode(token.getBytes()));
				 String[] userDetails = decodedBase64String.split(":",2);
				 String emailId = userDetails[0];
				 User user = userRepository.findByemail(emailId);
//				 System.out.println("Email from url :"+emailId);
//				 System.out.println("Email from user object: "+ user.getEmail());
				 String password = userDetails[1];
//				 System.out.println("Password from url: "+password);
//				 System.out.println("Password from repo: "+user.getPassword());
				
				
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
						 if(bills.contains(b)) {
							 Optional<FileAttachment> optionalFile = fileRepository.findById(fileId);
							  if(!optionalFile.isPresent()) {
								  return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified file id is not present");
							  }
							  f = optionalFile.get();
							  if(b.getFile().getId().equals(fileId)) {
								  
								  try {
									  String fileUrl=f.getUrl();
									  String fileName=fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
									  AmazonS3 s3Client = AmazonS3ClientBuilder.standard() .withRegion(Regions.US_EAST_1).build();
									  long startTimeDeleteFileS3 =  System.currentTimeMillis();
							          DeleteObjectRequest deleteAttach=new DeleteObjectRequest(this.bucketName,fileName);
							          s3Client.deleteObject(deleteAttach);
							          long stopTimeFileDeleteS3 =  System.currentTimeMillis();
									  long durationFileDeleteS3 = (stopTimeFileDeleteS3 - startTimeDeleteFileS3);
				    		         statsDClient.recordExecutionTime("S3FileDeleteTime",durationFileDeleteS3);
									  
								  }catch(AmazonServiceException e){
									  e.printStackTrace();
									  
								  }
								  b.setFile(null);
								  long startTimeDeleteFile =  System.currentTimeMillis();
								  fileRepository.delete(f);
								  long stopTimeFileDelete =  System.currentTimeMillis();
								  long durationFileDelete = (stopTimeFileDelete - startTimeDeleteFile);
			    		         statsDClient.recordExecutionTime("dbFileDeleteTime",durationFileDelete);
								  long stopTimeDeleteFileApi =  System.currentTimeMillis();
								  long durationFileDeleteApi = (stopTimeDeleteFileApi - startTimeDeleteFileApi);
							      statsDClient.recordExecutionTime("APIFileDelteTime",durationFileDeleteApi);
							      logger.info("File deleted in  "+durationFileDeleteApi +" milli seconds");
								  return ResponseEntity.status(HttpStatus.NO_CONTENT).body("The file attachment is deleted"); 
							  }
							   return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The file specified does not belong to the bill");
							   
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
	    


	    private File convertMultiPartToFile(MultipartFile file) throws IOException {
	        File convFile = new File(file.getOriginalFilename());
	        FileOutputStream fos = new FileOutputStream(convFile);
	        fos.write(file.getBytes());
	        fos.close();
	        return convFile;
	    }
	    
	    private String generateFileName(String fileName,String billId) {
	        return new Date().getTime() + "-" + billId + "-" +fileName.replace(" ", "_");
	    }
}


