package com.project.cloud.files.storage.service.file.impl;

import com.project.cloud.files.storage.exception.FileUploadException;
import com.project.cloud.files.storage.service.file.FileUploadService;
import com.project.cloud.files.storage.service.storage.StorageService;
import com.project.cloud.files.storage.util.validator.PathUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final StorageService storageService;
    private final PathUtil pathUtil;

    @Override
    public void upload(MultipartFile file, String path) {
        try {

            path = pathUtil.normalizeStoragePath(path);

            String fullPath = pathUtil.createFullPath(file, path);

            performUpload(file, fullPath);
        } catch (Exception e) {
            throw new FileUploadException("Failed to upload file: " + file.getOriginalFilename(), e);
        }
    }

    private void performUpload(MultipartFile file, String fullPath) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            storageService.store(inputStream, fullPath);
        }
    }


}
