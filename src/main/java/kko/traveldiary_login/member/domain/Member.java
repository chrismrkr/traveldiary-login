package kko.traveldiary_login.member.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    private Long id;            // 우리 서비스 내부 식별자 (PK)
    private AuthProvider authProvider;
    private String providerId;   // 고유 ID (인증 매핑용)
    private String email;
    private String name;
    private Role role;

    public static Member register(AuthProvider provider, String providerId, String email, String name) {
        Member member = new Member();
        member.authProvider = provider;
        member.providerId = providerId;
        member.email = email;
        member.name = name;
        member.role = Role.USER;   // 기본 역할
        return member;
    }
}
