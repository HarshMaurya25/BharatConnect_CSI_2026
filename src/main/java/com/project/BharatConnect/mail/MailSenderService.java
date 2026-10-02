package com.project.BharatConnect.mail;

import com.project.BharatConnect.dto.notification.MailDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailSenderService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public MailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendMail(MailDto dto) {
        try {
            MimeMessage mailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage, true, "UTF-8");
            helper.setTo(dto.getEmail());
            helper.setFrom(fromEmail);
            helper.setSubject(dto.getSubject());

            String text = dto.getText();
            helper.setText(text, true);

            mailSender.send(mailMessage);

        } catch (MessagingException e) {
            log.error("Mail Sender Error", e);
        }

    }
}
