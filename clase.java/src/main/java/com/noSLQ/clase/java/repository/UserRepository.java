package com.noSLQ.clase.java.repository;

import com.noSLQ.clase.java.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);
    List<User> findByName(String name);
    List<User> findByNameContainingIgnoreCase(String name);
    boolean existsByEmail(String email);

}
