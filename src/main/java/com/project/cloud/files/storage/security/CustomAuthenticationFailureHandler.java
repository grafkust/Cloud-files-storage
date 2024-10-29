package com.project.cloud.files.storage.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        if (exception instanceof InternalAuthenticationServiceException) {
            request.setAttribute("illegalArgument", exception.getMessage());
        } else if (exception instanceof BadCredentialsException) {
            String username = request.getParameter("username");
            request.setAttribute("illegalArgument", "Incorrect password");
            request.setAttribute("username", username);
        }
        request.getRequestDispatcher("/auth/login").forward(request, response);
    }
}
