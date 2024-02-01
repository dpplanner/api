package com.dp.dplanner.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

    private final JwtTokenProvider tokenProvider;
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String accessToken = getToken((HttpServletRequest) request);
        String allowedUri = "/|/login|/logout|/auth/.*|/swagger-ui/.*|/v3/.*";  // ToDo 컨트롤러 요청 중에 이것 말고 또 허용해야 하는 요청 파악하기

        if (accessToken != null && tokenProvider.verify(accessToken)) {
            Authentication authentication = tokenProvider.getAuthentication(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } else if ((((HttpServletRequest) request).getRequestURI().matches(allowedUri))) {
            chain.doFilter(request, response);
        } else {
            // accessToken verify 안 될 때는 /auth/refresh 리다이렉트 주소와 기존 요청 담아서
            // ToDO 헤더에 담을 정보 및 헤더 말고 다른 방식으로 전달할지 파악
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setStatus(401);
            httpServletResponse.setHeader("Redirect-Uri", "/auth/refresh");
            httpServletResponse.setHeader("Original-Uri", ((HttpServletRequest) request).getRequestURI());
            httpServletResponse.setHeader("Original-Method", ((HttpServletRequest) request).getMethod());
            // doFilter하지 않고 바로 반환
        }

    }

    private static String getToken(HttpServletRequest request) {
        String authorizationHeader= request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}
