package com.escritr.escritr.auth.service;

import com.escritr.escritr.auth.model.UserDetailsImpl;
import com.escritr.escritr.user.domain.User;
import com.escritr.escritr.user.domain.UserAccountLink;
import com.escritr.escritr.user.repository.UserAccountLinkRepository;
import com.escritr.escritr.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final Logger logger = LoggerFactory.getLogger(CustomOidcUserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAccountLinkRepository userAccountLinkRepository;

    // We can delegate to Spring's default OidcUserService to fetch the standard OidcUser first.
    private final OidcUserService delegate = new OidcUserService();

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Delegate to the default OIDC user service to get the standard OidcUser.
        // This handles fetching user info from the UserInfo endpoint and parsing the ID token.
        OidcUser oidcUser = delegate.loadUser(userRequest);
        logger.debug("DefaultOidcUser loaded by delegate: Name='{}', Authorities='{}', Attributes='{}'",
                oidcUser.getName(), oidcUser.getAuthorities(), oidcUser.getAttributes());


        Map<String, Object> attributes = oidcUser.getAttributes();
        String providerName = userRequest.getClientRegistration().getRegistrationId();

        String providerUserId = getProviderUserId(providerName, attributes); // oidcUser.getName() or oidcUser.getSubject() often works too
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName(); // Or oidcUser.getGivenName() + oidcUser.getFamilyName()
        // String picture = oidcUser.getPicture();

        if (!StringUtils.hasText(providerUserId)) {
            logger.error("OIDC provider '{}' did not return a subject (user ID). Attributes: {}", providerName, attributes);
            throw new OAuth2AuthenticationException("OIDC provider " + providerName + " did not return a subject ID.");
        }
        logger.debug("Provider User ID (from OIDC User): {}, Email: {}, Name: {}", providerUserId, email, name);

        // --- Your existing logic to find or create User and UserAccountLink ---
        Optional<UserAccountLink> accountLinkOptional =
                userAccountLinkRepository.findByProviderNameAndProviderUserId(providerName, providerUserId);
        User userEntity;
        if (accountLinkOptional.isPresent()) {
            userEntity = accountLinkOptional.get().getUser();
            logger.info("User found via existing {} account link. User ID: {}, Username: {}, Provider User ID: {}",
                    providerName, userEntity.getId(), userEntity.getUsername(), providerUserId);
            updateUserFromOAuthAttributes(userEntity, attributes, email, name);
        } else {
            Optional<User> userOptionalByEmail = Optional.empty();
            if (StringUtils.hasText(email)) {
                userOptionalByEmail = userRepository.findByEmail(email);
            }
            if (userOptionalByEmail.isPresent()) {
                userEntity = userOptionalByEmail.get();
                logger.info("Existing user found by email '{}' for provider '{}'. User ID: {}, Username: {}",
                        email, providerName, userEntity.getId(), userEntity.getUsername());
                if (userEntity.getPassword() != null && !userEntity.getPassword().isEmpty()) {
                    logger.warn("OAuth login attempt for email '{}' (provider '{}') which already exists as a local account with a password.", email, providerName);
                    throw new OAuth2AuthenticationException(
                            "An account already exists with this email address and is password-protected. " +
                                    "Please log in using your password, or link this provider from your account settings if supported."
                    );
                } else {
                    logger.info("Linking new {} provider account (ID: {}) to existing user (ID: {}).",
                            providerName, providerUserId, userEntity.getId());
                    linkProviderToUser(userEntity, providerName, providerUserId);
                    updateUserFromOAuthAttributes(userEntity, attributes, email, name);
                }
            } else {
                logger.info("Creating new user for {} provider (ID: {}) and email: {}",
                        providerName, providerUserId, email);
                if (!StringUtils.hasText(email)) {
                    logger.warn("Email not provided by {} for new user with provider ID {}. Username generation may be less ideal.",
                            providerName, providerUserId);
                }
                String username = generateUniqueUsername(email, name, providerUserId);
                userEntity = new User(username, email, name);
                userRepository.save(userEntity);
                linkProviderToUser(userEntity, providerName, providerUserId);
                logger.info("New user created with username: {} (ID: {}) and linked to {} (ID: {})",
                        username, userEntity.getId(), providerName, providerUserId);
            }
        }
        // --- End of your user logic ---

        // 2. Create your UserDetailsImpl, now it correctly implements OidcUser
        //    Pass the OidcIdToken and OidcUserInfo from the oidcUser obtained from the delegate.
        Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());
        // You can add your own application-specific authorities here if needed
        // authorities.add(new SimpleGrantedAuthority("ROLE_USER_OIDC"));


        // The 'nameAttributeKey' tells Spring Security which attribute in the claims set
        // represents the "name" of the user (often the subject 'sub').
        // This is important for how DefaultOidcUser is constructed internally if you were to use it.
        // Since we are returning UserDetailsImpl, the getName() method of UserDetailsImpl will be used.
        String nameAttributeKey = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        if (!StringUtils.hasText(nameAttributeKey)) {
            nameAttributeKey = "sub"; // Default to 'sub' if not configured
        }


        // Construct your UserDetailsImpl which IS-A OidcUser
        UserDetailsImpl customOidcPrincipal = new UserDetailsImpl(
                userEntity,
                attributes,         // These are the claims
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()
                // You might need to pass authorities if UserDetailsImpl doesn't derive them
        );

        // Ensure the UserDetailsImpl authorities are set if not done in constructor
        // For now, assuming UserDetailsImpl handles its authorities based on the User entity or defaults.
        // If you need to merge authorities from oidcUser with your app's authorities:
        // customOidcPrincipal.setAuthorities(mappedAuthorities); // You'd need a setter

        logger.info("Successfully processed OIDC user. Returning UserDetailsImpl for username: {}",
                customOidcPrincipal.getUsername());
        return customOidcPrincipal;
    }

    // --- Helper methods (linkProviderToUser, updateUserFromOAuthAttributes, getProviderUserId, generateUniqueUsername) ---
    // These remain the same as in your previous CustomOAuth2UserService
    private void linkProviderToUser(User user, String providerName, String providerUserId) {
        UserAccountLink newLink = new UserAccountLink(user, providerName, providerUserId);
        userAccountLinkRepository.save(newLink);
    }

    private void updateUserFromOAuthAttributes(User user, Map<String, Object> attributes, String emailFromProvider, String nameFromProvider) {
        boolean needsSave = false;
        if (StringUtils.hasText(nameFromProvider) && !nameFromProvider.equals(user.getName())) {
            user.setName(nameFromProvider);
            needsSave = true;
        }
        if (StringUtils.hasText(emailFromProvider) && !emailFromProvider.equals(user.getEmail())) {
            Optional<User> existingUserWithNewEmail = userRepository.findByEmail(emailFromProvider);
            if (existingUserWithNewEmail.isEmpty() || existingUserWithNewEmail.get().getId().equals(user.getId())) {
                user.setEmail(emailFromProvider);
                needsSave = true;
            } else {
                logger.warn("Attempted to update email for user ID {} to '{}', but this email is already taken by user ID {}.",
                        user.getId(), emailFromProvider, existingUserWithNewEmail.get().getId());
            }
        }
        if (needsSave) {
            userRepository.save(user);
        }
    }

    private String getProviderUserId(String providerName, Map<String, Object> attributes) {
        Object idValue;
        switch (providerName.toLowerCase()) {
            case "google": idValue = attributes.get("sub"); break;
            case "github": idValue = attributes.get("id"); break;
            case "facebook": idValue = attributes.get("id"); break;
            default:
                idValue = attributes.get("sub");
                if (idValue == null) idValue = attributes.get("id");
                break;
        }
        return (idValue != null) ? idValue.toString() : null;
    }

    private String generateUniqueUsername(String email, String name, String providerUserId) {
        String baseUsername = null;
        int maxLength = 45;
        if (StringUtils.hasText(email)) {
            baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_.-]", "");
            if (baseUsername.length() > maxLength) baseUsername = baseUsername.substring(0, maxLength);
        } else if (StringUtils.hasText(name)) {
            baseUsername = name.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9_]", "");
            if (baseUsername.length() > maxLength) baseUsername = baseUsername.substring(0, maxLength);
        }
        if (!StringUtils.hasText(baseUsername) || baseUsername.trim().isEmpty()) {
            baseUsername = "user_" + providerUserId.substring(0, Math.min(providerUserId.length(), maxLength - 5));
        }
        if (baseUsername.trim().isEmpty()) {
            baseUsername = "oauthuser";
        }
        String finalUsername = baseUsername;
        int attempts = 0;
        while (userRepository.findByUsername(finalUsername).isPresent() && attempts < 100) {
            String suffix = UUID.randomUUID().toString().substring(0, attempts < 10 ? 3 : 5);
            int availableLengthForBase = 50 - 1 - suffix.length();
            String potentiallyShortenedBase = baseUsername;
            if (baseUsername.length() > availableLengthForBase) {
                potentiallyShortenedBase = baseUsername.substring(0, availableLengthForBase);
            }
            finalUsername = potentiallyShortenedBase + "_" + suffix;
            attempts++;
        }
        if (userRepository.findByUsername(finalUsername).isPresent()) {
            finalUsername = "user_" + UUID.randomUUID().toString().replace("-","").substring(0, Math.min(32, 50-5));
            if (userRepository.findByUsername(finalUsername).isPresent()) {
                throw new OAuth2AuthenticationException("Fatal error: Could not generate a unique username.");
            }
        }
        return finalUsername.substring(0, Math.min(finalUsername.length(), 50));
    }
}