package com.csye6225.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.csye6225.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, String>{
	
	User findByemail(String email);

}

