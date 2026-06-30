package kko.traveldiary_login.member.application.required;

import java.time.Instant;

public interface RefreshTokenStorage {
    void save(Long memberId, String refreshToken);
    boolean isValid(Long memberId, String refreshToken);
    void delete(Long memberId);   // 로그아웃 시
}
