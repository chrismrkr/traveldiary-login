package kko.traveldiary_login.member.adaptor.oauth;

import kko.traveldiary_login.member.adaptor.oauth.config.JwtProperties;
import kko.traveldiary_login.member.application.required.TokenIssuer;
import kko.traveldiary_login.member.domain.Member;
import kko.traveldiary_login.member.domain.TokenPair;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuer {
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private static final String ACCESS_TK_NM = "access";
    private static final String REFRESH_TK_NM = "refresh";

    @Override
    public TokenPair issue(Member member) {
        String access = createToken(member, jwtProperties.accessTokenTtl(), ACCESS_TK_NM);
        String refresh = createToken(member, jwtProperties.refreshTokenTtl(), REFRESH_TK_NM);
        return new TokenPair(access, refresh);
    }

    @Override
    public String reissueAccessToken(Member member) {
        return createToken(member, jwtProperties.accessTokenTtl(), ACCESS_TK_NM);
    }

    private String createToken(Member member, Duration ttl, String type) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(member.getId().toString())
                .claim("role", member.getRole().name())
                .claim("type", type)              // access / refresh 구분
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(ttl))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
