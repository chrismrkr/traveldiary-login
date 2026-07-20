package kko.traveldiary_login.member.adaptor.oauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, RestAuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        http
                // JWT 기반이라 CSRF 불필요 (세션 안 씀)
                .csrf(AbstractHttpConfigurer::disable)
                // 세션을 안 만들도록 (stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 엔드포인트별 인증 정책
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                        // Swagger UI / OpenAPI 문서
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                // access token 검증 (공개키 기반 JwtDecoder 사용)
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(Customizer.withDefaults())
                                // access token 검증 중 실패 시, 어떤 ExceptionTranslationFilter가 동작할지 결정
                                .authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(authenticationEntryPoint));
        return http.build();
    }
}
