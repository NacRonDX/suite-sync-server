package com.nacrondx.suitesync.service;

import com.nacrondx.suitesync.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final JwtEncoder jwtEncoder;

  @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
  private long jwtExpiration;

  public JwtService(JwtEncoder jwtEncoder) {
    this.jwtEncoder = jwtEncoder;
  }

  public String generateToken(User user) {
    var now = Instant.now();
    long expirationSeconds = jwtExpiration / 1000;

    var claims =
        JwtClaimsSet.builder()
            .issuer("suite-sync")
            .issuedAt(now)
            .expiresAt(now.plus(expirationSeconds, ChronoUnit.SECONDS))
            .subject(user.getEmail())
            .claim("userId", user.getId())
            .claim("userType", user.getUserType().name())
            .claim("firstName", user.getFirstName())
            .claim("lastName", user.getLastName())
            .build();

    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  public String generateRefreshToken(User user) {
    var now = Instant.now();
    long refreshExpirationSeconds = (jwtExpiration * 7) / 1000; // 7 days

    var claims =
        JwtClaimsSet.builder()
            .issuer("suite-sync")
            .issuedAt(now)
            .expiresAt(now.plus(refreshExpirationSeconds, ChronoUnit.SECONDS))
            .subject(user.getEmail())
            .claim("userId", user.getId())
            .claim("type", "refresh")
            .build();

    return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  public long getExpirationSeconds() {
    return jwtExpiration / 1000;
  }
}
