package com.project.cloud.files.storage.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        if (exception instanceof InternalAuthenticationServiceException) {
            log.error("Critical authentication error. URL: {}, HTTP Method: {}, Session ID: {}, Error message: {}",
                    request.getRequestURL(),
                    request.getMethod(),
                    request.getSession().getId(),
                    exception.getMessage(),
                    exception);

            response.sendRedirect("/");
        } else if (exception instanceof UsernameNotFoundException) {
            request.setAttribute("illegalArgument", exception.getMessage());
        } else if (exception instanceof BadCredentialsException) {
            String username = request.getParameter("username");
            request.setAttribute("illegalArgument", "Incorrect password");
            request.setAttribute("username", username);
        }
        request.getRequestDispatcher("/auth/login").forward(request, response);
    }
}
