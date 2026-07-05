package kko.traveldiary_login.member.adaptor.inbound.dto;

public record MemberLoginRequest(
        String provider,
        String idToken
) { }
