package kko.traveldiary_login.member.adaptor.infrastructure;

import jakarta.persistence.*;
import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.Member;
import kko.traveldiary_login.member.domain.Role;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_provider",
                columnNames = {"provider", "providerId"}))   // (provider, providerId) 복합 unique
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId;
    private String email;
    private String name;

    @Enumerated(EnumType.STRING)
    private Role role;

    public MemberEntity(Long id, AuthProvider provider, String providerId, String email, String name, Role role) {
        this.id = id;
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    public static MemberEntity from(Member m) {
        return new MemberEntity(
                m.getId(),
                m.getAuthProvider(),
                m.getProviderId(),
                m.getEmail(),
                m.getName(),
                m.getRole()
        );
    }
    public Member toDomain() {
        return Member.reconstitute(id, provider, providerId, email, name, role);
    }
}
