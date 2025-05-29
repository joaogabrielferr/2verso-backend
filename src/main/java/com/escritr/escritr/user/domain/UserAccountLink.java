package com.escritr.escritr.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_account_links", schema = "escritr",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider_name", "provider_user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class UserAccountLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String providerName;

    private String providerUserId;

    private Instant createdAt;

    private Instant updatedAt;

    public UserAccountLink(User user, String providerName, String providerUserId) {
        this.user = user;
        this.providerName = providerName;
        this.providerUserId = providerUserId;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }


}