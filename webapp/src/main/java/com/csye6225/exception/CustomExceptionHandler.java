package com.csye6225.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.csye6225.controller.BillController;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.timgroup.statsd.StatsDClient;

@ControllerAdvice
public class CustomExceptionHandler {

	@Autowired
	private StatsDClient statsDClient;
	
	
	private final static Logger logger= LoggerFactory.getLogger(BillController.class);
//	@ExceptionHandler
//	public ResponseEntity<?> onInvalidFormatException(InvalidFormatException e){
//		
//		 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Amount should be a number or Payment_status field should be in [paid, due, past_due, no_payment_required] ");
//		
//	}
//	
//	@ExceptionHandler
//	public ResponseEntity<?> onGetException(Exception e){
//		
//		 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("URI is not supported without an id");
//		
//	}
	
//	@ExceptionHandler
//  public ResponseEntity<?> onHttpMessageNotReadable(final Exception e) throws Throwable {
//      final Throwable cause = e.getCause();
//      if (cause == null) 
//      {
//          return new ResponseEntity<String>("Request body cannot be null",HttpStatus.BAD_REQUEST);
//      } 
//      else if (cause instanceof HttpMessageNotReadableException)
//      {
//      	 return new ResponseEntity<String>("Request body cannot be null", HttpStatus.BAD_REQUEST);
//      }
//      else if (cause instanceof InvalidFormatException) 
//      {
//      	 return new ResponseEntity<String>("Amount should be a number or Payment_status field should be in [paid, due, past_due, no_payment_required] ", HttpStatus.BAD_REQUEST);
//      } 
//      else if(cause instanceof Exception) 
//      {
//    	  return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("URI is not supported without an id");  
//      }
//      return new ResponseEntity<String>("Unauthorized", HttpStatus.UNAUTHORIZED);
//  }
	
	@ExceptionHandler
	  public ResponseEntity<?> onMultipleExceptions(final Exception e) throws Throwable{
		final Throwable cause = e.getCause();
		long startTimePostBillApi =  System.currentTimeMillis();
	       if (e.getMessage().contains("Enum")) 
	      {
	    	 statsDClient.incrementCounter("Post_Bill_API");
	    	 long stopTimePostBillApi =  System.currentTimeMillis();
		     long durationBillPostApi = (stopTimePostBillApi - startTimePostBillApi);
	         statsDClient.recordExecutionTime("APIBillPostTime",durationBillPostApi);
	         logger.error("PaymentStatus field should be in [paid, due, past_due, no_payment_required]");
	      	 return new ResponseEntity<String>("PaymentStatus field should be in [paid, due, past_due, no_payment_required] ", HttpStatus.BAD_REQUEST);
	      } 
	      else if(e.getMessage().contains("double")) 
	      {
	    	  statsDClient.incrementCounter("Post_Bill_API");
		      long stopTimePostBillApi =  System.currentTimeMillis();
			  long durationBillPostApi = (stopTimePostBillApi - startTimePostBillApi);
		      statsDClient.recordExecutionTime("APIBillPostTime",durationBillPostApi);
		      logger.error("Amount due should be a number");
	    	  return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Amount due should be a number");  
	      }
	      else if (cause instanceof  MissingServletRequestPartException) 
	      {
	    	  return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Requied request part 'file' in the key");   
	      }
	       e.printStackTrace();
	      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input");
	  }
	
}

