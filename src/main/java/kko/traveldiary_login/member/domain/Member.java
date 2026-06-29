package kko.traveldiary_login.member.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    private Long id;            // 내부 식별자 (PK)
    private AuthProvider authProvider;
    private String providerId;   // OAuth Provider 고유 ID (인증 매핑용)
    private String email;
    private String name;
    private Role role;

    public static Member register(AuthProvider provider, String providerId, String email, String name, Role role) {
        Member member = new Member();
        member.authProvider = provider;
        member.providerId = providerId;
        member.email = email;
        member.name = name;
        member.role = role;   // 기본 역할
        return member;
    }

    public static Member reconstitute(Long id, AuthProvider provider, String providerId,
                                      String email, String name, Role role) {
        Member member = new Member();
        member.id = id;
        member.authProvider = provider;
        member.providerId = providerId;
        member.email = email;
        member.name = name;
        member.role = role;
        return member;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updateName(String name) {
        this.name = name;
    }
}
