package kko.traveldiary_login.member.adaptor.infrastructure;

import kko.traveldiary_login.member.adaptor.oauth.config.JwtProperties;
import kko.traveldiary_login.member.application.required.RefreshTokenStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStorage implements RefreshTokenStorage {

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    @Override
    public void save(Long memberId, String jti, String refreshToken) {
        // refresh token TTL을 Redis 키 만료시간으로 지정 → 만료 시 자동 삭제
        redisTemplate.opsForValue()
                .set(key(memberId, jti), refreshToken, jwtProperties.refreshTokenTtl());
    }

    @Override
    public String getAndDelete(Long memberId, String jti) {
        return redisTemplate.opsForValue().getAndDelete(key(memberId, jti));
    }

    @Override
    public boolean isValid(Long memberId, String jti, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(key(memberId, jti));
        return stored != null && stored.equals(refreshToken);
    }

    @Override
    public void delete(Long memberId, String jti) {
        redisTemplate.delete(key(memberId, jti));
    }

    private String key(Long memberId, String jti) {
        return "refresh:" + memberId + ":" + jti;
    }
}
