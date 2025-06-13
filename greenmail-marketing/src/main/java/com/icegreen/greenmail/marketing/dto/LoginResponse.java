package com.icegreen.greenmail.marketing.dto;

public class LoginResponse {
    private String token;
    private MemberDto member;

    // Constructors, Getters, Setters
    public LoginResponse() {
    }

    public LoginResponse(String token, MemberDto member) {
        this.token = token;
        this.member = member;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public MemberDto getMember() {
        return member;
    }

    public void setMember(MemberDto member) {
        this.member = member;
    }
}
