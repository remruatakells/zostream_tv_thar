package com.buannel.studio.pvt.ltd.zostream.utils;

public final class AuthHeader {

    private static final String BEARER_PREFIX = "Bearer ";

    private AuthHeader() {}

    public static String bearer(String accessToken) {
        if (accessToken == null) {
            return null;
        }

        String token = accessToken.trim();
        if (token.isEmpty()) {
            return null;
        }

        if (token.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return BEARER_PREFIX + token.substring(BEARER_PREFIX.length()).trim();
        }

        return BEARER_PREFIX + token;
    }
}
