package com.oop.disaster.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailAlertService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailAlertService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String recipient, String subject, String message) {
        if (fromEmail == null || fromEmail.isBlank()) {
            System.out.println("EMAIL DEMO: " + recipient + " -> " + message);
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromEmail);
        mail.setTo(recipient);
        mail.setSubject(subject);
        mail.setText(message);
        mailSender.send(mail);
    }
}
