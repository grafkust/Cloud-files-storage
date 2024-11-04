package com.project.cloud.files.storage.exception;

public class EmailOperationException extends RuntimeException {
    public EmailOperationException(String message, Exception e) {
        super(message, e);
    }
}
