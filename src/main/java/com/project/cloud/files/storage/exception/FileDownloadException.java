package com.project.cloud.files.storage.exception;

public class FileDownloadException extends RuntimeException {
    public FileDownloadException(String message, Exception e) {
        super(message, e);
    }
}
