package ru.tms.user.controller.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class OwnerIdResolver {

    private static final Logger logger = LoggerFactory.getLogger(OwnerIdResolver.class);

    public String resolve(JwtAuthenticationToken authentication) {
        String userId = authentication.getToken().getClaimAsString("userId");
        if (userId != null && !userId.isBlank()) {
            logger.debug("[OwnerIdResolver] Resolved ownerId from JWT claim: {}", userId);
            return userId;
        }
        String username = authentication.getName();
        logger.debug("[OwnerIdResolver] Resolved ownerId from username: {}", username);
        return username;
    }
}




