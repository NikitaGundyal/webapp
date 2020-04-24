package com.csye6225.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.csye6225.model.Bill;

@Repository
public interface BillRepository extends JpaRepository<Bill, String>{
	
	List<Bill> findByUserId(String id);
	Optional<Bill> findById(String id);
}
