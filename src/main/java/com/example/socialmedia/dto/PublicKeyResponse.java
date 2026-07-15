package com.example.socialmedia.dto;

public class PublicKeyResponse {
    private String publicKey;

    public PublicKeyResponse() {
    }

    public PublicKeyResponse(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
