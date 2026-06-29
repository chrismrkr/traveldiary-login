package kko.traveldiary_login.member.application.required;

import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.OAuthInfo;

public interface OAuthVerifier {
    AuthProvider provider();
    OAuthInfo verify(String tokenId);
}
