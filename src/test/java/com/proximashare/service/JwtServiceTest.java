package com.proximashare.service;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import com.proximashare.ProximaShareApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import io.jsonwebtoken.MalformedJwtException;

@SpringBootTest(classes = ProximaShareApplication.class)
@TestPropertySource(properties = {
        "application.security.jwt.secret=testSecretKeyForJwtTokenGenerationAndValidationTesting123456",
        "application.security.jwt.expiration=3600000" // 1 hour
})
@DisplayName("JwtService Tests")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private UserDetails userDetails;
    private Map<String, Object> claims;

    @BeforeEach
    void setUp() {
        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .roles("USER")
                .build();

        claims = new HashMap<>();
        claims.put("roles", "USER");
        claims.put("customClaim", "customValue");
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid JWT token")
        void shouldGenerateValidToken() {
            // Act
            String token = jwtService.generateToken(claims, "testuser");

            // Assert
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            // Act
            String token1 = jwtService.generateToken(claims, "user1");
            String token2 = jwtService.generateToken(claims, "user2");

            // Assert
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Should generate token with empty claims")
        void shouldGenerateTokenWithEmptyClaims() {
            // Act
            String token = jwtService.generateToken(new HashMap<>(), "testuser");

            // Assert
            assertThat(token).isNotNull();
            assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should include custom claims in token")
        void shouldIncludeCustomClaimsInToken() {
            // Arrange
            Map<String, Object> customClaims = new HashMap<>();
            customClaims.put("userId", 123);
            customClaims.put("email", "test@example.com");

            // Act
            String token = jwtService.generateToken(customClaims, "testuser");

            // Assert
            assertThat(token).isNotNull();
            Integer userId = jwtService.extractClaim(token, claims -> claims.get("userId", Integer.class));
            String email = jwtService.extractClaim(token, claims -> claims.get("email", String.class));

            assertThat(userId).isEqualTo(123);
            assertThat(email).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("Username Extraction Tests")
    class UsernameExtractionTests {

        @Test
        @DisplayName("Should extract username from token")
        void shouldExtractUsernameFromToken() {
            // Arrange
            String token = jwtService.generateToken(claims, "testuser");

            // Act
            String extractedUsername = jwtService.extractUsername(token);

            // Assert
            assertThat(extractedUsername).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should extract username with special characters")
        void shouldExtractUsernameWithSpecialCharacters() {
            // Arrange
            String username = "user_123-test@domain";
            String token = jwtService.generateToken(claims, username);

            // Act
            String extractedUsername = jwtService.extractUsername(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(username);
        }

        @Test
        @DisplayName("Should throw exception for malformed token")
        void shouldThrowExceptionForMalformedToken() {
            // Arrange
            String malformedToken = "not.a.valid.jwt.token";

            // Act & Assert
            assertThatThrownBy(() -> jwtService.extractUsername(malformedToken))
                    .isInstanceOf(MalformedJwtException.class);
        }

        @Test
        @DisplayName("Should throw exception for empty token")
        void shouldThrowExceptionForEmptyToken() {
            // Act & Assert
            assertThatThrownBy(() -> jwtService.extractUsername(""))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Claim Extraction Tests")
    class ClaimExtractionTests {

        @Test
        @DisplayName("Should extract custom claim from token")
        void shouldExtractCustomClaim() {
            // Arrange
            String token = jwtService.generateToken(claims, "testuser");

            // Act
            String customClaim = jwtService.extractClaim(token, c -> c.get("customClaim", String.class));

            // Assert
            assertThat(customClaim).isEqualTo("customValue");
        }

        @Test
        @DisplayName("Should extract subject claim")
        void shouldExtractSubjectClaim() {
            // Arrange
            String token = jwtService.generateToken(claims, "testuser");

            // Act
            String subject = jwtService.extractClaim(token, io.jsonwebtoken.Claims::getSubject);

            // Assert
            assertThat(subject).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should extract issued at date")
        void shouldExtractIssuedAtDate() {
            // Arrange
            String token = jwtService.generateToken(claims, "testuser");

            // Act
            java.util.Date issuedAt = jwtService.extractClaim(token, io.jsonwebtoken.Claims::getIssuedAt);

            // Assert
            assertThat(issuedAt).isNotNull();
            assertThat(issuedAt).isBeforeOrEqualTo(new java.util.Date());
        }

        @Test
        @DisplayName("Should extract expiration date")
        void shouldExtractExpirationDate() {
            // Arrange
            String token = jwtService.generateToken(claims, "testuser");

            // Act
            java.util.Date expiration = jwtService.extractClaim(token, io.jsonwebtoken.Claims::getExpiration);

            // Assert
            assertThat(expiration).isNotNull();
            assertThat(expiration).isAfter(new java.util.Date());
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate correct token")
        void shouldValidateCorrectToken() {
            // Arrange
            String token = jwtService.generateToken(claims, "testuser");

            // Act
            boolean isValid = jwtService.isTokenValid(token, userDetails);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject token with wrong username")
        void shouldRejectTokenWithWrongUsername() {
            // Arrange
            String token = jwtService.generateToken(claims, "wronguser");

            // Act
            boolean isValid = jwtService.isTokenValid(token, userDetails);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject token for different user")
        void shouldRejectTokenForDifferentUser() {
            // Arrange
            String token = jwtService.generateToken(claims, "user1");
            UserDetails differentUser = User.builder()
                    .username("user2")
                    .password("password")
                    .roles("USER")
                    .build();

            // Act
            boolean isValid = jwtService.isTokenValid(token, differentUser);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should validate token case-sensitively")
        void shouldValidateTokenCaseSensitively() {
            // Arrange
            String token = jwtService.generateToken(claims, "TestUser");
            UserDetails lowerCaseUser = User.builder()
                    .username("testuser")
                    .password("password")
                    .roles("USER")
                    .build();

            // Act
            boolean isValid = jwtService.isTokenValid(token, lowerCaseUser);

            // Assert
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Expiration Tests")
    class TokenExpirationTests {

        @Test
        @DisplayName("Should create token with future expiration")
        void shouldCreateTokenWithFutureExpiration() {
            // Arrange
            String token = jwtService.generateToken(claims, "testuser");

            // Act
            java.util.Date expiration = jwtService.extractClaim(token, io.jsonwebtoken.Claims::getExpiration);
            java.util.Date now = new java.util.Date();

            // Assert
            assertThat(expiration).isAfter(now);

            // Should expire approximately 1 hour from now (allowing 1 second tolerance)
            long timeDiff = expiration.getTime() - now.getTime();
            assertThat(timeDiff).isGreaterThan(3599000L); // At least 59 minutes 59 seconds
            assertThat(timeDiff).isLessThan(3601000L);    // At most 60 minutes 1 second
        }

        @Test
        @DisplayName("Should detect non-expired token")
        void shouldDetectNonExpiredToken() {
            // Arrange
            String token = jwtService.generateToken(claims, "testuser");

            // Act
            boolean isValid = jwtService.isTokenValid(token, userDetails);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should handle token at boundary of expiration")
        void shouldHandleTokenAtBoundaryOfExpiration() throws InterruptedException {
            // This test verifies the token is valid immediately after creation
            String token = jwtService.generateToken(claims, "testuser");

            // Small delay to ensure time has passed
            Thread.sleep(100);

            // Token should still be valid
            boolean isValid = jwtService.isTokenValid(token, userDetails);
            assertThat(isValid).isTrue();
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should use consistent signing for same input")
        void shouldUseConsistentSigningForSameInput() {
            // Note: JWT includes timestamp, so tokens will always be different
            // This test verifies both tokens are valid for the same user

            String token1 = jwtService.generateToken(claims, "testuser");
            String token2 = jwtService.generateToken(claims, "testuser");

            assertThat(jwtService.isTokenValid(token1, userDetails)).isTrue();
            assertThat(jwtService.isTokenValid(token2, userDetails)).isTrue();
        }

        @Test
        @DisplayName("Should reject tampered token")
        void shouldRejectTamperedToken() {
            // Arrange
            String validToken = jwtService.generateToken(claims, "testuser");
            String tamperedToken = validToken.substring(0, validToken.length() - 5) + "12345";

            // Act & Assert
            assertThatThrownBy(() -> jwtService.extractUsername(tamperedToken))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should handle null token gracefully")
        void shouldHandleNullTokenGracefully() {
            // Act & Assert
            assertThatThrownBy(() -> jwtService.extractUsername(null))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long username")
        void shouldHandleVeryLongUsername() {
            // Arrange
            String longUsername = "a".repeat(100);
            String token = jwtService.generateToken(claims, longUsername);

            // Act
            String extractedUsername = jwtService.extractUsername(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(longUsername);
        }

        @Test
        @DisplayName("Should handle username with unicode characters")
        void shouldHandleUsernameWithUnicodeCharacters() {
            // Arrange
            String unicodeUsername = "user测试🎉";
            String token = jwtService.generateToken(claims, unicodeUsername);

            // Act
            String extractedUsername = jwtService.extractUsername(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(unicodeUsername);
        }

        @Test
        @DisplayName("Should handle large claims map")
        void shouldHandleLargeClaimsMap() {
            // Arrange
            Map<String, Object> largeClaims = new HashMap<>();
            for (int i = 0; i < 50; i++) {
                largeClaims.put("claim" + i, "value" + i);
            }

            // Act
            String token = jwtService.generateToken(largeClaims, "testuser");

            // Assert
            assertThat(token).isNotNull();
            assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
        }
    }
}