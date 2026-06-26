package kko.traveldiary_login.member.application;

import kko.traveldiary_login.member.domain.OAuthInfo;

interface OAuthVerifier {
    OAuthInfo verify(String tokenId);
}
