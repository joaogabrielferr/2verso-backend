package com.escritr.escritr.auth.repository;

import com.escritr.escritr.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailOrUsername(String email, String username);

}

