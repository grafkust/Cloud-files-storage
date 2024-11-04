package com.project.cloud.files.storage.exception;

public class FileUploadException extends RuntimeException {

    public FileUploadException(String message, Exception e) {
        super(message, e);
    }
}
