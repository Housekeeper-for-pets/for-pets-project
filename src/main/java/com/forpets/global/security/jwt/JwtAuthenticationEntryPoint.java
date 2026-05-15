package com.forpets.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.exception.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 요청이 보호된 API에 접근할 때 JSON 에러 응답을 내려줍니다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ErrorCode errorCode = resolveErrorCode(request);

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.fail(ErrorResponse.of(errorCode, request.getRequestURI()));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private ErrorCode resolveErrorCode(HttpServletRequest request) {
        Object jwtException = request.getAttribute("jwtException");

        if ("EXPIRED_TOKEN".equals(jwtException)) {
            return CommonErrorCode.EXPIRED_TOKEN;
        }

        if ("INVALID_TOKEN".equals(jwtException)) {
            return CommonErrorCode.INVALID_TOKEN;
        }

        return CommonErrorCode.UNAUTHORIZED;
    }
}