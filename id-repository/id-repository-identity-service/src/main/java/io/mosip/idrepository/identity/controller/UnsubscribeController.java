package io.mosip.idrepository.identity.controller;

import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.identity.dto.UnsubscribeRequestDto;
import io.mosip.idrepository.identity.service.UnsubscribeService;
import io.mosip.kernel.core.logger.spi.Logger;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Collections;
import java.util.Map;
import java.util.Locale;
import java.util.Base64;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class UnsubscribeController {

    Logger mosipLogger = IdRepoLogger.getLogger(UnsubscribeController.class);

    @Autowired
    private UnsubscribeService unsubscribeService;

    @Configuration
    public static class UnsubscribeSecurityConfig {
        @Bean
        public WebSecurityCustomizer unsubscribeWebSecurityCustomizer() {
            // PRODUCTION & LOCAL: Completely bypass Spring Security and Keycloak AuthFilter for these public endpoints.
            // Required because users clicking the unsubscribe link from an email will not have a Keycloak session.
            return (web) -> web.ignoring().requestMatchers(
                    new AntPathRequestMatcher("/unsubscribe", "POST"),
                    new AntPathRequestMatcher("/unsubscribe/status", "GET")
            );
        }
    }

    /**
     * POST /v1/identity/unsubscribe
     *
     * Accepts an unsubscribe request, validates the payload, checks for
     * duplicate requests, and deactivates the associated UIN.
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(
            @Valid @RequestBody UnsubscribeRequestDto request) {

        // PRODUCTION & LOCAL: Inject a system context to satisfy downstream MOSIP security and audit checks.
        // Since this is a public endpoint, there is no logged-in user. We log this action under "UNSUBSCRIBE_SERVICE".
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("UNSUBSCRIBE_SERVICE", null, Collections.emptyList())
        );

        try {
            // Validate JWT Token to prove email ownership before proceeding
            String email = verifyAndExtractEmail(request.getToken());
            
            // Pass the trusted email downstream via the DTO
            request.setEmail(email);

            // Guard: consent must be explicitly "true"
            if (!"true".equalsIgnoreCase(request.getConsent())) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "status", "FAILED",
                                "message", "Consent is required to proceed with unsubscription."
                        ));
            }

            // Guard: idempotent — already unsubscribed
            if (unsubscribeService.isAlreadyUnsubscribed(email)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "status", "ALREADY_UNSUBSCRIBED",
                                "message", "You are already unsubscribed from MOSIP emails. No further action is required."
                        ));
            }

            unsubscribeService.processUnsubscribe(request);
            mosipLogger.info(IdRepoSecurityManager.getUser(), "UnsubscribeController",
                    "unsubscribe", "Unsubscribe processed successfully");
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "You have successfully unsubscribed. Your associated UIN and related data have been deactivated."
            ));
        } catch (IdRepoAppException e) {
            mosipLogger.error(IdRepoSecurityManager.getUser(), "UnsubscribeController",
                    "unsubscribe", e.getErrorText());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of(
                            "status", "FAILED",
                            "message", e.getErrorText()
                    ));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * GET /v1/identity/unsubscribe/status?email=...
     *
     * Allows checking unsubscribe status without side effects.
     */
    @GetMapping("/unsubscribe/status")
    public ResponseEntity<Map<String, Object>> checkStatus(@RequestParam String token) {
        try {
            String email = verifyAndExtractEmail(token);
            boolean unsubscribed = unsubscribeService.isAlreadyUnsubscribed(email);
            
            // Omit raw email from response to prevent data leakage/probing
            return ResponseEntity.ok(Map.of("unsubscribed", unsubscribed));
        } catch (IdRepoAppException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "FAILED", "message", e.getErrorText()));
        }
    }

    private String verifyAndExtractEmail(String token) throws IdRepoAppException {
        if (token == null || token.isBlank() || !token.contains(".")) {
            throw new IdRepoAppException("INVALID_TOKEN", "Unsubscribe token is missing or invalid.");
        }
        try {
            // PRODUCTION TODO: Use MOSIP KeyManager/TokenValidator to verify the JWT signature
            // before trusting any claims inside it. Replace the fallback logic below.
            /*
            Claims claims = mosipTokenValidator.validateAndGetClaims(token);
            String email = claims.get("email", String.class);
            */
            
            // INSECURE FALLBACK:
            String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
            JsonNode claims = new ObjectMapper().readTree(payload);
            if (claims.has("email") && !claims.get("email").asText().isBlank()) {
                return claims.get("email").asText().toLowerCase(Locale.ENGLISH).trim();
            }
            throw new IdRepoAppException("INVALID_TOKEN", "Token does not contain an email claim.");
        } catch (Exception e) {
            if (e instanceof IdRepoAppException) {
                throw (IdRepoAppException) e;
            }
            throw new IdRepoAppException("INVALID_TOKEN", "Failed to parse unsubscribe token.");
        }
    }

    /**
     * =========================================================================
     * LOCAL DEVELOPMENT ONLY:
     * Completely bypasses Spring Security globally so MOSIP AuthFilter is never
     * triggered. Injects mock permissions so you can test internal and public 
     * endpoints via curl/Postman without needing any Keycloak token. 
     * This will NOT run in production due to the @Profile("local") annotation.
     * =========================================================================
     */
    @Configuration
    @org.springframework.context.annotation.Profile("local")
    public static class LocalDevSecurityBypassConfig {
        @Bean
        public WebSecurityCustomizer globalLocalWebSecurityCustomizer() {
            return (web) -> web.ignoring().requestMatchers(new AntPathRequestMatcher("/**"));
        }

        @Bean(name = "authFilter")
        @org.springframework.context.annotation.Primary
        public jakarta.servlet.Filter mockMosipAuthFilter() {
            return new org.springframework.web.filter.GenericFilterBean() {
                @Override
                public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, jakarta.servlet.FilterChain chain) throws java.io.IOException, jakarta.servlet.ServletException {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken("LOCAL_DEV_USER", null, java.util.List.of(
                                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ID_REPOSITORY"),
                                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_REGISTRATION_PROCESSOR")
                            ))
                    );
                    chain.doFilter(request, response);
                }
            };
        }
    }
}