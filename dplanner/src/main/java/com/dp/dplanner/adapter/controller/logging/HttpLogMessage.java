package com.dp.dplanner.adapter.controller.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class HttpLogMessage {
    private String httpMethod;
    private String requestUri;
    private HttpStatus httpStatus;
    private String clientIp;
    private double elapsedTime;
    private String headers;
    private String requestParam;
    private String requestBody;
    private String responseBody;


    public HttpLogMessage(ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper,Double elapsedTime) {
        this.httpMethod = requestWrapper.getMethod();
        this.requestUri = requestWrapper.getRequestURI();
        this.httpStatus = HttpStatus.valueOf(responseWrapper.getStatus());
        this.clientIp = requestWrapper.getRemoteAddr();
        this.elapsedTime = elapsedTime;
        this.headers = extractHeaders(requestWrapper);
        this.requestParam = extractRequestParameters(requestWrapper);
        this.requestBody = extractRequestBody(requestWrapper);
        this.responseBody = extractResponseBody(responseWrapper);
    }
    public String toPrettierLog() {
        return String.format(
                "\n[REQUEST] %s %s %s (%s)" +
                        "\n>> CLIENT_IP: %s" +
                        "\n>> HEADERS: %s" +
                        "\n>> REQUEST_PARAM: %s" +
                        "\n>> REQUEST_BODY: %s" +
                        "\n>> RESPONSE_BODY: %s",
                this.getHttpMethod(), this.getRequestUri(), this.getHttpStatus(), this.getElapsedTime(),
                this.getClientIp(), this.getHeaders(), this.getRequestParam(), this.getRequestBody(), this.getResponseBody()
        );
    }


    private String extractHeaders(ContentCachingRequestWrapper requestWrapper) {
        Map headerMap = new HashMap();
        Enumeration<String> headerArray = requestWrapper.getHeaderNames();
        while (headerArray.hasMoreElements()) {
            String headerName = headerArray.nextElement();
            if (headerName.equals("authorization")) {
                headerMap.put(headerName, requestWrapper.getHeader(headerName).substring(0, 40));
            }else{
                headerMap.put(headerName, requestWrapper.getHeader(headerName));
            }
        }
        return headerMap.toString();
    }

    private String extractRequestParameters(ContentCachingRequestWrapper requestWrapper) {

        Map paramMap = new HashMap();
        Enumeration<String> parameterArray = requestWrapper.getParameterNames();
        while (parameterArray.hasMoreElements()) {
            String paramName = parameterArray.nextElement();
            paramMap.put(paramName, requestWrapper.getParameter(paramName));
        }
        return paramMap.toString();
    }

    private String extractRequestBody(ContentCachingRequestWrapper requestWrapper) {
        byte[] buf = requestWrapper.getContentAsByteArray();
        if (buf.length > 0) {
            try{
                return new String(buf, 0, buf.length, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "-";
            }
        }
        return "-";
    }

    private String extractResponseBody(ContentCachingResponseWrapper responseWrapper) {
        byte[] buf = responseWrapper.getContentAsByteArray();
        int size = Math.min(buf.length, 1000);
        if (buf.length > 0) {
            try{
                return new String(buf, 0, size, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "-";
            }
        }
        return "-";
    }
}
