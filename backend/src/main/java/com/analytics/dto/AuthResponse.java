package com.analytics.dto;

public class AuthResponse {
    private String token;
    private String username;
    private String email;
    private String tokenType = "Bearer";

    public AuthResponse(String token, String username, String email) {
        this.token = token;
        this.username = username;
        this.email = email;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getTokenType() { return tokenType; }
}
