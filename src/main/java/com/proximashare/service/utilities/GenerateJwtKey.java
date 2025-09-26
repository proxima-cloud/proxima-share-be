package com.proximashare.service.utilities;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;

public class GenerateJwtKey {
    public static void main(String[] args) {
        // Generate a secure 256-bit key for HS256
        SecretKey key = Jwts.SIG.HS256.key().build();
        String base64Key = java.util.Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("Generated JWT Key (add to application.properties):");
        System.out.println("jwt.secret=" + base64Key);
    }
}