package kko.traveldiary_login.member.adaptor.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import kko.traveldiary_login.member.application.required.OAuthVerifier;
import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.OAuthInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleOAuthVerifier implements OAuthVerifier {
    private final GoogleIdTokenVerifier verifier;

    @Autowired
    public GoogleOAuthVerifier(@Value("${app.oauth.google.client-id}") String clientId) throws GeneralSecurityException, IOException {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }


    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public OAuthInfo verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                // 서명 불일치, 만료, audience 불일치 등 → 검증 실패 시 null 반환
                throw new BadCredentialsException("Invalid Google Token");
            }
            GoogleIdToken.Payload payload = token.getPayload();

            String providerId = payload.getSubject();        // 구글 sub
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            return new OAuthInfo(providerId, email, name);
        } catch (Exception e) {
            throw new BadCredentialsException("Verifying ID Token Failed", e);
        }
    }
}
