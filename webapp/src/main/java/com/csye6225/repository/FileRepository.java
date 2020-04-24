package com.csye6225.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.csye6225.model.FileAttachment;

@Repository
public interface FileRepository extends JpaRepository<FileAttachment, String>{
	
	Optional<FileAttachment> findById(String id);

}

