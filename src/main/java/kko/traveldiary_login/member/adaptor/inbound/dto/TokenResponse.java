package kko.traveldiary_login.member.adaptor.inbound.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {}
