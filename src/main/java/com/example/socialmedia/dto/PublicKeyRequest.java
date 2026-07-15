package com.example.socialmedia.dto;

public class PublicKeyRequest {
    private String publicKey;

    public PublicKeyRequest() {
    }

    public PublicKeyRequest(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
