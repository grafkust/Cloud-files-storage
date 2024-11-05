package com.project.cloud.files.storage.service.mail;

import com.project.cloud.files.storage.model.entity.user.User;

public interface MailService {

    void sendEmail(User user);
}
