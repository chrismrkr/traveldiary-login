package kko.traveldiary_login.member.application.required;

import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.Member;

import java.util.Optional;

public interface MemberRepository {
    Optional<Member> findByProviderAndProviderId(AuthProvider authProvider, String providerId);
    Optional<Member> findById(Long id);
    Member save(Member member);
    void delete(Long memberId);
}
