package kko.traveldiary_login.member.adaptor.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import kko.traveldiary_login.member.adaptor.oauth.config.JwtProperties;
import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.Member;
import kko.traveldiary_login.member.domain.Role;
import kko.traveldiary_login.member.domain.TokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenIssuerTest {

    private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
    private static final Duration REFRESH_TTL = Duration.ofDays(14);

    private JwtTokenIssuer tokenIssuer;
    private JwtDecoder jwtDecoder;

    private final Member member =
            Member.reconstitute(42L, AuthProvider.GOOGLE, "google-sub", "user@example.com", "홍길동", Role.USER);

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);

        JwtProperties jwtProperties = new JwtProperties(ACCESS_TTL, REFRESH_TTL);
        tokenIssuer = new JwtTokenIssuer(jwtEncoder, jwtProperties);
        jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Test
    @DisplayName("issue: access/refresh 토큰을 모두 발급하고 서명이 검증된다")
    void issue_returnsVerifiableTokenPair() {
        TokenPair pair = tokenIssuer.issue(member);

        assertThat(pair.accessToken()).isNotBlank();
        assertThat(pair.refreshToken()).isNotBlank();
        // 디코딩이 성공하면 서명 검증을 통과한 것
        assertThat(jwtDecoder.decode(pair.accessToken())).isNotNull();
        assertThat(jwtDecoder.decode(pair.refreshToken())).isNotNull();
    }

    @Test
    @DisplayName("issue: access 토큰에 subject, role, type=access 클레임이 담긴다")
    void issue_accessTokenHasExpectedClaims() {
        TokenPair pair = tokenIssuer.issue(member);

        Jwt access = jwtDecoder.decode(pair.accessToken());

        assertThat(access.getSubject()).isEqualTo("42");
        assertThat(access.getClaimAsString("role")).isEqualTo("USER");
        assertThat(access.getClaimAsString("type")).isEqualTo("access");
    }

    @Test
    @DisplayName("issue: refresh 토큰은 type=refresh 클레임을 가진다")
    void issue_refreshTokenHasRefreshType() {
        TokenPair pair = tokenIssuer.issue(member);

        Jwt refresh = jwtDecoder.decode(pair.refreshToken());

        assertThat(refresh.getSubject()).isEqualTo("42");
        assertThat(refresh.getClaimAsString("type")).isEqualTo("refresh");
    }

    @Test
    @DisplayName("issue: 토큰 만료시간이 JwtProperties의 TTL만큼 설정된다")
    void issue_setsExpiryAccordingToTtl() {
        TokenPair pair = tokenIssuer.issue(member);

        Jwt access = jwtDecoder.decode(pair.accessToken());
        Jwt refresh = jwtDecoder.decode(pair.refreshToken());

        // JWT 시간 클레임은 초 단위로 truncate 되므로 약간의 오차를 허용
        assertThat(Duration.between(access.getIssuedAt(), access.getExpiresAt()))
                .isBetween(ACCESS_TTL.minusSeconds(2), ACCESS_TTL.plusSeconds(2));
        assertThat(refresh.getExpiresAt())
                .isBetween(Instant.now().plus(REFRESH_TTL).minusSeconds(5),
                        Instant.now().plus(REFRESH_TTL).plusSeconds(5));
    }

    @Test
    @DisplayName("reissueAccessToken: type=access 인 access 토큰만 재발급한다")
    void reissueAccessToken_returnsAccessToken() {
        String token = tokenIssuer.reissueAccessToken(member);

        Jwt access = jwtDecoder.decode(token);

        assertThat(access.getSubject()).isEqualTo("42");
        assertThat(access.getClaimAsString("role")).isEqualTo("USER");
        assertThat(access.getClaimAsString("type")).isEqualTo("access");
    }
}
