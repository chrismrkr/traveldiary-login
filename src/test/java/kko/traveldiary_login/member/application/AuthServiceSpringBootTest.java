package kko.traveldiary_login.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import kko.traveldiary_login.member.adaptor.infrastructure.MemberJpaRepository;
import kko.traveldiary_login.member.application.provided.MemberService;
import kko.traveldiary_login.member.application.provided.MobileSDKOAuthManager;
import kko.traveldiary_login.member.application.required.MemberRepository;
import kko.traveldiary_login.member.application.required.OAuthVerifier;
import kko.traveldiary_login.member.application.required.RefreshTokenStorage;
import kko.traveldiary_login.member.domain.AuthProvider;
import kko.traveldiary_login.member.domain.Member;
import kko.traveldiary_login.member.domain.OAuthUserInfo;
import kko.traveldiary_login.member.domain.Role;
import kko.traveldiary_login.member.domain.TokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Testcontainers
class AuthServiceSpringBootTest {

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    /**
     * OAuthVerifier만 mock으로 교체한다. AuthService가 생성자에서 provider()로 map을 만들기 때문에
     * 컨텍스트 로드 시점에 provider()가 GOOGLE을 반환하도록, 실제 googleOAuthVerifier 빈을 오버라이드한다.
     */
    @TestConfiguration
    static class MockOAuthVerifierConfig {
        @Bean
        OAuthVerifier googleOAuthVerifier() {
            OAuthVerifier mock = mock(OAuthVerifier.class);
            when(mock.provider()).thenReturn(AuthProvider.GOOGLE);
            return mock;
        }
    }

    @Autowired
    private MobileSDKOAuthManager authService;
    @Autowired
    private MemberService memberService;   // 같은 AuthService 빈 (refresh 호출용)
    @Autowired
    private OAuthVerifier oAuthVerifier;   // 위에서 등록한 mock
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private MemberJpaRepository memberJpaRepository;
    @Autowired
    private RefreshTokenStorage refreshTokenStorage;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    EntityManager em;

    @BeforeEach
    void setUp() {
        memberJpaRepository.deleteAll();
        redisTemplate.getRequiredConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    @DisplayName("최초 로그인: Member 신규 생성 및 Access 및 Refresh 발급")
    void firstLogin() {
        when(oAuthVerifier.verify(anyString()))
                .thenReturn(new OAuthUserInfo("sub-new", "new@example.com", "새유저"));

        TokenPair tokenPair = authService.login(AuthProvider.GOOGLE, "google-id-token");

        // access/refresh/jti 모두 발급됨
        assertThat(tokenPair.accessToken()).isNotBlank();
        assertThat(tokenPair.refreshToken()).isNotBlank();
        assertThat(tokenPair.jti()).isNotBlank();

        // 회원이 신규로 저장됨
        assertThat(memberJpaRepository.count()).isEqualTo(1);
        Member saved = memberRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "sub-new").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getName()).isEqualTo("새유저");
        assertThat(saved.getRole()).isEqualTo(Role.USER);

        // refresh token이 redis에 저장되어 유효함
        assertThat(refreshTokenStorage.isValid(saved.getId(), tokenPair.jti(), tokenPair.refreshToken()))
                .isTrue();
    }

    @Test
    @DisplayName("다시 로그인: 기존 Member 업데이트 및 토큰 발급")
    void loginAgain() {
        Member existing = memberRepository.save(
                Member.register(AuthProvider.GOOGLE, "sub-1", "old@example.com", "옛이름", Role.USER));
        assertThat(memberJpaRepository.count()).isEqualTo(1);

        when(oAuthVerifier.verify(anyString()))
                .thenReturn(new OAuthUserInfo("sub-1", "changed@example.com", "새이름"));

        TokenPair tokenPair = authService.login(AuthProvider.GOOGLE, "google-id-token");

        // 새 회원을 만들지 않고 동일 회원(id 일치)으로 처리됨
        assertThat(memberJpaRepository.count()).isEqualTo(1);
        Member after = memberRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "sub-1").orElseThrow();
        assertThat(after.getId()).isEqualTo(existing.getId());
        assertThat(after.getName()).isEqualTo("새이름");
        assertThat(after.getEmail()).isEqualTo("changed@example.com");


        // 토큰 발급 & refresh 저장
        assertThat(tokenPair.accessToken()).isNotBlank();
        assertThat(refreshTokenStorage.isValid(existing.getId(), tokenPair.jti(), tokenPair.refreshToken()));
    }

    @Test
    @DisplayName("서로 다른 기기로 로그인 : 서로 다른 jti로 여러개의 토큰 발급")
    void loginVariousDevice() {
        when(oAuthVerifier.verify(anyString()))
                .thenReturn(new OAuthUserInfo("sub-1", "user@example.com", "유저"));

        // 같은 회원이 두 기기에서 각각 로그인
        TokenPair deviceA = authService.login(AuthProvider.GOOGLE, "id-token-from-A");
        TokenPair deviceB = authService.login(AuthProvider.GOOGLE, "id-token-from-B");

        Long memberId = memberRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "sub-1")
                .orElseThrow().getId();

        // 회원은 하나지만 로그인마다 서로 다른 jti가 발급됨
        assertThat(memberJpaRepository.count()).isEqualTo(1);
        assertThat(deviceA.jti()).isNotEqualTo(deviceB.jti());

        // 두 기기의 refresh token이 서로 덮어쓰지 않고 각각 독립적으로 유효함
        assertThat(refreshTokenStorage.isValid(memberId, deviceA.jti(), deviceA.refreshToken())).isTrue();
        assertThat(refreshTokenStorage.isValid(memberId, deviceB.jti(), deviceB.refreshToken())).isTrue();
    }

    @Test
    @DisplayName("동시에 같은 refresh token으로 refresh 하면 정확히 하나만 성공한다 (double-spend 방지)")
    void refresh_concurrentSameToken_onlyOneSucceeds() throws InterruptedException {
        when(oAuthVerifier.verify(anyString()))
                .thenReturn(new OAuthUserInfo("sub-1", "user@example.com", "유저"));
        TokenPair loggedIn = authService.login(AuthProvider.GOOGLE, "id-token");
        String refreshToken = loggedIn.refreshToken();

        int threadCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();   // 모두 동시에 같은 토큰으로 refresh 시도
                    memberService.refresh(refreshToken);
                    success.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    failure.incrementAndGet();   // BadCredentialsException 등
                }
            });
        }
        ready.await();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        // 동시 요청 중 정확히 하나만 성공, 나머지는 모두 거부
        assertThat(success.get()).isEqualTo(1);
        assertThat(failure.get()).isEqualTo(threadCount - 1);
    }
}
