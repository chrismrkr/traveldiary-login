package kko.traveldiary_login.member.application.provided;

import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.OAuthInfo;

public interface OAuthVerifyManager {
    OAuthInfo verify(AuthProvider authProvider, String idToken);
}
