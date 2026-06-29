package kko.traveldiary_login.member.adaptor.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.Member;
import kko.traveldiary_login.member.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberEntityTest {

    @Test
    @DisplayName("Member 도메인의 모든 필드가 Entity로 매핑된다")
    void from_mapsAllFields() {
        Member member = Member.reconstitute(
                1L, AuthProvider.GOOGLE, "google-sub-123", "user@example.com", "홍길동", Role.USER);

        MemberEntity entity = MemberEntity.from(member);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(entity.getProviderId()).isEqualTo("google-sub-123");
        assertThat(entity.getEmail()).isEqualTo("user@example.com");
        assertThat(entity.getName()).isEqualTo("홍길동");
        assertThat(entity.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("from(Member): id가 없는(미저장) 도메인도 변환된다")
    void from_mapsNullId() {
        Member member = Member.register(
                AuthProvider.GOOGLE, "google-sub-456", "new@example.com", "신규유저", Role.USER);

        MemberEntity entity = MemberEntity.from(member);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getProviderId()).isEqualTo("google-sub-456");
        assertThat(entity.getEmail()).isEqualTo("new@example.com");
        assertThat(entity.getName()).isEqualTo("신규유저");
        assertThat(entity.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("toDomain(): 엔티티의 모든 필드가 도메인으로 매핑된다")
    void toDomain_mapsAllFields() {
        MemberEntity entity = new MemberEntity(
                2L, AuthProvider.GOOGLE, "google-sub-789", "admin@example.com", "관리자", Role.ADMIN);

        Member member = entity.toDomain();

        assertThat(member.getId()).isEqualTo(2L);
        assertThat(member.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(member.getProviderId()).isEqualTo("google-sub-789");
        assertThat(member.getEmail()).isEqualTo("admin@example.com");
        assertThat(member.getName()).isEqualTo("관리자");
        assertThat(member.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("round-trip: Member -> Entity -> Member 변환 후에도 모든 필드가 보존된다")
    void roundTrip_preservesAllFields() {
        Member original = Member.reconstitute(
                3L, AuthProvider.GOOGLE, "google-sub-999", "round@example.com", "왕복", Role.USER);

        Member result = MemberEntity.from(original).toDomain();

        assertThat(result.getId()).isEqualTo(original.getId());
        assertThat(result.getAuthProvider()).isEqualTo(original.getAuthProvider());
        assertThat(result.getProviderId()).isEqualTo(original.getProviderId());
        assertThat(result.getEmail()).isEqualTo(original.getEmail());
        assertThat(result.getName()).isEqualTo(original.getName());
        assertThat(result.getRole()).isEqualTo(original.getRole());
    }
}
