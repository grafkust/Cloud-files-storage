package com.project.cloud.files.storage.service;

import com.project.cloud.files.storage.model.entity.user.User;
import freemarker.template.Configuration;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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

    @SneakyThrows
    public void sendEmail(User user) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "utf-8");
        helper.setSubject("Thank you for your registration, " + user.getUsername());
        helper.setTo(user.getEmail());
        String emailContent = getRegistrationEmailContent(user);
        helper.setText(emailContent, true);
        mailSender.send(mimeMessage);
    }

    @SneakyThrows
    private String getRegistrationEmailContent(User user) {
        final String url = "http://localhost:8087/";

        StringWriter writer = new StringWriter();
        Map<String, Object> model = new HashMap<>();
        model.put("username", user.getUsername());
        model.put("url", url);
        configuration.getTemplate("mail.ftlh").process(model, writer);
        return writer.getBuffer().toString();
    }


}
