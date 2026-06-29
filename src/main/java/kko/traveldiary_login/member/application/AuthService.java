package kko.traveldiary_login.member.application;

import kko.traveldiary_login.member.application.provided.MobileSDKOAuthManager;
import kko.traveldiary_login.member.application.required.MemberRepository;
import kko.traveldiary_login.member.application.required.OAuthVerifier;
import kko.traveldiary_login.member.application.required.TokenIssuer;
import kko.traveldiary_login.member.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService implements MobileSDKOAuthManager {
    private final Map<AuthProvider, OAuthVerifier> verifiers;
    private final MemberRepository memberRepository;
    private final TokenIssuer tokenIssuer;

    @Override
    @Transactional
    public TokenPair login(AuthProvider authProvider, String idToken) {
        // TODO
        OAuthInfo oAuthInfo = verifiers.get(authProvider)
                .verify(idToken);

        Member m = memberRepository.findByProviderAndProviderId(authProvider, oAuthInfo.providerId())
                .map(member -> {
                    member.updateEmail(oAuthInfo.email());
                    member.updateName(oAuthInfo.name());
                    return member;
                })
                .orElseGet(() -> {
                    Member newMember = Member.register(authProvider, oAuthInfo.providerId(), oAuthInfo.email(), oAuthInfo.name(), Role.USER);
                    newMember = memberRepository.save(newMember);
                    return newMember;
                });

        return tokenIssuer.issue(m);
    }
}
