package com.escritr.escritr.auth.repository;

import com.escritr.escritr.auth.model.RefreshToken;
import com.escritr.escritr.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    // Method to delete tokens by user (useful for logout all devices)
    int deleteByUser(User user);

}