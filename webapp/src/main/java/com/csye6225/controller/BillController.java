package com.csye6225.controller;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.validation.ObjectError;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.csye6225.model.Bill;
import com.csye6225.model.PaymentType;
import com.csye6225.model.User;
import com.csye6225.repository.BillRepository;
import com.csye6225.repository.UserRepository;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.gson.Gson;
import com.timgroup.statsd.StatsDClient;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@RestController
public class BillController {
	
	@Autowired
	BillRepository billRepository;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private StatsDClient statsDClient;
	
	
	private final static Logger logger= LoggerFactory.getLogger(BillController.class);
	
	private static final String QUEUE_NAME = "testQueue" + new Date().getTime();
	
	@Value("${bucket.name}")
	String bucketName;
	
	@PostMapping(path = "/v1/bill/" , consumes = "application/json", produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> addBill(HttpServletRequest request, @Valid @RequestBody(required = false) Bill bill, BindingResult result ){
		statsDClient.incrementCounter("Post_Bill_API");
	    long startTimePostBillApi =  System.currentTimeMillis();
		logger.info("Inside Post_Bill_API");
		String token = request.getHeader("Authorization");
		Gson gson = new Gson(); 
		 if(token!=null) {
			 token= token.replaceFirst("Basic ", "");
			 byte[] credDecoded=Base64.getDecoder().decode(token);
			 String credentials=new String(credDecoded,StandardCharsets.UTF_8);
			 String[] userDetails=credentials.split(":",2);
			 String emailId = userDetails[0];
			 User user = userRepository.findByemail(emailId);
			 String password = userDetails[1];
			 String json = gson.toJson(user);
	
			 if(user==null) {
				 long stopTimePostBillApi =  System.currentTimeMillis();
			    	long durationBillPostApi = (stopTimePostBillApi - startTimePostBillApi);
		            statsDClient.recordExecutionTime("APIBillPostTime",durationBillPostApi);
		            logger.error("Email id does not exist");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The user with emailid:"+emailId+" does not exist. Try again");
			 }
			 else if (user!=null && !bCryptPasswordEncoder.matches(password, user.getPassword())) {
				 long stopTimePostBillApi =  System.currentTimeMillis();
			    	long durationBillPostApi = (stopTimePostBillApi - startTimePostBillApi);
		            statsDClient.recordExecutionTime("APIBillPostTime",durationBillPostApi);
		            logger.error("Password is incorrect");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The password is incorrect. Try again");
			 }
			 
			 else {
				   
				  if(bill==null) {
							 
							 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request Body cannot be null");
						 }
			if(bill.getVendor()!=null && bill.getBill_date()!=null && bill.getDue_date() !=null && bill.getCategories()!=null && bill.getPaymentStatus()!=null)
			{
				if(bill.getVendor().trim().length() > 0 && bill.getAmount_due() > 0.00 && !bill.getCategories().isEmpty()) 
				{
					
					if(validateDate(bill.getBill_date()) && validateDate(bill.getDue_date())) 
					{
						   Bill new_bill = new Bill();
						   new_bill.setVendor(bill.getVendor());
						   new_bill.setBill_date(bill.getBill_date());
						   new_bill.setDue_date(bill.getDue_date());
						   new_bill.setAmount_due(bill.getAmount_due());
						   new_bill.setCategories(bill.getCategories());
						   SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); 
						    Date date_created = new Date();
						    new_bill.setCreated_ts(formatter.format(date_created).toString());
						    System.out.println("Created_ts : "+ new_bill.getCreated_ts());
							Date date_updated = new Date();
							new_bill.setUpdated_ts(formatter.format(date_updated).toString());
							new_bill.setPaymentStatus(bill.getPaymentStatus());
							new_bill.setUser(user);
							new_bill.setOwner_id(new_bill.getUser().getId());
							Map<String, String> map = new HashMap<>();
							long startTimeBillSave =  System.currentTimeMillis();
							billRepository.save(new_bill);
							long stopTimeBillSave =  System.currentTimeMillis();
							long durationBillSave = (stopTimeBillSave - startTimeBillSave);
	    		            statsDClient.recordExecutionTime("dbBillSaveTime",durationBillSave);
							new_bill.setUser(null);
							 long stopTimePostBillApi =  System.currentTimeMillis();
						    	long durationBillPostApi = (stopTimePostBillApi - startTimePostBillApi);
					            statsDClient.recordExecutionTime("APIBillPostTime",durationBillPostApi);
							logger.info("Bill has been created for the user "+durationBillPostApi+" milli seconds");
							return ResponseEntity.status(HttpStatus.OK).body(new_bill);
					}
					 long stopTimePostBillApi =  System.currentTimeMillis();
				    	long durationBillPostApi = (stopTimePostBillApi - startTimePostBillApi);
			            statsDClient.recordExecutionTime("APIBillPostTime",durationBillPostApi);
			            logger.error("Invalid date format");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid date format for due_date and bill_date ");
				}
				 long stopTimePostBillApi =  System.currentTimeMillis();
			    	long durationBillPostApi = (stopTimePostBillApi - startTimePostBillApi);
		            statsDClient.recordExecutionTime("APIBillPostTime",durationBillPostApi);
		            logger.error("Amount due should be a valid double");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Amount_due should be a valid double value. Vendor,Categories cannot be blank");

			}
			 long stopTimePostBillApi =  System.currentTimeMillis();
		    	long durationBillPostApi = (stopTimePostBillApi - startTimePostBillApi);
	            statsDClient.recordExecutionTime("APIBillPostTime",durationBillPostApi);
	            logger.error("Vendor,Bill_date, Due_date, Amount_due,Categories and Payment Status cannot be empty");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Vendor,Bill_date, Due_date, Amount_due,Categories and Payment Status cannot be empty");
	       }
	}
		 long stopTimePostBillApi =  System.currentTimeMillis();
	    long durationBillPostApi = (stopTimePostBillApi - startTimePostBillApi);
         statsDClient.recordExecutionTime("APIBillPostTime",durationBillPostApi);
         logger.error("User needs to be logged in");
		 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
		
	}

	@GetMapping(path = "/v1/bills", produces = "application/json")
	public ResponseEntity<?> getBills(HttpServletRequest request){
		statsDClient.incrementCounter("Get_Bills_API");
		long startTimeGetBillsApi =  System.currentTimeMillis();
		logger.info("Inside Get_Bills_API");
		String token = request.getHeader("Authorization");
		 System.out.println("Token is :"+token);
		 Gson gson = new Gson(); 
		 if(token!=null) {
			 token= token.replaceFirst("Basic ", "");
			 byte[] credDecoded=Base64.getDecoder().decode(token);
			 String credentials=new String(credDecoded,StandardCharsets.UTF_8);
			 String[] userDetails=credentials.split(":",2);
			 String emailId = userDetails[0];
			 User user = userRepository.findByemail(emailId);
			 String password = userDetails[1];
			 String json = gson.toJson(user);
	
			 if(user==null) {
				 long stopTimeGetBillApi =  System.currentTimeMillis();
			    	long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillsApi);
		         statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
		         logger.error("User with email does not exist");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The user with emailid:"+emailId+" does not exist. Try again");
			 }
			 else if (user!=null && !bCryptPasswordEncoder.matches(password, user.getPassword())) {
				 long stopTimeGetBillApi =  System.currentTimeMillis();
			    	long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillsApi);
		         statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
		         logger.error("Password is incorrect");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The password is incorrect. Try again");
			 }
			 
			 else {
				 long startTimeGetBill =  System.currentTimeMillis();
				  List<Bill> bills = billRepository.findByUserId(user.getId());
				  long stopTimeGetBill =  System.currentTimeMillis();
				  long durationGetBill = (stopTimeGetBill - startTimeGetBill);
		          statsDClient.recordExecutionTime("dbGetBillTime",durationGetBill);
				  List<Bill> new_bills = new ArrayList<>();
				  for(Bill bill : bills) {
					  bill.setUser(null);
					  new_bills.add(bill);
				  }
				  long stopTimeGetBillApi =  System.currentTimeMillis();
			      long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillsApi);
		         statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
				  logger.info("Bill information is retrieved in "+durationBillGetApi+" milli seconds");
				 return ResponseEntity.status(HttpStatus.OK).body(new_bills);

			 }
		 }
		 long stopTimeGetBillApi =  System.currentTimeMillis();
	    	long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillsApi);
      statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
      logger.error("User needs to be logged in");
		 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
		
	}

	@GetMapping(path = "/v1/bill/{id}", produces = "application/json")
	public ResponseEntity<?> getBillById(HttpServletRequest request, @PathVariable(value = "id") String billId){
		statsDClient.incrementCounter("Get_Bill_By_ID_API");
		long startTimeGetBillApi =  System.currentTimeMillis();
		logger.info("Inside Get_Bill_By_ID_API");
		String token = request.getHeader("Authorization");
		 Gson gson = new Gson(); 
		 if(token!=null) {
			 token= token.replaceFirst("Basic ", "");
			 byte[] credDecoded=Base64.getDecoder().decode(token);
			 String credentials=new String(credDecoded,StandardCharsets.UTF_8);
			 String[] userDetails=credentials.split(":",2);
			 String emailId = userDetails[0];
			 User user = userRepository.findByemail(emailId);
			 String password = userDetails[1];
			 String json = gson.toJson(user);
	
			 if(user==null) {
				 long stopTimeGetBillApi =  System.currentTimeMillis();
			    	long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillApi);
		         statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
		         logger.error("User with email does not exist");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The user with emailid: "+emailId+" does not exist. Try again");
			 }
			 else if (user!=null && !bCryptPasswordEncoder.matches(password, user.getPassword())) {
				 long stopTimeGetBillApi =  System.currentTimeMillis();
			    	long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillApi);
		         statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
		         logger.error("Password is incorrect");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The password is incorrect. Try again");
			 }
			 
			 else {
				 
				 UUID uuid = null;
				 try {
					uuid = UUID.fromString(billId);
				 }
				 catch(Exception e) {
					 long stopTimeGetBillApi =  System.currentTimeMillis();
				    	long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillApi);
			         statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
			         logger.error("The bill does not exist");
					 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id not found. Enter correct bill id");
				 }
				 Bill b = new Bill(); 
				 long startTimeGetUser =  System.currentTimeMillis();
				 List<Bill> bills = billRepository.findByUserId(user.getId());
				 long stopTimeGetUser =  System.currentTimeMillis();
				 long durationGetUser_Bill = (stopTimeGetUser - startTimeGetUser);
		         statsDClient.recordExecutionTime("dbGetUser-BillTime",durationGetUser_Bill);
				
				 if(bills.size() > 0) 
				 {
					 long startTimeGetBill =  System.currentTimeMillis();
					 Optional<Bill> optionalBill = billRepository.findById(billId);
					 long stopTimeGetBill =  System.currentTimeMillis();
					 long durationGetBill = (stopTimeGetBill - startTimeGetBill);
			         statsDClient.recordExecutionTime("dbGetBillTime",durationGetBill);
					 if(optionalBill.isPresent()) {
						 b = optionalBill.get();
						 //System.out.println("Bill is :"+b);
					 }
					 if(b==null) 
					 {
						 long stopTimeGetBillApi =  System.currentTimeMillis();
					      long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillApi);
				         statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
				         logger.error("The bill does not exist");
						 
						 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id is not present");
						 
					 }
					 if(bills.contains(b)) {
						  
						   b.setUser(null);
						   long stopTimeGetBillApi =  System.currentTimeMillis();
						      long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillApi);
					         statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
						   logger.info("Bill information has been retrieved in "+durationBillGetApi+" milliseconds");
						   return ResponseEntity.status(HttpStatus.OK).body(b);
						 
					 }
                     else  {
                    	 long stopTimeGetBillApi =  System.currentTimeMillis();
					      long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillApi);
				         statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
				         logger.error("One user cannot view other user's information");
						 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You cannot view any other user's bill information");
					 }
 
				 }
				 long stopTimeGetBillApi =  System.currentTimeMillis();
			      long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillApi);
		         statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
		         logger.error("Specified bill id not found"); 
				  return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id not found");
			 }
				 			
		 }
		 long stopTimeGetBillApi =  System.currentTimeMillis();
	      long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillApi);
        statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
        logger.error("User needs to be logged in to view the information");
		 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
		
	}

	@DeleteMapping(path = "/v1/bill/{id}", produces = "application/json")
	public ResponseEntity<?> deleteBill(HttpServletRequest request, @PathVariable(value = "id") String billId){
		statsDClient.incrementCounter("Delete_Bill_API");
		long startTimeDeleteBillApi =  System.currentTimeMillis();
		logger.info("Delete_Bill_API");
		String token = request.getHeader("Authorization");
		 if(token!=null) {
			 token= token.replaceFirst("Basic ", "");
			 String decodedBase64String = new String(Base64.getDecoder().decode(token.getBytes()));
			 String[] userDetails = decodedBase64String.split(":",2);
			 String emailId = userDetails[0];
			 User user = userRepository.findByemail(emailId);
			 String password = userDetails[1];
			 
			 if(user==null) {
				 long stopTimeDeleteBillApi =  System.currentTimeMillis();
			      long durationBillDeleteApi = (stopTimeDeleteBillApi - startTimeDeleteBillApi);
		        statsDClient.recordExecutionTime("APIBillDeleteTime",durationBillDeleteApi);
		        logger.error("Email ID does not exist");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The user with emailid:"+emailId+" does not exist. Try again");
			 }
			 else if (user!=null && !bCryptPasswordEncoder.matches(password, user.getPassword())) {
				 long stopTimeDeleteBillApi =  System.currentTimeMillis();
			      long durationBillDeleteApi = (stopTimeDeleteBillApi - startTimeDeleteBillApi);
		        statsDClient.recordExecutionTime("APIBillDeleteTime",durationBillDeleteApi);
		        logger.error("Password is incorrect");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The password is incorrect. Try again");
			 }
			 else {
				 
				 UUID uuid = null;
				 try {
					uuid = UUID.fromString(billId);
				 }
				 catch(Exception e) {
					 long stopTimeDeleteBillApi =  System.currentTimeMillis();
				      long durationBillDeleteApi = (stopTimeDeleteBillApi - startTimeDeleteBillApi);
			        statsDClient.recordExecutionTime("APIBillDeleteTime",durationBillDeleteApi);
			        logger.error("Bill id does not exists");
					 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id not found. Enter correct bill id");
				 }
				
				 Bill b = new Bill(); 
				  
				 List<Bill> bills = billRepository.findByUserId(user.getId());
				 
				 
				 if(bills.size() > 0) 
				 {
					 
					 Optional<Bill> optionalBill = billRepository.findById(billId);
					 if(optionalBill.isPresent()) {
						 b = optionalBill.get();
					 }
					 if(b==null) 
					 {
						 long stopTimeDeleteBillApi =  System.currentTimeMillis();
					      long durationBillDeleteApi = (stopTimeDeleteBillApi - startTimeDeleteBillApi);
				        statsDClient.recordExecutionTime("APIBillDeleteTime",durationBillDeleteApi);
				        logger.error("Bill id does not exists");
						 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id is not present");
						 
					 }
					 if(bills.contains(b)) 
					 {
					       if(b.getFile()!=null) 
					       {
					    	   try {
									  String fileUrl=b.getFile().getUrl();
									  String fileName=fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
									  AmazonS3 s3Client = AmazonS3ClientBuilder.standard() .withRegion(Regions.US_EAST_1).build();
							          DeleteObjectRequest deleteAttach=new DeleteObjectRequest(this.bucketName,fileName);
							          s3Client.deleteObject(deleteAttach);
									  
								  }catch(AmazonServiceException e){
									  e.printStackTrace();
									  
								  }
//					    	   File file = new File(b.getFile().getUrl());
//					    	   file.delete();
					    	   long startTimeDeleteBill =  System.currentTimeMillis();
					    	   billRepository.delete(b);
					    	   long stopTimeDeleteBill =  System.currentTimeMillis();
					    	   long durationDeleteBill = (stopTimeDeleteBill - startTimeDeleteBill);
						       statsDClient.recordExecutionTime("dbDeleteBillTime",durationDeleteBill);
						       long stopTimeDeleteBillApi =  System.currentTimeMillis();
			   				   long durationBillDeleteApi = (stopTimeDeleteBillApi - startTimeDeleteBillApi);
			   			       statsDClient.recordExecutionTime("APIBillDeleteTime",durationBillDeleteApi);
						       logger.info("Bill has been deleted in "+durationBillDeleteApi+" milli seconds");
						      return ResponseEntity.status(HttpStatus.NO_CONTENT).body("The Bill  is deleted successfully");
					       }
					       long startTimeDeleteBill =  System.currentTimeMillis();
				    	   billRepository.delete(b);
				    	   long stopTimeDeleteBill =  System.currentTimeMillis();
				    	   long durationDeleteBill = (stopTimeDeleteBill - startTimeDeleteBill);
					       statsDClient.recordExecutionTime("dbDeleteBillTime",durationDeleteBill);
						   return ResponseEntity.status(HttpStatus.NO_CONTENT).body("The Bill  is deleted successfully");
						 
					 }
                     else  {
                    	 long stopTimeDeleteBillApi =  System.currentTimeMillis();
   				      long durationBillDeleteApi = (stopTimeDeleteBillApi - startTimeDeleteBillApi);
   			        statsDClient.recordExecutionTime("APIBillDeleteTime",durationBillDeleteApi);
   			        logger.error("One user cannot delete other user's bill details");
						 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You cannot delete any other user's bill information");
					 }
					 
				 }
				 
				 else  {
					 long stopTimeDeleteBillApi =  System.currentTimeMillis();
				      long durationBillDeleteApi = (stopTimeDeleteBillApi - startTimeDeleteBillApi);
			        statsDClient.recordExecutionTime("APIBillDeleteTime",durationBillDeleteApi);
			        logger.error("Bill id does not exists");
					 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id is not present");
				 }
				
					 	 
		      }
		 }
		 long stopTimeDeleteBillApi =  System.currentTimeMillis();
	      long durationBillDeleteApi = (stopTimeDeleteBillApi - startTimeDeleteBillApi);
       statsDClient.recordExecutionTime("APIBillDeleteTime",durationBillDeleteApi);
       logger.error("User needs to be logged in");
		 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
		
	}

	@PutMapping(path = "/v1/bill/{id}", consumes = "application/json", produces = "application/json")
	public ResponseEntity<?> updateBill(HttpServletRequest request, @PathVariable(value = "id") String billId, @RequestBody(required = false) Bill bill){
		statsDClient.incrementCounter("Update_Bill_API");
		long startTimePutBillApi =  System.currentTimeMillis();
		logger.info("Update_Bill_API");
		String token = request.getHeader("Authorization");
		 if(token!=null) {
			 token= token.replaceFirst("Basic ", "");
			 String decodedBase64String = new String(Base64.getDecoder().decode(token.getBytes()));
			 String[] userDetails = decodedBase64String.split(":",2);
			 String emailId = userDetails[0];
			 User user = userRepository.findByemail(emailId);
			 String password = userDetails[1];
			
			
			 if(user==null) {
				 long stopTimePutBillApi =  System.currentTimeMillis();
			      long durationBillPutApi = (stopTimePutBillApi - startTimePutBillApi);
		       statsDClient.recordExecutionTime("APIBillPutTime",durationBillPutApi);
		       logger.error("Email id does not exists");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The user with emailid:"+emailId+" does not exist. Try again");
			 }
			 else if (user!=null && !bCryptPasswordEncoder.matches(password, user.getPassword())) {
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The password is incorrect. Try again");
			 }
			 else {
				
				 if(bill==null) {
					 
					 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request Body cannot be null");
				 }
				 
				 UUID uuid = null;
				 try {
					uuid = UUID.fromString(billId);
				 }
				 catch(Exception e) {
					 
					 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id not found. Enter correct bill id");
				 }
				 Bill b = new Bill(); 
				 Optional<Bill> optionalBill = billRepository.findById(billId); 
				 List<Bill> bills = billRepository.findByUserId(user.getId());
				 if(optionalBill.isPresent()) {
					 b = optionalBill.get();
				 }
				 if(b!=null) {
					 if(bills.contains(b))
				  {
						 if(bill.getVendor()!=null && bill.getBill_date()!=null && bill.getDue_date() !=null && bill.getCategories()!=null && bill.getPaymentStatus()!=null) 
						 {
							 if(bill.getVendor().length() > 0 && bill.getAmount_due() > 0.00 && !bill.getCategories().isEmpty()) 
								{
								 if(validateDate(bill.getBill_date()) && validateDate(bill.getDue_date())) 
								 {
									 b.setVendor(bill.getVendor());
									   b.setBill_date(bill.getBill_date());
									   b.setDue_date(bill.getDue_date());
									   b.setAmount_due(bill.getAmount_due());
									   b.setCategories(bill.getCategories());
									   SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); 
										Date date_updated = new Date();
										b.setUpdated_ts(formatter.format(date_updated).toString());
										b.setPaymentStatus(bill.getPaymentStatus());
										long startTimePutBill =  System.currentTimeMillis();
										billRepository.save(b);
										long stopTimePutBill =  System.currentTimeMillis();
								    	long durationDeleteBill = (stopTimePutBill - startTimePutBill);
									   statsDClient.recordExecutionTime("dbPutBillTime",durationDeleteBill);
									    	b.setUser(null);
										 long stopTimePutBillApi =  System.currentTimeMillis();
									     long durationBillPutApi = (stopTimePutBillApi - startTimePutBillApi);
								       statsDClient.recordExecutionTime("APIBillPutTime",durationBillPutApi);
								       logger.info("The bill has been updated");
									    return ResponseEntity.status(HttpStatus.OK).body(b);
								 }
								 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid date format for due_date and bill_date ");
							}
							 
							 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Amount_due should be a valid double value. Vendor,Categories cannot be blank");
							 
						 }
						   
						 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Vendor,Bill_date, Due_date, Amount_due,Categories and Payment Status cannot be empty");
					 }

					 else {
						 
						 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You cannot update any other user's bill information");
					 }
				 }
					
					 else {
						 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified bill id is not present");
					 }	
			   		 	 
		    }
			 
		 }
		 
		 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
		
	}
	
	
	
	@GetMapping(path = "/v1/bills/due/{x}", produces = "application/json")
	public ResponseEntity<?> getDueBills(HttpServletRequest request, @PathVariable(value = "x") String dueDays){
		statsDClient.incrementCounter("Get_Bills_Due_API");
		long startTimeGetBillsApi =  System.currentTimeMillis();
		logger.info("Inside Get_Bills_Due_API");
		logger.info("Due Days Path Variable: "+dueDays );
		String token = request.getHeader("Authorization");
		 System.out.println("Token is :"+token);
		 Gson gson = new Gson(); 
		 if(token!=null) {
			 token= token.replaceFirst("Basic ", "");
			 byte[] credDecoded=Base64.getDecoder().decode(token);
			 String credentials=new String(credDecoded,StandardCharsets.UTF_8);
			 String[] userDetails=credentials.split(":",2);
			 String emailId = userDetails[0];
			 User user = userRepository.findByemail(emailId);
			 String password = userDetails[1];
			 String json = gson.toJson(user);
	
			 if(user==null) {
				 long stopTimeGetBillApi =  System.currentTimeMillis();
			    	long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillsApi);
		         statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
		         logger.error("User with email does not exist");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The user with emailid:"+emailId+" does not exist. Try again");
			 }
			 else if (user!=null && !bCryptPasswordEncoder.matches(password, user.getPassword())) {
				 long stopTimeGetBillApi =  System.currentTimeMillis();
			    	long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillsApi);
		         statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
		         logger.error("Password is incorrect");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The password is incorrect. Try again");
			 }
			 
			 else {
				 
				 int numberOfDays=Integer.parseInt(dueDays);	   
			  		if(numberOfDays <0) {
			  			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No of days cannot be negative");
			  		}
				 
				 long startTimeGetBill =  System.currentTimeMillis();
				  List<Bill> bills = billRepository.findByUserId(user.getId());
				  long stopTimeGetBill =  System.currentTimeMillis();
				  long durationGetBill = (stopTimeGetBill - startTimeGetBill);
		          statsDClient.recordExecutionTime("dbGetBillTime",durationGetBill);
		          
		        JSONArray jsonArray = new JSONArray();
		  		JSONObject jsonObject = new JSONObject();
		  		jsonObject.put("username", user.getEmail());
		  		jsonObject.put("noDays",dueDays);
		  		
			      AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
			      String queueUrl= sqs.getQueueUrl("lambda-sqs-test").getQueueUrl();
			      logger.info("The queue url is : "+queueUrl);
			      
//			      CreateQueueRequest create_request = new CreateQueueRequest(QUEUE_NAME)
//			    	        .addAttributesEntry("DelaySeconds", "60")
//			    	        .addAttributesEntry("MessageRetentionPeriod", "86400");
//		  		
//			      try {
//			    	    sqs.createQueue(create_request);
//			    	} catch (AmazonSQSException e) {
//			    	    if (!e.getErrorCode().equals("QueueAlreadyExists")) {
//			    	        throw e;
//			    	    }
//			    	}
		  		
		  			       // String queueUrl = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();
		  		
		  			        SendMessageRequest send_msg_request = new SendMessageRequest()
		  			                .withQueueUrl(queueUrl)
		  			                .withMessageBody(jsonObject.toString())
		  			                .withDelaySeconds(5);
		  			        sqs.sendMessage(send_msg_request);
		  		
		  			     
		          Thread thread = new Thread(){
		        	    public void run(){
		        	     logger.info("Thread Running");
		        	     
		        	   //SQS polling
		        	     
		        	     ReceiveMessageRequest receive_request = new ReceiveMessageRequest()
		        	    	        .withQueueUrl(queueUrl)
		        	    	        .withWaitTimeSeconds(20);
		        	     try {
							Thread.sleep(3000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					        List<Message> messages = sqs.receiveMessage(receive_request).getMessages();
					        String my_msg="";
					        for(Message msg : messages) {
					        	sqs.changeMessageVisibility(queueUrl, msg.getReceiptHandle(), 60*60);
					        	my_msg= msg.getBody();
					        }				        
		  			      JSONParser parser = new JSONParser();
		  			      String username="";
		  			      String num="";
		  			    List<String> new_bills = new ArrayList<>();
		  			  JSONObject snsjsonObject = new JSONObject();
		  			  try {
		  			       	logger.info("Inside try");
		  						JSONObject json = (JSONObject) parser.parse(my_msg);
		  					     username=(String)json.get("username");
		  					      num = (String) json.get("noDays");
		  					      int daysDueforPayment=Integer.parseInt(num);
		  			              Date date = Calendar.getInstance().getTime();  
						          DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd");  
						          String strDate = dateFormat.format(date);  
						          logger.info("Current Date is : "+strDate);
						          String[] currentDates=strDate.split("-");
						          int current_date=Integer.parseInt(currentDates[2]);
								 
								  for(Bill bill : bills) {
									 String[] bill_due_dates=bill.getDue_date().split("-");
									 int bill_due=Integer.parseInt(bill_due_dates[2]);
									 int diff = bill_due - current_date;
									 if(diff >=0) {
										 if((diff < daysDueforPayment || diff==0) && bill.getPaymentStatus().equals(PaymentType.due)) {
											 new_bills.add(bill.getId());  
										 }
									 }  					  
								  }
								  
								  snsjsonObject.put("username",user.getEmail());
								  for(int i =0;i<new_bills.size();i++) {
									  jsonArray.add(new_bills.get(i));
								  }
								  snsjsonObject.put("billIds", jsonArray);
		  			  }
		  			  catch(ParseException e) {
		  				  
		  				  e.printStackTrace();
		  			  }
		  			        logger.info("Username is : "+username);
		  			        logger.info("Publishing through sns");
		  			      AmazonSNS snsClient = AmazonSNSAsyncClientBuilder.standard()
				                    .withRegion(Regions.US_EAST_1)
				                    .build();

				            List<Topic> topics = snsClient.listTopics().getTopics();

				            for(Topic topic: topics)
				            {
				                if(topic.getTopicArn().endsWith("lambda-sns-test")){
				                	logger.info("iInside SNS Username is : "+username);
				                    PublishRequest req = new PublishRequest(topic.getTopicArn(),snsjsonObject.toString());
				                    PublishResult result =snsClient.publish(req);
				                    logger.info("Message id is : "+result.getMessageId());
				                    break;
				                }
				            }
				            
				            
				           
				            
		        	     
		        	    }
		        	  };

		        	  thread.start();
		        	  
		        	  
		        	  
		        	  
		         
				  long stopTimeGetBillApi =  System.currentTimeMillis();
			      long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillsApi);
		         statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
				  logger.info("Bill information is retrieved in "+durationBillGetApi+" milli seconds");
				 
				
				 return ResponseEntity.status(HttpStatus.OK).body("Email has been initiated");

			 }
		 }
		 long stopTimeGetBillApi =  System.currentTimeMillis();
	    	long durationBillGetApi = (stopTimeGetBillApi - startTimeGetBillsApi);
      statsDClient.recordExecutionTime("APIBillGetTime",durationBillGetApi);
      logger.error("User needs to be logged in");
		 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
		
	}
	
	
		public boolean validateDate(String date) 
		{
		
		String regex =  "^[12]\\d{3}([- \\/.])((((0[13578])|(1[02]))[\\-\\s]?(([0-2][0-9])|(3[01])))|(((0[469])|(11))[\\-\\s]?(([0-2][0-9])|(30)))|(02[\\-\\s]?[0-2][0-9]))$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(date);
		if(matcher.matches()) return true;
		else return false;

	  }


}


