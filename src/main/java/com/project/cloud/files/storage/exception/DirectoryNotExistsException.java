package com.project.cloud.files.storage.exception;

public class DirectoryNotExistsException extends RuntimeException{
    public DirectoryNotExistsException(String message) {
        super(message);
    }
}
