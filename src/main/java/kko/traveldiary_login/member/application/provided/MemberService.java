package kko.traveldiary_login.member.application.provided;

import kko.traveldiary_login.member.domain.Member;
import kko.traveldiary_login.member.domain.TokenPair;

public interface MemberService {
    // 토큰 갱신 (refresh)
    TokenPair refresh(String refreshToken);

    // 로그아웃 (logout)
    void logout(String refreshToken);
    // 내정보조회 (me)
    Member me(Long memberId);
    // 회원탈퇴 (withdraw)
    void withdraw(Long memberId);
}
