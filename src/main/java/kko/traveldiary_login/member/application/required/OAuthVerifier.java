package kko.traveldiary_login.member.application.required;

import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.OAuthUserInfo;

public interface OAuthVerifier {
    AuthProvider provider();
    OAuthUserInfo verify(String tokenId);
}
