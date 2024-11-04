package com.project.cloud.files.storage.exception;

public class StorageOperationException extends RuntimeException {
    public StorageOperationException(String message, Exception e) {
        super(message, e);
    }
}
