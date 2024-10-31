package com.project.cloud.files.storage.controller.exception;

import com.project.cloud.files.storage.exception.NotUniqueFieldException;
import com.project.cloud.files.storage.model.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.Set;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandlerController {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public String handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                      HttpServletRequest request, Model model) {
        log.error("Method {} not allowed for URL: {}. Supported methods: {}. Error: {}",
                request.getMethod(),
                request.getRequestURL(),
                e.getSupportedHttpMethods(),
                e.getMessage(),
                e);

        Set<HttpMethod> supportedMethods = e.getSupportedHttpMethods();
        if (supportedMethods != null) {
            model.addAttribute("supportedMethods", supportedMethods);
        }
        model.addAttribute("requestMethod", request.getMethod());

        return "error/405-page";
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleInternalAuthenticationServiceException(InternalAuthenticationServiceException e,
                                                               HttpServletRequest request) {
        logHttpRequestException("Authentication failed", request, e, true);
        return "error/500-page";
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String handleBadCredentialsException() {
        return "redirect:/auth/login";
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

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoHandlerFoundException e) {
        log.warn("Requested page not found. HTTP Method: {}, URL: {}",
                e.getHttpMethod(),
                e.getRequestURL());
        return "error/404-page";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNoResourceFoundException(NoResourceFoundException e, HttpServletResponse response) throws IOException {
        String resourcePath = e.getResourcePath();
        String resourceName = resourcePath.contains(".") ? "Static resource" : "Requested page";
        String message = String.format("%s not found: %s", resourceName, resourcePath);
        log.warn(message);
        response.sendRedirect("/");
    }

    @ExceptionHandler({FileSizeLimitExceededException.class, MaxUploadSizeExceededException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handleMaxUploadSizeExceededException(HttpServletResponse response, HttpServletRequest request, Exception e) throws IOException {
        logHttpRequestException("File size limit exception", request, e, false);
        response.sendRedirect("/?error=size");
    }

    @ExceptionHandler(FileUploadException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUploadFileException( HttpServletRequest request, Exception e) {
        logHttpRequestException("Unexpected upload file exception", request, e, true);
        return "error/500-page";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception e, HttpServletRequest request) {
        logHttpRequestException("Application unexpected error", request, e, true);
        return "error/500-page";
    }


    private void logHttpRequestException(String errorMessage, HttpServletRequest request, Exception e, boolean includeStackTrace) {
        String logMessage = String.format("%s. URL: {}, HTTP Method: {}, Session ID: {}, Error message: {}", errorMessage);
        Object[] logParams;

        if (includeStackTrace) {
            logParams = new Object[]{
                    request.getRequestURL(),
                    request.getMethod(),
                    request.getSession().getId(),
                    e.getMessage(),
                    e
            };
        } else {
            logParams = new Object[]{
                    request.getRequestURL(),
                    request.getMethod(),
                    request.getSession().getId(),
                    e.getMessage()
            };
        }

        if (includeStackTrace) {
            log.error(logMessage, logParams);
        } else {
            log.warn(logMessage, logParams);
        }
    }


}
