package com.escritr.escritr.user.repository;

import com.escritr.escritr.user.domain.UserAccountLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAccountLinkRepository extends JpaRepository<UserAccountLink, UUID> {
    Optional<UserAccountLink> findByProviderNameAndProviderUserId(String providerName, String providerUserId);
}