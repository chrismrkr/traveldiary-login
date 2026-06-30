package kko.traveldiary_login.member.adaptor.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.webtoken.JsonWebSignature;
import java.io.IOException;
import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.OAuthInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

class GoogleOAuthVerifierTest {

    private GoogleIdTokenVerifier delegate;
    private GoogleOAuthVerifier sut;

    @BeforeEach
    void setUp() throws Exception {
        delegate = mock(GoogleIdTokenVerifier.class);
        // 생성자는 내부에서 실제 GoogleIdTokenVerifier를 만들므로 더미 clientId로 생성한 뒤
        // private verifier 필드를 모킹된 것으로 교체한다.
        sut = new GoogleOAuthVerifier("test-client-id");
        ReflectionTestUtils.setField(sut, "verifier", delegate);
    }

    @Test
    @DisplayName("provider: GOOGLE을 반환한다")
    void provider_returnsGoogle() {
        assertThat(sut.provider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    @DisplayName("verify: 검증 성공 시 payload의 sub/email/name을 OAuthInfo로 매핑한다")
    void verify_mapsPayloadToOAuthInfo() throws Exception {
        GoogleIdToken token = googleIdToken("google-sub-123", "user@example.com", "홍길동");
        when(delegate.verify("valid-token")).thenReturn(token);

        OAuthInfo info = sut.verify("valid-token");

        assertThat(info.providerId()).isEqualTo("google-sub-123");
        assertThat(info.email()).isEqualTo("user@example.com");
        assertThat(info.name()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("verify: 검증 실패로 verifier가 null을 반환하면 BadCredentialsException을 던진다")
    void verify_throwsWhenVerifierReturnsNull() throws Exception {
        when(delegate.verify("invalid-token")).thenReturn(null);

        assertThatThrownBy(() -> sut.verify("invalid-token"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("verify: verifier가 예외를 던지면 BadCredentialsException으로 감싼다")
    void verify_wrapsVerifierException() throws Exception {
        when(delegate.verify(anyString())).thenThrow(new IOException("network failure"));

        assertThatThrownBy(() -> sut.verify("any-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Verifying ID Token Failed");
    }

    private GoogleIdToken googleIdToken(String sub, String email, String name) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject(sub);
        payload.setEmail(email);
        payload.set("name", name);

        JsonWebSignature.Header header = new JsonWebSignature.Header().setAlgorithm("RS256");
        return new GoogleIdToken(header, payload, new byte[0], new byte[0]);
    }
}
