package kko.traveldiary_login.member.adaptor.oauth;

import kko.traveldiary_login.member.adaptor.oauth.exception.TokenExpiredException;
import kko.traveldiary_login.member.application.required.TokenParser;
import kko.traveldiary_login.member.domain.TokenClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtTokenParser implements TokenParser {
    private final JwtDecoder jwtDecoder;
    @Override
    public TokenClaims parse(String token, TokenType expectedType) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);

        } catch (JwtValidationException e) {
            throw new TokenExpiredException("Expired Token", e);
        } catch (JwtException ex) {
            throw new BadCredentialsException("Invalid Token", ex);
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
