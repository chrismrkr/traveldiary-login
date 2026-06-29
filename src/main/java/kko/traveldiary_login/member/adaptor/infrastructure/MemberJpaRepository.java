package kko.traveldiary_login.member.adaptor.infrastructure;

import kko.traveldiary_login.member.domain.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface MemberJpaRepository extends JpaRepository<MemberEntity, Long> {
    Optional<MemberEntity> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
