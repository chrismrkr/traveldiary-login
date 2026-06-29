package kko.traveldiary_login.member.application.required;

import kko.traveldiary_login.member.domain.Member;
import kko.traveldiary_login.member.domain.TokenPair;

import java.time.Duration;

public interface TokenIssuer {
    TokenPair issue(Member member);
    String reissueAccessToken(Member member);
}
