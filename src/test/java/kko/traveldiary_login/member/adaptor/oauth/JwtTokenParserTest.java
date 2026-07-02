package kko.traveldiary_login.member.adaptor.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import kko.traveldiary_login.member.application.required.TokenParser.TokenType;
import kko.traveldiary_login.member.domain.TokenClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtTokenParserTest {

    private JwtDecoder jwtDecoder;
    private JwtTokenParser sut;

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        sut = new JwtTokenParser(jwtDecoder);
    }

    @Test
    @DisplayName("parse: 유효한 access 토큰이면 sub/jti/type을 TokenClaims로 매핑한다")
    void parse_validAccessToken_returnsClaims() {
        when(jwtDecoder.decode("access-token")).thenReturn(jwt("42", "jti-1", "access"));

        TokenClaims claims = sut.parse("access-token", TokenType.ACCESS);

        assertThat(claims.memberId()).isEqualTo(42L);
        assertThat(claims.jti()).isEqualTo("jti-1");
        assertThat(claims.tokenType()).isEqualTo(TokenType.ACCESS);
    }

    @Test
    @DisplayName("parse: refresh 토큰이면 tokenType이 REFRESH로 매핑된다")
    void parse_validRefreshToken_returnsClaims() {
        when(jwtDecoder.decode("refresh-token")).thenReturn(jwt("7", "jti-2", "refresh"));

        TokenClaims claims = sut.parse("refresh-token", TokenType.REFRESH);

        assertThat(claims.memberId()).isEqualTo(7L);
        assertThat(claims.jti()).isEqualTo("jti-2");
        assertThat(claims.tokenType()).isEqualTo(TokenType.REFRESH);
    }

    @Test
    @DisplayName("parse: 기대한 타입과 토큰의 type이 다르면 BadCredentialsException을 던진다")
    void parse_typeMismatch_throws() {
        // 토큰은 access인데 REFRESH를 기대
        when(jwtDecoder.decode("access-token")).thenReturn(jwt("42", "jti-1", "access"));

        assertThatThrownBy(() -> sut.parse("access-token", TokenType.REFRESH))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("parse: 디코딩에 실패하면 BadCredentialsException으로 감싼다")
    void parse_decoderThrows_wrapsInBadCredentials() {
        when(jwtDecoder.decode("broken")).thenThrow(new JwtException("decode failed"));

        assertThatThrownBy(() -> sut.parse("broken", TokenType.ACCESS))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid Token");
    }

    @Test
    @DisplayName("parseAccessToken: ACCESS 타입으로 위임한다")
    void parseAccessToken_delegatesWithAccessType() {
        when(jwtDecoder.decode("access-token")).thenReturn(jwt("42", "jti-1", "access"));

        TokenClaims claims = sut.parseAccessToken("access-token");

        assertThat(claims.tokenType()).isEqualTo(TokenType.ACCESS);
        assertThat(claims.memberId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("parseRefreshToken: REFRESH 타입으로 위임한다")
    void parseRefreshToken_delegatesWithRefreshType() {
        when(jwtDecoder.decode("refresh-token")).thenReturn(jwt("42", "jti-2", "refresh"));

        TokenClaims claims = sut.parseRefreshToken("refresh-token");

        assertThat(claims.tokenType()).isEqualTo(TokenType.REFRESH);
        assertThat(claims.jti()).isEqualTo("jti-2");
    }

    private Jwt jwt(String subject, String jti, String type) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .jti(jti)
                .claim("type", type)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
