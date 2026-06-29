package kko.traveldiary_login.member.adaptor.oauth;

import kko.traveldiary_login.member.application.required.OAuthVerifier;
import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.OAuthInfo;
import org.springframework.stereotype.Component;

@Component // TODO
public class GoogleOAuthVerifier implements OAuthVerifier {
    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public OAuthInfo verify(String tokenId) {
        return null;
    }
}
