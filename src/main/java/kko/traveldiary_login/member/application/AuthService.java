package kko.traveldiary_login.member.application;

import kko.traveldiary_login.member.application.provided.MemberService;
import kko.traveldiary_login.member.application.provided.MobileSDKOAuthManager;
import kko.traveldiary_login.member.application.required.*;
import kko.traveldiary_login.member.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService implements MobileSDKOAuthManager, MemberService {
    private final Map<AuthProvider, OAuthVerifier> verifiers;
    private final MemberRepository memberRepository;
    private final RefreshTokenStorage refreshTokenStorage;
    private final TokenIssuer tokenIssuer;
    private final TokenParser tokenParser;


    @Autowired
    public AuthService(List<OAuthVerifier> verifierList,
                       MemberRepository memberRepository, RefreshTokenStorage refreshTokenStorage,
                       TokenIssuer tokenIssuer, TokenParser tokenParser) {
        this.verifiers = verifierList.stream()
                .collect(Collectors.toMap(OAuthVerifier::provider, oAuthVerifier -> oAuthVerifier));
        this.memberRepository = memberRepository;
        this.refreshTokenStorage = refreshTokenStorage;
        this.tokenIssuer = tokenIssuer;
        this.tokenParser = tokenParser;
    }

    @Override
    @Transactional
    public TokenPair login(AuthProvider authProvider, String idToken) {
        OAuthUserInfo oAuthUserInfo = verifiers.get(authProvider).verify(idToken);

        Member member = memberRepository.findByProviderAndProviderId(authProvider, oAuthUserInfo.providerId())
                .map(m -> {
                    m.updateByOAuthInfo(oAuthUserInfo);
                    return m;
                })
                .orElseGet(() -> Member.register(authProvider, oAuthUserInfo.providerId(), oAuthUserInfo.email(), oAuthUserInfo.name(), Role.USER));

        member = memberRepository.save(member);
        TokenPair tokenPair = tokenIssuer.issue(member);
        refreshTokenStorage.save(member.getId(), tokenPair.jti(), tokenPair.refreshToken());
        return tokenPair;
    }

    @Override
    @Transactional
    public TokenPair refresh(String refreshToken) {
        TokenClaims tokenClaims = tokenParser.parseRefreshToken(refreshToken);

        String storedRefreshToken = refreshTokenStorage.getAndDelete(tokenClaims.memberId(), tokenClaims.jti());
        if(storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new BadCredentialsException("Invalid Refresh Token: expired");
        }

        Member member = memberRepository.findById(tokenClaims.memberId()).orElseThrow();
        TokenPair newToken = tokenIssuer.issue(member);

        refreshTokenStorage.save(member.getId(), newToken.jti(), newToken.refreshToken());
        return newToken;
    }

    @Override
    public void logout(String refreshToken) {
        TokenClaims tokenClaims = tokenParser.parseRefreshToken(refreshToken);
        refreshTokenStorage.delete(tokenClaims.memberId(), tokenClaims.jti());
    }

    @Override
    public Member me(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow();
    }

    @Override
    public void withdraw(Long memberId) {
        memberRepository.delete(memberId);
        // TODO (memberId - jti) 로 저장된 RefreshToken을 refreshTokenStorage에서 모두 삭제하도록 추가 구현 필요
    }
}
