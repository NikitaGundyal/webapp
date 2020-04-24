package com.csye6225.controllertest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.Assert.assertEquals;

import com.csye6225.controller.UserController;
import com.csye6225.model.User;
import com.csye6225.repository.UserRepository;

@RunWith(SpringRunner.class)
public class WebAppRestLayerUnitTest {
	
	private MockMvc mockMvc;
	
	@InjectMocks
	private UserController userController;
	
	@Mock
	private UserRepository userRepository;
	
	User mockUser = new User("0","deepika.gundyal@gmail.com","Deepika@12345");
	
	String exampleUser = "{\"id\":\"0\",\"email\":\"deepika.gundyal@gmail.com\",\"password\":\"Deepika@12345\"}";
	
	
	@Before
	public void setup() throws Exception{
		
		mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

	}
	
	@Test
	public void testIfExists() throws Exception{
		
		Mockito.when(userRepository.findByemail(Mockito.anyString())).thenReturn(mockUser);
		RequestBuilder requestBuilder = MockMvcRequestBuilders.post(
				"/v1/user").accept(MediaType.APPLICATION_JSON).content(exampleUser).contentType(
				MediaType.APPLICATION_JSON);
		
		MvcResult result = mockMvc.perform(requestBuilder).andReturn();
		System.out.println(result.getResponse());
				
		MockHttpServletResponse response = result.getResponse();
		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		//assertEquals(HttpStatus.OK.value(), response.getStatus());
		
		//mockMvc.perform(MockMvcRequestBuilders.post("/v1/user")).andExpect(MockMvcResultMatchers.status().isOk())
		
	}
	

}