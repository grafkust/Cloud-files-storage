package com.project.cloud.files.storage.exception;

import com.project.cloud.files.storage.model.dto.UserDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotUniqueFieldException extends RuntimeException {

    private UserDto userDto;

    public NotUniqueFieldException(String message) {
        super(message);
    }
}
