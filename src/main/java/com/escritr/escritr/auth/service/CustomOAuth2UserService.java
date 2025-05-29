package com.escritr.escritr.auth.service;

import com.escritr.escritr.auth.model.UserDetailsImpl;
import com.escritr.escritr.user.domain.User;
import com.escritr.escritr.user.domain.UserAccountLink;
import com.escritr.escritr.user.repository.UserAccountLinkRepository;
import com.escritr.escritr.user.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAccountLinkRepository userAccountLinkRepository;


    private final OidcUserService oidcUserService = new OidcUserService();

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2UserToProcess;
        OidcUser oidcUser = null;

        String providerName = userRequest.getClientRegistration().getRegistrationId();
        logger.debug("Loading user for OAuth2 provider: {}", providerName);

        // Check if the request is an OIDC request (e.g., Google with 'openid' scope)
        if (userRequest instanceof OidcUserRequest) {
            logger.debug("Processing as OIDC user request for provider: {}", providerName);
            oidcUser = oidcUserService.loadUser((OidcUserRequest) userRequest);
            oAuth2UserToProcess = oidcUser; // DefaultOidcUser (returned by oidcUserService) implements OAuth2User
        } else {
            // For non-OIDC OAuth2 providers
            logger.debug("Processing as standard OAuth2 user request for provider: {}", providerName);
            oAuth2UserToProcess = super.loadUser(userRequest);
        }

        Map<String, Object> attributes = oAuth2UserToProcess.getAttributes();
        if (attributes == null || attributes.isEmpty()) {
            logger.error("OAuth2 provider '{}' did not return any user attributes.", providerName);
            throw new OAuth2AuthenticationException("OAuth2 provider " + providerName + " did not return user attributes.");
        }
        logger.debug("Attributes received from provider '{}': {}", providerName, attributes);


        String providerUserId = getProviderUserId(providerName, attributes);
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        // String picture = (String) attributes.get("picture");

        if (!StringUtils.hasText(providerUserId)) {
            logger.error("OAuth2 provider '{}' did not return a unique user ID. Attributes: {}", providerName, attributes);
            throw new OAuth2AuthenticationException("OAuth2 provider " + providerName + " did not return a unique user ID.");
        }
        logger.debug("Provider User ID: {}, Email: {}, Name: {}", providerUserId, email, name);



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

                // Handle scenario: email exists. Is it a local account or already an OAuth account
                if (userEntity.getPassword() != null && !userEntity.getPassword().isEmpty()) {
                    // This user registered with a password (local account).
                    // block linking if local account with password exists.
                    logger.warn("OAuth login attempt for email '{}' (provider '{}') which already exists as a local account with a password.", email, providerName);
                    throw new OAuth2AuthenticationException(
                            "An account already exists with this email address and is password-protected. " +
                                    "Please log in using your password, or link this provider from your account settings if supported."
                    );
                } else {
                    // Email exists, but no password (could be another OAuth account, or manually created without password).
                    // Link this new provider to the existing user.
                    logger.info("Linking new {} provider account (ID: {}) to existing user (ID: {}).",
                            providerName, providerUserId, userEntity.getId());
                    linkProviderToUser(userEntity, providerName, providerUserId);
                    updateUserFromOAuthAttributes(userEntity, attributes, email, name);
                }
            } else {
                // new user: Neither provider link nor email exists.
                logger.info("Creating new user for {} provider (ID: {}) and email: {}",
                        providerName, providerUserId, email);
                if (!StringUtils.hasText(email)) {
                    logger.warn("Email not provided by {} for new user with provider ID {}. Username generation may be less ideal.",
                            providerName, providerUserId);
                }
                String username = generateUniqueUsername(email, name, providerUserId);
                userEntity = new User(username, email, name); // Create with null password for OAuth
                userRepository.save(userEntity); // Save the new User entity first
                linkProviderToUser(userEntity, providerName, providerUserId); // Then link the provider
                logger.info("New user created with username: {} (ID: {}) and linked to {} (ID: {})",
                        username, userEntity.getId(), providerName, providerUserId);
            }
        }


        UserDetailsImpl customPrincipal;
        if (oidcUser != null) {
            logger.debug("Creating UserDetailsImpl for OIDC user. User entity ID: {}", userEntity.getId());
            customPrincipal = new UserDetailsImpl(userEntity, attributes, oidcUser.getIdToken(), oidcUser.getUserInfo());
        } else {
            logger.debug("Creating UserDetailsImpl for standard OAuth2 user. User entity ID: {}", userEntity.getId());
            customPrincipal = new UserDetailsImpl(userEntity, attributes);
        }
        logger.info("Successfully loaded/created user for provider '{}'. Returning UserDetailsImpl for username: {}",
                providerName, customPrincipal.getUsername());
        return customPrincipal;
    }

    private void linkProviderToUser(User user, String providerName, String providerUserId) {
        UserAccountLink newLink = new UserAccountLink(user, providerName, providerUserId);
        userAccountLinkRepository.save(newLink);
    }

    private void updateUserFromOAuthAttributes(User user, Map<String, Object> attributes, String emailFromProvider, String nameFromProvider) {
        boolean needsSave = false;

        if (StringUtils.hasText(nameFromProvider) && !nameFromProvider.equals(user.getName())) {
            logger.debug("Updating user name for user ID {} from '{}' to '{}'", user.getId(), user.getName(), nameFromProvider);
            user.setName(nameFromProvider);
            needsSave = true;
        }

        if (StringUtils.hasText(emailFromProvider) && !emailFromProvider.equals(user.getEmail())) {
            Optional<User> existingUserWithNewEmail = userRepository.findByEmail(emailFromProvider);
            if (existingUserWithNewEmail.isEmpty() || existingUserWithNewEmail.get().getId().equals(user.getId())) {
                logger.debug("Updating user email for user ID {} from '{}' to '{}'", user.getId(), user.getEmail(), emailFromProvider);
                user.setEmail(emailFromProvider);
                needsSave = true;
            } else {
                logger.warn("Attempted to update email for user ID {} to '{}', but this email is already taken by user ID {}.",
                        user.getId(), emailFromProvider, existingUserWithNewEmail.get().getId());
            }
        }

        if (needsSave) {
            logger.info("Saving updated user entity for user ID: {}", user.getId());
            userRepository.save(user);
        }
    }

    private String getProviderUserId(String providerName, Map<String, Object> attributes) {

        Object idValue;
        switch (providerName.toLowerCase()) {
            case "google":
                idValue = attributes.get("sub");
                break;

                //TODO:add other providers
//            case "github":
//                idValue = attributes.get("id");
//                break;
//            case "facebook":
//                idValue = attributes.get("id");
//                break;

            default:
                logger.warn("Unknown OAuth2 provider: {}. Attempting 'sub' or 'id' attribute for user ID.", providerName);
                idValue = attributes.get("sub");
                if (idValue == null) {
                    idValue = attributes.get("id");
                }
                break;
        }
        if (idValue == null) {
            logger.error("Could not determine provider_user_id for provider '{}' from attributes: {}", providerName, attributes);
            throw new OAuth2AuthenticationException("Could not determine the necessary information from the provider");
        }
        return idValue.toString();
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
            // Fallback if no email or name, use part of provider ID
            baseUsername = "user_" + providerUserId.substring(0, Math.min(providerUserId.length(), maxLength - 5)); // "user_" is 5 chars
        }
        // Ensure baseUsername is not empty after processing
        if (baseUsername.trim().isEmpty()) {
            baseUsername = "oauthuser"; // Absolute fallback if all else fails before suffixing
        }


        String finalUsername = baseUsername;
        int attempts = 0;
        while (userRepository.findByUsername(finalUsername).isPresent() && attempts < 100) {

            String suffix = UUID.randomUUID().toString().substring(0, attempts < 10 ? 3 : 5);
            int availableLengthForBase = 50 - 1 - suffix.length(); // 50 is DB limit
            String potentiallyShortenedBase = baseUsername;
            if (baseUsername.length() > availableLengthForBase) {
                potentiallyShortenedBase = baseUsername.substring(0, availableLengthForBase);
            }
            finalUsername = potentiallyShortenedBase + "_" + suffix;
            attempts++;
        }

        if (userRepository.findByUsername(finalUsername).isPresent()) {
            logger.error("Could not generate a unique username for OAuth user (email: [{}], name: [{}], providerId: [{}]). Fallback to UUID-based.",
                    StringUtils.hasText(email) ? email.length() : "null",
                    StringUtils.hasText(name) ? name.length() : "null",
                    providerUserId.length());

            finalUsername = "user_" + UUID.randomUUID().toString().replace("-","").substring(0, Math.min(32, 50-5));
            if (userRepository.findByUsername(finalUsername).isPresent()) {
                // This should be virtually impossible, might as well play the lottery
                throw new OAuth2AuthenticationException("Fatal error: Could not generate a unique username even with UUID. Please contact support.");
            }
        }
        return finalUsername.substring(0, Math.min(finalUsername.length(), 50)); // Final trim to DB limit
    }
}