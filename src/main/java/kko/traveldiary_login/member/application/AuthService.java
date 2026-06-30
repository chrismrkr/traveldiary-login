package kko.traveldiary_login.member.application;

import kko.traveldiary_login.member.application.provided.MobileSDKOAuthManager;
import kko.traveldiary_login.member.application.required.MemberRepository;
import kko.traveldiary_login.member.application.required.OAuthVerifier;
import kko.traveldiary_login.member.application.required.RefreshTokenStorage;
import kko.traveldiary_login.member.application.required.TokenIssuer;
import kko.traveldiary_login.member.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService implements MobileSDKOAuthManager {
    private final Map<AuthProvider, OAuthVerifier> verifiers;
    private final MemberRepository memberRepository;
    private final RefreshTokenStorage refreshTokenStorage;
    private final TokenIssuer tokenIssuer;


    @Autowired
    public AuthService(List<OAuthVerifier> verifierList,
                       MemberRepository memberRepository, RefreshTokenStorage refreshTokenStorage, TokenIssuer tokenIssuer) {
        this.verifiers = verifierList.stream()
                .collect(Collectors.toMap(OAuthVerifier::provider, oAuthVerifier -> oAuthVerifier));
        this.memberRepository = memberRepository;
        this.refreshTokenStorage = refreshTokenStorage;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    @Transactional
    public TokenPair login(AuthProvider authProvider, String idToken) {
        OAuthInfo oAuthInfo = verifiers.get(authProvider).verify(idToken);

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

        TokenPair tokenPair = tokenIssuer.issue(m);
        refreshTokenStorage.save(m.getId(), tokenPair.refreshToken());
        return tokenPair;
    }
}
