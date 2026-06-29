package kko.traveldiary_login.member.adaptor.oauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt.ttl")
public record JwtProperties(Duration accessTokenTtl, Duration refreshTokenTtl) { }
