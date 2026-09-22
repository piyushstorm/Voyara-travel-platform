package com.travelplatform.service;

import com.travelplatform.service.provider.EmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final EmailProvider emailProvider;
    private final ResourceLoader resourceLoader;
    private final String layoutTemplate;
    private final String providerName;

    public EmailService(
            @Value("${email.provider:mock}") String providerType,
            List<EmailProvider> providers,
            ResourceLoader resourceLoader) {

        this.providerName = providerType;
        this.emailProvider = resolveProvider(providerType, providers);
        this.resourceLoader = resourceLoader;
        this.layoutTemplate = loadTemplate("layout.html");
        logger.info("EmailService initialized with provider: {} ({})", providerType, emailProvider.getClass().getSimpleName());
    }

    private EmailProvider resolveProvider(String providerType, List<EmailProvider> providers) {
        String requested = providerType.toLowerCase();

        EmailProvider matched = providers.stream()
                .filter(p -> {
                    String className = p.getClass().getSimpleName().toLowerCase();
                    return className.contains(requested);
                })
                .findFirst()
                .orElse(null);

        if (matched != null) {
            return matched;
        }

        // Fallback to MockEmailProvider if requested provider is unavailable
        EmailProvider fallback = providers.stream()
                .filter(p -> p.getClass().getSimpleName().toLowerCase().contains("mock"))
                .findFirst()
                .orElse(null);

        if (fallback != null) {
            logger.warn("Configured email provider '{}' not found in available providers {}. Falling back to {}",
                    providerType, providers.stream().map(p -> p.getClass().getSimpleName()).toList(), fallback.getClass().getSimpleName());
            return fallback;
        }

        throw new IllegalArgumentException(
                "No email provider available for type: " + providerType + ". Available providers: " +
                        providers.stream().map(p -> p.getClass().getSimpleName()).toList());
    }

    // ─── Domain-specific Email Methods ──────────────────────────────────────────

    public void sendVerificationEmail(String to, String name, String verificationUrl, String frontendUrl) {
        sendEmail(
                to,
                "verification",
                "Verify your Voyara email address",
                Map.of(
                        "name", name != null ? name : "Traveler",
                        "verification_url", verificationUrl,
                        "frontend_url", frontendUrl
                )
        );
    }

    public void sendWelcomeEmail(String to, String name, String frontendUrl) {
        sendEmail(
                to,
                "welcome",
                "Welcome to Voyara!",
                Map.of(
                        "name", name != null ? name : "Traveler",
                        "frontend_url", frontendUrl
                )
        );
    }

    public void sendPasswordResetEmail(String to, String name, String resetUrl, String frontendUrl) {
        sendEmail(
                to,
                "password-reset",
                "Reset your Voyara password",
                Map.of(
                        "name", name != null ? name : "Traveler",
                        "reset_url", resetUrl,
                        "frontend_url", frontendUrl
                )
        );
    }

    public void sendPasswordResetConfirmedEmail(String to, String name, String frontendUrl) {
        sendEmail(
                to,
                "password-reset-confirmed",
                "Your Voyara password has been changed",
                Map.of(
                        "name", name != null ? name : "Traveler",
                        "frontend_url", frontendUrl
                )
        );
    }

    public void sendGroupTripInvitationEmail(String to, Map<String, String> variables) {
        String tripName = variables != null ? variables.getOrDefault("tripName", "a trip") : "a trip";
        sendEmail(
                to,
                "group-trip-invitation",
                "You're invited to join " + tripName + " on Voyara",
                variables != null ? variables : Map.of()
        );
    }

    public void sendEmail(String to, String templateName, String subject, Map<String, String> variables) {
        try {
            String contentTemplate = loadTemplate(templateName + ".html");

            // Template variable replacement
            String htmlContent = contentTemplate;
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                htmlContent = htmlContent.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }

            // Wrap in layout
            String finalHtml = htmlContent;
            if (layoutTemplate != null && !layoutTemplate.isEmpty()) {
                finalHtml = layoutTemplate.replace("{{content}}", htmlContent);
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    finalHtml = finalHtml.replace("{{" + entry.getKey() + "}}", entry.getValue());
                }
            }

            emailProvider.sendEmail(to, subject, finalHtml);
        } catch (Exception e) {
            logger.error("Failed to send email to {} via {}: {}", maskEmail(to), providerName, e.getMessage());
            // Re-throw so callers know the email failed
            throw new RuntimeException("Email delivery failed", e);
        }
    }

    private String loadTemplate(String filename) {
        try {
            Resource resource = resourceLoader.getResource("classpath:templates/email/" + filename);
            if (resource.exists()) {
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.warn("Could not load email template: {}", filename);
        }
        return "";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
