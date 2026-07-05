package kko.traveldiary_login.member.adaptor.infrastructure;

import kko.traveldiary_login.member.application.required.MemberRepository;
import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberDatabaseRepository implements MemberRepository {
    private final MemberJpaRepository jpaRepository;
    @Override
    public Optional<Member> findByProviderAndProviderId(AuthProvider authProvider, String providerId) {
        return jpaRepository.findByProviderAndProviderId(authProvider, providerId)
                .map(MemberEntity::toDomain);
    }

    @Override
    public Optional<Member> findById(Long id) {
        return jpaRepository.findById(id)
                .map(MemberEntity::toDomain);
    }

    @Override
    public Member save(Member member) {
        MemberEntity saved = jpaRepository.save(MemberEntity.from(member));
        return saved.toDomain();
    }

    @Override
    public void delete(Long memberId) {
        jpaRepository.deleteById(memberId);
    }
}
