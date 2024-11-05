package com.project.cloud.files.storage.service.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    void upload(MultipartFile file, String path);
}
