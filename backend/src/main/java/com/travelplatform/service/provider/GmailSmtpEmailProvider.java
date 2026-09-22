package com.travelplatform.service.provider;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component("gmailSmtpEmailProvider")
@ConditionalOnProperty(name = "email.provider", havingValue = "gmail")
public class GmailSmtpEmailProvider implements EmailProvider {

    private static final Logger logger = LoggerFactory.getLogger(GmailSmtpEmailProvider.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from:${spring.mail.from:${spring.mail.username:}}}")
    private String fromEmail;

    @Value("${mail.from.name:${spring.mail.from-name:Voyara}}")
    private String fromName;

    public GmailSmtpEmailProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void init() {
        if (mailSender instanceof JavaMailSenderImpl senderImpl) {
            if ((fromEmail == null || fromEmail.isBlank()) && senderImpl.getUsername() != null && !senderImpl.getUsername().isBlank()) {
                fromEmail = senderImpl.getUsername();
            }
            String pass = senderImpl.getPassword();
            if (pass != null && pass.contains(" ")) {
                senderImpl.setPassword(pass.replaceAll("\\s+", ""));
            }
        }
    }

    @Override
    public void sendEmail(String to, String subject, String htmlContent) {
        if (fromEmail == null || fromEmail.isBlank()) {
            logger.error("MAIL_FROM is not configured. Cannot send email to {}", maskEmail(to));
            throw new RuntimeException("Email configuration incomplete: MAIL_FROM not set");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Email sent via Gmail SMTP to {}", maskEmail(to));
        } catch (MessagingException e) {
            logger.error("Gmail SMTP error sending email to {}: {}", maskEmail(to), e.getMessage());
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error sending email to {}: {}", maskEmail(to), e.getMessage());
            throw new RuntimeException("Email delivery failed", e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
