package com.project.cloud.files.storage.controller.exception;

import com.project.cloud.files.storage.exception.NotUniqueFieldException;
import com.project.cloud.files.storage.model.dto.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandlerController {


    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handle404Error() {
        return "error/404";
    }

    @ExceptionHandler(NotUniqueFieldException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleNotUniqueFieldError(NotUniqueFieldException e, Model model) {
        UserDto userDto = e.getUserDto();
        String message = e.getMessage();

        if (message.contains("username"))
            userDto.setUsername("");

        if (message.contains("email"))
            userDto.setEmail("");

        model.addAttribute("fieldNotUnique", message);
        model.addAttribute("userDto", userDto);
        return "auth/registration";
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public void handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e){
        e.printStackTrace();
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception e) {
        e.printStackTrace();
        return "error/500";
    }


}
