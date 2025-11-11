package com.proximashare.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public void sendVerificationEmail(String toEmail, String username, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email - Proxima Share");

            String verificationLink = frontendUrl + "/verify-email?token=" + token;

            String htmlContent = buildEmailTemplate(username, verificationLink);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private String buildEmailTemplate(String username, String verificationLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                        .content { background-color: #f9f9f9; padding: 30px; border-radius: 5px; }
                        .button { display: inline-block; padding: 12px 30px; background-color: #4CAF50; color: white; 
                                  text-decoration: none; border-radius: 5px; margin: 20px 0; }
                        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Welcome to Proxima Share!</h1>
                        </div>
                        <div class="content">
                            <h2>Hello %s,</h2>
                            <p>Thank you for registering with Proxima Share. To complete your registration and activate your account, 
                            please verify your email address by clicking the button below:</p>
                
                            <a href="%s" class="button">Verify Email Address</a>
                
                            <p>Or copy and paste this link into your browser:</p>
                            <p style="word-break: break-all; color: #4CAF50;">%s</p>
                
                            <p><strong>This verification link will expire in 24 hours.</strong></p>
                
                            <p>If you didn't create an account with Proxima Share, please ignore this email.</p>
                        </div>
                        <div class="footer">
                            <p>© 2025 Proxima Share. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username, verificationLink, verificationLink);
    }
}