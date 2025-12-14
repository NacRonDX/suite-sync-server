package com.nacrondx.suitesync.config;

import java.util.Collection;
import java.util.Collections;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
    var authorities = extractAuthorities(jwt);
    return new JwtAuthenticationToken(jwt, authorities);
  }

  private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    var userType = jwt.getClaimAsString("userType");
    if (userType != null) {
      return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userType));
    }
    return Collections.emptyList();
  }
}
