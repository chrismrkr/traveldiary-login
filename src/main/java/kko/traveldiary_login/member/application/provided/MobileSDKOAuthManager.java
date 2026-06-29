package kko.traveldiary_login.member.application.provided;

import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.TokenPair;

public interface MobileSDKOAuthManager {
    TokenPair login(AuthProvider authProvider, String idToken);
}
