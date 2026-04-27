package ru.tms.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT от auth-service кладёт список ролей в claim {@code roles} (см. {@code JwtUtil}).
 * Стандартный OAuth2-конвертер ищет {@code scope}/{@code authorities}, поэтому явно маппим {@code roles}.
 */
@Configuration
public class JwtRolesConverterConfig {

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> new JwtAuthenticationToken(jwt, extractRoles(jwt));
    }

    @SuppressWarnings("unchecked")
    private static Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        Object raw = jwt.getClaim("roles");
        if (!(raw instanceof Collection<?> coll) || coll.isEmpty()) {
            return Collections.emptyList();
        }
        List<GrantedAuthority> out = new ArrayList<>();
        for (Object item : coll) {
            if (item == null) {
                continue;
            }
            String r = item.toString().trim();
            if (r.isEmpty()) {
                continue;
            }
            if (!r.startsWith("ROLE_")) {
                r = "ROLE_" + r;
            }
            out.add(new SimpleGrantedAuthority(r));
        }
        return out.stream().distinct().collect(Collectors.toList());
    }
}
