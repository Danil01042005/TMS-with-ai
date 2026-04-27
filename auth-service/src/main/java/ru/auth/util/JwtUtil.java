package ru.auth.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ru.auth.config.RsaKeyProvider;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

	private final RsaKeyProvider rsaKeyProvider;
	private final Duration jwtExpiration;

	public JwtUtil(RsaKeyProvider rsaKeyProvider, 
	               @Value("${auth.jwt.expires:PT10M}") String jwtExpirationStr) {
		this.rsaKeyProvider = rsaKeyProvider;
		this.jwtExpiration = Duration.parse(jwtExpirationStr);
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	public Claims extractAllClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(rsaKeyProvider.getPublicKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	public Boolean validateToken(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
	}

	private Boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}
	
	@SuppressWarnings("unchecked")
	public List<String> extractRoles(String token) {
		Claims claims = extractAllClaims(token);
		return (List<String>) claims.get("roles");
	}

	public String generateToken(UserDetails userDetails, Long userId) {
		Map<String, Object> claims = new HashMap<>();
		
		List<String> roles = userDetails.getAuthorities().stream()
			.map(authority -> authority.getAuthority())
			.collect(java.util.stream.Collectors.toList());
		claims.put("roles", roles);
		claims.put("userId", userId);
		
		return createToken(claims, userDetails.getUsername());
	}

	private String createToken(Map<String, Object> claims, String subject) {
		long expirationMillis = System.currentTimeMillis() + jwtExpiration.toMillis();
		return Jwts.builder()
				.setHeaderParam("kid", rsaKeyProvider.getKeyId())
				.setClaims(claims)
				.setSubject(subject)
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(expirationMillis))
				.signWith(rsaKeyProvider.getPrivateKey(), SignatureAlgorithm.RS256)
				.compact();
	}
}



