package kko.traveldiary_login.member.adaptor.oauth;

import kko.traveldiary_login.member.application.required.TokenParser;
import kko.traveldiary_login.member.domain.TokenClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenParser implements TokenParser {
    private final JwtDecoder jwtDecoder;
    @Override
    public TokenClaims parse(String token, TokenType expectedType) {
        Jwt jwt;   // 스프링 Jwt — 이 메서드 안에서만 존재
        try {
            jwt = jwtDecoder.decode(token);
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid Token", e);
        }

        String type = jwt.getClaimAsString("type");
        if (!expectedType.value().equals(type)) {
            throw new BadCredentialsException("Unexpected Token: " + expectedType.value() + " expected, but " + type);
        }
        return new TokenClaims(
                Long.valueOf(jwt.getSubject()),
                jwt.getId(),
                expectedType
        );
    }
}
