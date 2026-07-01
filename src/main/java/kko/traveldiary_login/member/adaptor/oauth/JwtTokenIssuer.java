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
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuer {
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private static final String ACCESS_TK_NM = "access";
    private static final String REFRESH_TK_NM = "refresh";

    @Override
    public TokenPair issue(Member member) {
        String jti = UUID.randomUUID().toString();
        String access = createToken(member, null, jwtProperties.accessTokenTtl(), ACCESS_TK_NM);
        String refresh = createToken(member, jti, jwtProperties.refreshTokenTtl(), REFRESH_TK_NM);
        return new TokenPair(access, refresh, jti);
    }

    @Override
    public String reissueAccessToken(Member member) {
        return createToken(member, null, jwtProperties.accessTokenTtl(), ACCESS_TK_NM);
    }

    private String createToken(Member member, String jti, Duration ttl, String type) {
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .subject(member.getId().toString())
                .claim("role", member.getRole().name())
                .claim("type", type)              // access / refresh 구분
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(ttl));
        if(jti != null) {
            builder.id(jti);
        }
        JwtClaimsSet claims = builder.build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
