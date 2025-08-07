package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.Configuration.DomainConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class CustomErrorController implements ErrorController {

    private final DomainConfig domainConfig;

    @RequestMapping("/error")
    public void handleError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String error = request.getParameter("error");

        if (error != null) {
            String redirectUrl = domainConfig.getFrontend() + "/oauth/callback?oauth2=false&error=" + error;
            response.sendRedirect(redirectUrl);
            return;
        }

        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":\"NOT_FOUND\",\"code\":404,\"message\":\"Page not found\"}");
    }
}
