package kko.traveldiary_login.member.adaptor.oauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kko.traveldiary_login.member.adaptor.inbound.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 만료 vs 무효 구분 (선택): 리소스 서버가 준 에러 설명에 "expired"가 있으면 만료로 분류
        String code = authException.getMessage() != null
                && authException.getMessage().toLowerCase().contains("expired")
                ? "EXPIRED_TOKEN" : "INVALID_TOKEN";

        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse(code, "Authentication failed"));

    }
}
