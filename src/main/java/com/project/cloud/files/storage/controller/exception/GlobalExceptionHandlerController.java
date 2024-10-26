package com.project.cloud.files.storage.controller.exception;

import com.project.cloud.files.storage.exception.NotUniqueFieldException;
import com.project.cloud.files.storage.model.dto.UserDto;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;


@ControllerAdvice
public class GlobalExceptionHandlerController {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public void handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        e.printStackTrace();
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleInternalAuthenticationServiceException(InternalAuthenticationServiceException e) {
        e.printStackTrace();
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public void handleBadCredentialsException(BadCredentialsException e) {
        e.printStackTrace();
    }


    @ExceptionHandler(FileUploadException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleFileUploadException(FileUploadException e) {
        System.out.println(e.getMessage());
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



    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception e) {
        e.printStackTrace();
        return "error/500-page";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound() {
        return "error/404-page";
    }


    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNoResourceFoundException(HttpServletResponse response) throws IOException {
        response.sendRedirect("/");
    }


}
