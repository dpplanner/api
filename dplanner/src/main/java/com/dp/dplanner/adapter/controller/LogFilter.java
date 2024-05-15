package com.dp.dplanner.adapter.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class LogFilter extends OncePerRequestFilter {

    private final String REQUEST_ID = "request_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        var cachingRequestWrapper = new ContentCachingRequestWrapper(request);
        var cachingResponseWrapper = new ContentCachingResponseWrapper(response);
        var requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(REQUEST_ID, requestId);

        var startTime = System.currentTimeMillis();
        filterChain.doFilter(cachingRequestWrapper, cachingResponseWrapper);
        var endTime = System.currentTimeMillis();
        var elapsedTime = endTime - startTime;

        try {
            HttpLogMessage httpLogMessage = new HttpLogMessage(cachingRequestWrapper, cachingResponseWrapper, (double) elapsedTime);
            log.info(httpLogMessage.toPrettierLog());
            cachingResponseWrapper.copyBodyToResponse();
        } catch (Exception e) {
            log.error("{} logging 실패", this.getClass().getSimpleName());
        } finally {
            MDC.remove(requestId);
        }

    }
}
