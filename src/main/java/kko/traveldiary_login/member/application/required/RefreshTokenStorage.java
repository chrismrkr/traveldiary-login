package kko.traveldiary_login.member.application.required;

public interface RefreshTokenStorage {
    void save(Long memberId, String jti, String refreshToken);
    String getAndDelete(Long memberId, String jti);
    boolean isValid(Long memberId, String jti, String refreshToken);
    void delete(Long memberId, String jti);// 로그아웃 시
}

