package kko.traveldiary_login.member.adaptor.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.Member;
import kko.traveldiary_login.member.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import(MemberDatabaseRepository.class)
class MemberDatabaseRepositoryTest {

    @Autowired
    private MemberDatabaseRepository repository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("save: 신규 회원을 저장하면 id가 채워지고 필드가 보존된다")
    void save_assignsIdAndPersistsFields() {
        Member newMember = Member.register(
                AuthProvider.GOOGLE, "google-sub-1", "user@example.com", "홍길동", Role.USER);

        Member saved = repository.save(newMember);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(saved.getProviderId()).isEqualTo("google-sub-1");
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getName()).isEqualTo("홍길동");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("findById: 저장된 회원을 id로 조회한다")
    void findById_returnsSavedMember() {
        Member saved = repository.save(Member.register(
                AuthProvider.GOOGLE, "google-sub-2", "find@example.com", "조회대상", Role.USER));

        Optional<Member> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getProviderId()).isEqualTo("google-sub-2");
        assertThat(found.get().getEmail()).isEqualTo("find@example.com");
    }

    @Test
    @DisplayName("findById: 존재하지 않는 id는 빈 Optional을 반환한다")
    void findById_returnsEmptyWhenNotFound() {
        Optional<Member> found = repository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByProviderAndProviderId: provider와 providerId로 회원을 조회한다")
    void findByProviderAndProviderId_returnsMatchingMember() {
        repository.save(Member.register(
                AuthProvider.GOOGLE, "google-sub-3", "match@example.com", "일치", Role.USER));

        Optional<Member> found =
                repository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-sub-3");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("match@example.com");
    }

    @Test
    @DisplayName("findByProviderAndProviderId: 일치하는 회원이 없으면 빈 Optional을 반환한다")
    void findByProviderAndProviderId_returnsEmptyWhenNotFound() {
        Optional<Member> found =
                repository.findByProviderAndProviderId(AuthProvider.GOOGLE, "no-such-id");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("(provider, providerId)가 중복되면 unique 제약 위반으로 저장이 실패한다")
    void save_violatesUniqueConstraintOnDuplicateProviderAndProviderId() {
        repository.save(Member.register(
                AuthProvider.GOOGLE, "dup-sub", "first@example.com", "첫번째", Role.USER));
        em.flush();

        assertThatThrownBy(() -> {
            repository.save(Member.register(
                    AuthProvider.GOOGLE, "dup-sub", "second@example.com", "두번째", Role.USER));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
