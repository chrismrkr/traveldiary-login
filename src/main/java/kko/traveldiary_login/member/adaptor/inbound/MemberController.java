package kko.traveldiary_login.member.adaptor.inbound;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kko.traveldiary_login.config.OpenApiConfig;
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
@Tag(name = "Auth", description = "소셜 로그인 · JWT 발급 · 회원 API")
public class MemberController {
    private final MemberService memberService;
    private final MobileSDKOAuthManager mobileSDKOAuthManager;

    @Operation(summary = "소셜 로그인",
            description = "provider(예: GOOGLE)의 ID token을 검증하고 서비스 JWT(access/refresh)를 발급한다. 인증 불필요.")
    @PostMapping("/api/auth/login")
    public ResponseEntity<TokenResponse> login(@RequestBody MemberLoginRequest loginRequest) {
        String authProvider = loginRequest.provider();
        TokenPair tokenPair = mobileSDKOAuthManager.login(AuthProvider.valueOf(authProvider), loginRequest.idToken());
        return ResponseEntity.ok(
                new TokenResponse(tokenPair.accessToken(), tokenPair.refreshToken(), "Bearer")
        );
    }

    @Operation(summary = "토큰 재발급",
            description = "refresh token으로 새 access/refresh 토큰을 발급(회전)한다. 이전 refresh token은 무효화된다. 인증 불필요.")
    @PostMapping("/api/auth/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody TokenRefreshRequest refreshRequest) {
        TokenPair refreshed = memberService.refresh(refreshRequest.refreshToken());
        return ResponseEntity.ok(
                new TokenResponse(refreshed.accessToken(), refreshed.refreshToken(), "Bearer")
        );
    }

    @Operation(summary = "내 정보 조회", description = "access token의 회원 정보를 조회한다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
    @GetMapping("/api/auth/me")
    public ResponseEntity<MemberDetailsResponse> me(@AuthenticationPrincipal Jwt jwt) {
        Long memberId = Long.valueOf(jwt.getSubject());
        Member me = memberService.me(memberId);
        return ResponseEntity.ok(
                new MemberDetailsResponse(me.getName(), me.getEmail())
        );
    }

    @Operation(summary = "로그아웃", description = "해당 refresh token(기기) 세션을 무효화한다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt, @RequestBody MemberLogoutRequest logoutRequest) {
        memberService.logout(logoutRequest.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 탈퇴", description = "회원을 삭제한다.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
    @PostMapping("/api/auth/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Jwt jwt) {
        Long memberId = Long.valueOf(jwt.getSubject());
        memberService.withdraw(memberId);
        return ResponseEntity.noContent().build();
    }

}
