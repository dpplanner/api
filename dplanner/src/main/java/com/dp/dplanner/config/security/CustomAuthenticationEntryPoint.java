package com.dp.dplanner.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        response.setHeader("Redirect-Uri", "/auth/refresh");
        response.setHeader("Original-Uri",  request.getRequestURI());
        response.setHeader("Original-Method", request.getMethod());

        JSONObject object = new JSONObject();
        object.put("status", "failure");
        object.put("data", null);
        object.put("message", "Not Authenticated Request");

        response.getWriter().write(object.toJSONString());
    }
}
