package com.project.cloud.files.storage.service;

import com.project.cloud.files.storage.exception.EmailOperationException;
import com.project.cloud.files.storage.model.entity.user.User;
import freemarker.template.Configuration;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailService {

    private final Configuration configuration;
    private final JavaMailSender mailSender;

    @Value("${application.url}")
    private String applicationUrl;


    public void sendEmail(User user) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "utf-8");
            helper.setSubject("Thank you for your registration, " + user.getUsername());
            helper.setTo(user.getEmail());
            String emailContent = getRegistrationEmailContent(user);
            helper.setText(emailContent, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new EmailOperationException("Failed to send Email", e);
        }
    }

    private String getRegistrationEmailContent(User user) {

        StringWriter writer = new StringWriter();
        Map<String, Object> model = new HashMap<>();
        model.put("username", user.getUsername());
        model.put("url", applicationUrl);
        try {
            configuration.getTemplate("mail.ftlh").process(model, writer);
        } catch (Exception e) {
            throw new EmailOperationException("Failed to get template for Email", e);
        }
        return writer.getBuffer().toString();
    }


}
