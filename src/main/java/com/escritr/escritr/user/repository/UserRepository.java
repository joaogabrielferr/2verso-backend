package com.escritr.escritr.user.repository;

import com.escritr.escritr.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailOrUsername(String email, String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);


}

