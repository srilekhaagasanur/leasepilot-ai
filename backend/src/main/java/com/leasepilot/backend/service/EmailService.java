package com.leasepilot.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Sends plain-text emails through SMTP. If no mail server is configured (MAIL_HOST is empty),
// the email is printed in the backend terminal instead, so everything still works while developing.
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String mailHost;
    private final String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${spring.mail.host:}") String mailHost,
                        @Value("${app.mail-from}") String from) {
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.from = from;
    }

    // Returns false if sending failed (the caller decides whether that matters)
    public boolean send(String to, String subject, String text) {
        if (mailHost.isBlank()) {
            log.info("""

                    ===== EMAIL (not sent: MAIL_HOST isn't set) =====
                    To: {}
                    Subject: {}

                    {}
                    =================================================""", to, subject, text);
            return true;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.getObject().send(message);
            return true;
        } catch (RuntimeException e) {
            log.error("Could not send email \"{}\"", subject, e);
            return false;
        }
    }
}
