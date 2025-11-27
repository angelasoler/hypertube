package com.hypertube.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails
 * Handles password reset emails and verification emails
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@hypertube.com}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Sends password reset email asynchronously
     *
     * @param toEmail Recipient email address
     * @param resetToken Password reset token
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("HyperTube - Password Reset Request");
            message.setText(
                "Hello,\n\n" +
                "You have requested to reset your password for HyperTube.\n\n" +
                "Please click the link below to reset your password:\n" +
                resetUrl + "\n\n" +
                "This link will expire in 15 minutes.\n\n" +
                "If you did not request this password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "The HyperTube Team"
            );

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            // Don't throw exception - email failure shouldn't break the flow
        }
    }

    /**
     * Sends email verification email asynchronously
     *
     * @param toEmail Recipient email address
     * @param verificationToken Email verification token
     */
    @Async
    public void sendEmailVerificationEmail(String toEmail, String verificationToken) {
        try {
            String verifyUrl = frontendUrl + "/verify-email?token=" + verificationToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("HyperTube - Verify Your Email Address");
            message.setText(
                "Hello,\n\n" +
                "Welcome to HyperTube! Please verify your email address by clicking the link below:\n\n" +
                verifyUrl + "\n\n" +
                "If you did not create an account, please ignore this email.\n\n" +
                "Best regards,\n" +
                "The HyperTube Team"
            );

            mailSender.send(message);
            log.info("Email verification sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email verification to: {}", toEmail, e);
            // Don't throw exception - email failure shouldn't break the flow
        }
    }
}
