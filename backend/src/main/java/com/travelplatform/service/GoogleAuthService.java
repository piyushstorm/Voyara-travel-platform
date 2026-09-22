package com.travelplatform.service;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

@Service
public class GoogleAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthService.class);
    private static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String EXPECTED_ISSUER_1 = "https://accounts.google.com";
    private static final String EXPECTED_ISSUER_2 = "accounts.google.com";

    @Value("${google.client.id:}")
    private String googleClientId;

    private final RestTemplate restTemplate = new RestTemplate();
    private JWKSet jwkSet;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            refreshJwks();
            logger.info("Google OAuth initialized with client ID prefix: {}",
                    googleClientId.substring(0, Math.min(12, googleClientId.length())));
        } else {
            logger.warn("Google OAuth client ID not configured. Google login will be disabled.");
        }
    }

    public boolean isConfigured() {
        return googleClientId != null && !googleClientId.isBlank();
    }

    /**
     * Refresh Google's public keys from their JWKS endpoint.
     */
    private void refreshJwks() {
        try {
            String jwksJson = restTemplate.getForObject(GOOGLE_JWKS_URL, String.class);
            if (jwksJson != null) {
                this.jwkSet = JWKSet.parse(jwksJson);
                logger.info("Google JWKS refreshed successfully ({} keys)", jwkSet.getKeys().size());
            }
        } catch (Exception e) {
            logger.error("Failed to refresh Google JWKS", e);
        }
    }

    /**
     * Verify a Google ID token and extract user info.
     */
    public GoogleUserInfo verifyIdToken(String idTokenString) {
        if (!isConfigured()) {
            logger.warn("Google OAuth not configured, cannot verify token");
            return null;
        }

        if (jwkSet == null) {
            refreshJwks();
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(idTokenString);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Verify issuer
            String issuer = claims.getIssuer();
            if (!EXPECTED_ISSUER_1.equals(issuer) && !EXPECTED_ISSUER_2.equals(issuer)) {
                logger.warn("Invalid Google token issuer: {}", issuer);
                return null;
            }

            // Verify audience (client ID)
            List<String> audience = claims.getAudience();
            if (audience == null || !audience.contains(googleClientId)) {
                logger.warn("Google token audience mismatch");
                return null;
            }

            // Verify expiry
            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                logger.warn("Google token expired");
                return null;
            }

            // Verify signature against Google's public keys
            String keyId = signedJWT.getHeader().getKeyID();
            JWK jwk = jwkSet.getKeyByKeyId(keyId);
            if (jwk == null) {
                // Key ID not found - refresh JWKS and retry once
                refreshJwks();
                jwk = jwkSet.getKeyByKeyId(keyId);
            }
            if (jwk == null) {
                logger.warn("Google public key not found for kid: {}", keyId);
                return null;
            }

            RSAPublicKey publicKey = ((RSAKey) jwk).toRSAPublicKey();
            RSASSAVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                logger.warn("Google ID token signature verification failed");
                return null;
            }

            // Extract user info
            String sub = claims.getSubject();
            String email = claims.getStringClaim("email");
            String name = claims.getStringClaim("name");
            Boolean emailVerified = claims.getBooleanClaim("email_verified");

            if (sub == null || email == null) {
                logger.warn("Google token missing required claims (sub or email)");
                return null;
            }

            logger.info("Google ID token verified successfully for email={}", email);
            return new GoogleUserInfo(sub, email, name, Boolean.TRUE.equals(emailVerified));
        } catch (Exception e) {
            logger.error("Failed to verify Google ID token", e);
            return null;
        }
    }

    public record GoogleUserInfo(String sub, String email, String name, boolean emailVerified) {}
}
