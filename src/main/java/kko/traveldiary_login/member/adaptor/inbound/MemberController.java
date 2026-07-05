package kko.traveldiary_login.member.adaptor.inbound;

import kko.traveldiary_login.member.adaptor.inbound.dto.*;
import kko.traveldiary_login.member.application.provided.MemberService;
import kko.traveldiary_login.member.application.provided.MobileSDKOAuthManager;
import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.Member;
import kko.traveldiary_login.member.domain.TokenPair;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final MobileSDKOAuthManager mobileSDKOAuthManager;

    @PostMapping("/auth/login")
    public ResponseEntity<TokenResponse> login(@RequestBody MemberLoginRequest loginRequest) {
        String authProvider = loginRequest.provider();
        TokenPair tokenPair = mobileSDKOAuthManager.login(AuthProvider.valueOf(authProvider), loginRequest.idToken());
        return ResponseEntity.ok(
                new TokenResponse(tokenPair.accessToken(), tokenPair.refreshToken(), "Bearer")
        );
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody TokenRefreshRequest refreshRequest) {
        TokenPair refreshed = memberService.refresh(refreshRequest.refreshToken());
        return ResponseEntity.ok(
                new TokenResponse(refreshed.accessToken(), refreshed.refreshToken(), "Bearer")
        );
    }

    @GetMapping("/auth/me")
    public ResponseEntity<MemberDetailsResponse> me(@AuthenticationPrincipal Jwt jwt) {
        Long memberId = Long.valueOf(jwt.getSubject());
        Member me = memberService.me(memberId);
        return ResponseEntity.ok(
                new MemberDetailsResponse(me.getName(), me.getEmail())
        );
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt, @RequestBody MemberLogoutRequest logoutRequest) {
        memberService.logout(logoutRequest.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Jwt jwt) {
        Long memberId = Long.valueOf(jwt.getSubject());
        memberService.withdraw(memberId);
        return ResponseEntity.noContent().build();
    }

}
