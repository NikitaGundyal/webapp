package com.csye6225.controller;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.csye6225.model.User;
import com.csye6225.repository.UserRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.timgroup.statsd.StatsDClient;

@RestController
public class UserController {
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private StatsDClient statsDClient;
	
	private final static Logger logger= LoggerFactory.getLogger(UserController.class);
	  
//	@GetMapping("/")
//    public ResponseEntity<?> beginApp(){
//        statsDClient.incrementCounter("endpoint.user.home.http.get");
//        LocalTime localTime= LocalTime.now();
//        return ResponseEntity.status(HttpStatus.OK).body("This is a health check endpoint");
//       
//
//    }
	
	@RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String GetUser(HttpServletRequest request, HttpServletResponse response) {
        statsDClient.incrementCounter("endpoint.api.get");

        JsonObject jsonObject = new JsonObject();
        try {
            jsonObject.addProperty("message", "you are logged in. current time is " + java.time.LocalTime.now().toString());
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            return jsonObject.toString();
        } catch (Exception ex) {
            logger.info("user register");
            logger.error(ex.getMessage(), ex.getStackTrace());
            jsonObject.addProperty("error", "Exception occured! Check log");
            return jsonObject.toString();
        }
    }
	
	@PostMapping(path="/v1/user", consumes = "application/json", produces = "application/json")
	@ResponseBody
	public ResponseEntity<?> addUser(@Valid @RequestBody(required=false) User user){
		     
             statsDClient.incrementCounter("User_Registration_API");
             long startTimePostApi =  System.currentTimeMillis();
		    logger.info("Inside User_Registration_API");
		   
		    if(user==null) {
		    	long stopTimePostApi =  System.currentTimeMillis();
		    	long durationUserPostApi = (stopTimePostApi - startTimePostApi);
	            statsDClient.recordExecutionTime("APIUserPostTime",durationUserPostApi);
	            logger.error("Request Body cannot be empty");
		    	 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request Body cannot be empty");
		    }
            if(userRepository.findByemail(user.getEmail())==null)
          {
            if(user.getFirstname()!=null && user.getLastname()!=null && user.getEmail()!=null && user .getPassword()!=null)
            {
            	if(user.getFirstname().trim().length() > 0 && user.getLastname().trim().length() > 0 ) 
            	{
            		if(validatePassword(user.getPassword()) && validateEmail(user.getEmail())) 
                    {
                		
    					User user_new = new User();
    					user_new.setFirstname(user.getFirstname());
    					user_new.setLastname(user.getLastname());
    					user_new.setEmail(user.getEmail());
    					String hashedpwd = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
    					user_new.setPassword(hashedpwd);
    					//user_new.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
    					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); 
    					Date date_created = new Date();
    				user_new.setAccount_created(formatter.format(date_created).toString());
    					Date date_updated = new Date();
    					user_new.setAccount_updated(formatter.format(date_updated).toString());
    					long startTimeUserSave =  System.currentTimeMillis();
    					userRepository.save(user_new);
    					long stopTimeUserSave =  System.currentTimeMillis();
    					long durationUserSave = (stopTimeUserSave - startTimeUserSave);
    		            statsDClient.recordExecutionTime("dbUserSaveTime",durationUserSave);
    					 user_new.setPassword(null);
    					 long stopTimePostApi =  System.currentTimeMillis();
    	    		     long durationUserPostApi = (stopTimePostApi - startTimePostApi);
    	    	         statsDClient.recordExecutionTime("APIUserPostTime",durationUserPostApi);
    					logger.info("User Registered Successfully in "+durationUserPostApi+" milli seconds");
    					return ResponseEntity.status(HttpStatus.CREATED).body(user_new);
                    }
            		
            		long stopTimePostApi =  System.currentTimeMillis();
    		    	long durationUserPostApi = (stopTimePostApi - startTimePostApi);
    	            statsDClient.recordExecutionTime("APIUserPostTime",durationUserPostApi);
    	            logger.error("Email address or password do not satisfy the constraints. Email address should be of the type name@domain.com. Password should contain atleast 1 lower case letter, 1 upper csae letter, 1 digit from 0-9,1 special character and should be of atleast 9 characters");
                   	 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email address or password do not satisfy the constraints. Email address should be of the type name@domain.com. Password should contain atleast 1 lower case letter, 1 upper csae letter, 1 digit from 0-9,1 special character and should be of atleast 9 characters");
            	}
            	long stopTimePostApi =  System.currentTimeMillis();
		    	long durationUserPostApi = (stopTimePostApi - startTimePostApi);
	            statsDClient.recordExecutionTime("APIUserPostTime",durationUserPostApi);
	            logger.error("Firstname and lastname cannot be blank");
            	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firstname and lastname cannot be blank");
            }
            long stopTimePostApi =  System.currentTimeMillis();
	    	long durationUserPostApi = (stopTimePostApi - startTimePostApi);
            statsDClient.recordExecutionTime("APIUserPostTime",durationUserPostApi);
            logger.error("Firstname,lastname, email_address and password cannot be empty");
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firstname,lastname, email_address and password cannot be empty");
 	
           }
            long stopTimePostApi =  System.currentTimeMillis();
	    	long durationUserPostApi = (stopTimePostApi - startTimePostApi);
            statsDClient.recordExecutionTime("APIUserPostTime",durationUserPostApi);
            logger.error("User with the email id already exists");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User with the email id already exists");
	}
	
	@GetMapping(path = "/v1/user/self", produces = "application/json")
	public ResponseEntity<?> getUser(HttpServletRequest request){
		
		statsDClient.incrementCounter("Get_User_API");
		long startTimeGetUserApi =  System.currentTimeMillis();
		 logger.info("Inside Get_User_API");
		 String token = request.getHeader("Authorization");
		 Gson gson = new Gson(); 
		 if(token!=null) {
			 token= token.replaceFirst("Basic ", "");
			 byte[] credDecoded=Base64.getDecoder().decode(token);
			 String credentials=new String(credDecoded,StandardCharsets.UTF_8);
			 String[] userDetails=credentials.split(":",2);
			 
			 //String decodedBase64String = new String(Base64.getDecoder().decode(token.getBytes()));
			 //String[] userDetails = decodedBase64String.split(":");
			 String emailId = userDetails[0];
			 long startTimeFindUser =  System.currentTimeMillis();
			 User user = userRepository.findByemail(emailId);
			 long stopTimeFindUser =  System.currentTimeMillis();
			 long durationFindUser = (stopTimeFindUser - startTimeFindUser);
	         statsDClient.recordExecutionTime("dbFindUserTime",durationFindUser);
			 String password = userDetails[1];
			// System.out.println("Password from repo: "+user.getPassword());
			
			// user.setPassword("null");
			 String json = gson.toJson(user);
	
			 if(user==null) {
				 long stopTimeGetUserApi =  System.currentTimeMillis();
			    	long durationGetUserApi = (stopTimeGetUserApi - startTimeGetUserApi);
		            statsDClient.recordExecutionTime("APIGetUserTime",durationGetUserApi);
		            logger.error("The email does not exist");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The user with emailid:"+emailId+" does not exist. Try again");
			 }
			 else if (user!=null && !bCryptPasswordEncoder.matches(password, user.getPassword())) {
				 long stopTimeGetUserApi =  System.currentTimeMillis();
			    	long durationGetUserApi = (stopTimeGetUserApi - startTimeGetUserApi);
		            statsDClient.recordExecutionTime("APIGetUserTime",durationGetUserApi);
		            logger.error("The password is incorrect");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The password is incorrect. Try again");
			 }
			 
			 else {
				 user.setPassword(null);
				 long stopTimeGetUserApi =  System.currentTimeMillis();
			     long durationGetUserApi = (stopTimeGetUserApi - startTimeGetUserApi);
		         statsDClient.recordExecutionTime("APIGetUserTime",durationGetUserApi);
		         logger.info("The user information has been retreived in "+durationGetUserApi+" milli seconds");
				 return ResponseEntity.status(HttpStatus.OK).body(user);

			 }
		 }
		 long stopTimeGetUserApi =  System.currentTimeMillis();
	     long durationGetUserApi = (stopTimeGetUserApi - startTimeGetUserApi);
         statsDClient.recordExecutionTime("APIGetUserTime",durationGetUserApi);
         logger.error("Login is required");
		 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
		
	}
	
	@PutMapping(path="/v1/user/self", consumes = "application/json", produces = "application/json")
	public ResponseEntity<?> updateUser(@RequestHeader HttpHeaders header,@RequestBody(required=false) User userRequest){
        statsDClient.incrementCounter("User_Update_API");
        long startTimeUpdateUserApi =  System.currentTimeMillis();
		logger.info("Inside User_Update_API");
		String token = header.getFirst("Authorization");
		 if(token!=null) {
			 token= token.replaceFirst("Basic ", "");
			 String decodedBase64String = new String(Base64.getDecoder().decode(token.getBytes()));
			 String[] userDetails = decodedBase64String.split(":",2);
			 String emailId = userDetails[0];
			 User user = userRepository.findByemail(emailId);
			 String password = userDetails[1];
			
			 if(user==null) {
				 long stopTimeUpdateUserApi =  System.currentTimeMillis();
			    	long durationUpdateUserApi = (stopTimeUpdateUserApi - startTimeUpdateUserApi);
		            statsDClient.recordExecutionTime("APIUpdateUserTime",durationUpdateUserApi);
		            logger.error("The email does not exist");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The user with emailid:"+emailId+" does not exist. Try again");
			 }
			 else if (user!=null && !bCryptPasswordEncoder.matches(password, user.getPassword())) {
				 long stopTimeUpdateUserApi =  System.currentTimeMillis();
			    	long durationUpdateUserApi = (stopTimeUpdateUserApi - startTimeUpdateUserApi);
		            statsDClient.recordExecutionTime("APIUpdateUserTime",durationUpdateUserApi);
		            logger.error("The password");
				 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("The password is incorrect. Try again");
			 }
			 else {
				 if(user==null) {
					 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request Body cannot be empty");
			    }
				if(user.getFirstname()!=null && user.getLastname()!=null || user.getEmail()!=null|| user .getPassword()!=null) 
				{
					if(user.getFirstname().length() > 0 && user.getLastname().length() > 0) 
					{
						if(validatePassword(user.getPassword()) && validateEmail(user.getEmail())) 
						{
							 if(userRequest.getEmail().equals(user.getEmail())) {
								 
								 user.setFirstname(userRequest.getFirstname());
								 user.setLastname(userRequest.getLastname());
								 String hashedpwd = BCrypt.hashpw(userRequest.getPassword(), BCrypt.gensalt());
								 user.setPassword(hashedpwd);
								 //user.setPassword(bCryptPasswordEncoder.encode(userRequest.getPassword()));
								 SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); 	
								 Date date_updated = new Date();
								 user.setAccount_updated(formatter.format(date_updated).toString()); 
								 long startTimeUpdateUser =  System.currentTimeMillis();
								 userRepository.save(user);
								 long stopTimeUpdateUser =  System.currentTimeMillis();
								 long durationUpdateUser = (stopTimeUpdateUser - startTimeUpdateUser);
			    		         statsDClient.recordExecutionTime("dbUpdateUserTime",durationUpdateUser);
			    		         long stopTimeUpdateUserApi =  System.currentTimeMillis();
							    	long durationUpdateUserApi = (stopTimeUpdateUserApi - startTimeUpdateUserApi);
						            statsDClient.recordExecutionTime("APIUpdateUserTime",durationUpdateUserApi);
			    		         logger.info("User details have been updated");
								 return ResponseEntity.status(HttpStatus.NO_CONTENT).body("User details updated");
								 
							 }
							 else if (userRepository.findByemail(userRequest.getEmail())==null) {
								 long stopTimeUpdateUserApi =  System.currentTimeMillis();
							    	long durationUpdateUserApi = (stopTimeUpdateUserApi - startTimeUpdateUserApi);
						            statsDClient.recordExecutionTime("APIUpdateUserTime",durationUpdateUserApi);
						            logger.error("The email address cannot be updated");
								 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You cannot update email_address field");
							 }
							 
							 else if (userRepository.findByemail(userRequest.getEmail())!=null && !userRequest.getEmail().equals(user.getEmail())) {
								 long stopTimeUpdateUserApi =  System.currentTimeMillis();
							    	long durationUpdateUserApi = (stopTimeUpdateUserApi - startTimeUpdateUserApi);
						            statsDClient.recordExecutionTime("APIUpdateUserTime",durationUpdateUserApi);
						            logger.error("No other user's information can be updated");
								 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You cannot update any other user's information");
							 }
						}
						long stopTimeUpdateUserApi =  System.currentTimeMillis();
				    	long durationUpdateUserApi = (stopTimeUpdateUserApi - startTimeUpdateUserApi);
			            statsDClient.recordExecutionTime("APIUpdateUserTime",durationUpdateUserApi);
			            logger.error("Email or password do not satisfy the constraints");
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email address or password do not satisfy the constraints. Email address should be of the type name@domain.com. Password should contain atleast 1 lower case letter, 1 upper csae letter, 1 digit from 0-9,1 special character and should be of atleast 9 characters");
						
					}
					long stopTimeUpdateUserApi =  System.currentTimeMillis();
			    	long durationUpdateUserApi = (stopTimeUpdateUserApi - startTimeUpdateUserApi);
		            statsDClient.recordExecutionTime("APIUpdateUserTime",durationUpdateUserApi);
		            logger.error("Firstname and lastname cannot be blank");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firstname and lastname cannot be blank");
				}
				long stopTimeUpdateUserApi =  System.currentTimeMillis();
		    	long durationUpdateUserApi = (stopTimeUpdateUserApi - startTimeUpdateUserApi);
	            statsDClient.recordExecutionTime("APIUpdateUserTime",durationUpdateUserApi);
	            logger.error("Firstname,lastname, email_address and password cannot be empty");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firstname,lastname, email_address and password cannot be empty");	 
					 
							 
				 }
		 }
		 long stopTimeUpdateUserApi =  System.currentTimeMillis();
	     long durationUpdateUserApi = (stopTimeUpdateUserApi - startTimeUpdateUserApi);
         statsDClient.recordExecutionTime("APIUpdateUserTime",durationUpdateUserApi);
         logger.error("You need to be logged in");
		 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not logged in");
		
		
	}
	
	private boolean validateEmail(String email) {
		String regex = "^[a-zA-Z0-9_+&-]+(?:\\." + "[a-zA-Z0-9_+&-]+)*@" + "(?:[a-zA-Z0-9-]+\\.)+[a-z"
				+ "A-Z]{2,7}$";
		//String regex = "^[A-Z0-9+_.-]+@[a-z.-]+$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(email);
		if(matcher.matches()) return true;
		else return false;
		
		
	}
	
//	public Boolean validatePassword(String password) 
//	{
//		if (password != null || (!password.equalsIgnoreCase(""))) 
//		{
//			System.out.println("password in regex is : "+ password);
//			String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\\\S+$).{9,}$";
//			System.out.println(password.matches(pattern));
//			return (password.matches(pattern));
//		} 
//		else 
//		{
//			return Boolean.FALSE;
//		}
//
//	}
	
//	public Boolean validateEmail(String email) 
//	{
//		if (email != null || (!email.equalsIgnoreCase(""))) 
//		{
//			System.out.println("Email in regex is : "+ email);
//			String emailvalidator = "^[a-zA-Z0-9_+&-]+(?:\\." + "[a-zA-Z0-9_+&-]+)*@" + "(?:[a-zA-Z0-9-]+\\.)+[a-z"
//					+ "A-Z]{2,7}$";
//			System.out.println(email.matches(emailvalidator));
//			return email.matches(emailvalidator);
//		} 
//		else
//		{
//			return Boolean.FALSE;
//		}
//
//	}
//	
      private boolean validatePassword(String password) {
		//String regex="^(?=.?[A-Z])(?=(.[a-z]){1,})(?=(.[\\d]){1,})(?=(.[\\W]){1,})(?!.*\\s).{9,16}$";
		String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{9,}$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(password);
		if(matcher.matches()) return true;
		else return false;
		
	}

}



