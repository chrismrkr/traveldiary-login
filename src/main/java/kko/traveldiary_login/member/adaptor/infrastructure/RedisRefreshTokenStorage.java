package kko.traveldiary_login.member.adaptor.infrastructure;

import kko.traveldiary_login.member.adaptor.oauth.config.JwtProperties;
import kko.traveldiary_login.member.application.required.RefreshTokenStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStorage implements RefreshTokenStorage {
    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    @Override
    public void save(Long memberId, String refreshToken) {
        // refresh token TTL을 Redis 키 만료시간으로 지정 → 만료 시 자동 삭제
        redisTemplate.opsForValue()
                .set(key(memberId), refreshToken, jwtProperties.refreshTokenTtl());
    }

    @Override
    public boolean isValid(Long memberId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(key(memberId));
        return stored != null && stored.equals(refreshToken);
    }

    @Override
    public void delete(Long memberId) {
        redisTemplate.delete(key(memberId));
    }

    private String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }
}
