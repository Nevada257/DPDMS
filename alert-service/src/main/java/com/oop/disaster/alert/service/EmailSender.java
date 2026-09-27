package com.oop.disaster.alert.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends alert emails through JavaMail. SMTP settings come from the MAIL_*
 * environment variables; when MAIL_HOST is not set the email is not sent and
 * the attempt is logged as SIMULATED so the system still works in a demo.
 */
@Component
public class EmailSender {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String from;

    public EmailSender(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${spring.mail.host:}") String host,
                       @Value("${alerts.mail.from:dpdms-alerts@localhost}") String from) {
        this.mailSender = mailSender;
        this.host = host;
        this.from = from;
    }

    public boolean isConfigured() {
        return host != null && !host.isBlank() && mailSender.getIfAvailable() != null;
    }

    /** Sends the email; throws if the SMTP server rejects it. */
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.getObject().send(message);
    }
}
