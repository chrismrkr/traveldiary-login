package kko.traveldiary_login.member.domain;

import java.time.Duration;

public record TokenPair(String accessToken, String refreshToken, String jti) { }
