package ru.auth.controller;



import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import ru.auth.dto.AuthenticationRequest;
import ru.auth.dto.AuthenticationResponse;
import ru.auth.service.JwtService;
import ru.auth.service.UserService;
import ru.auth.dto.RefreshRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.auth.service.RefreshTokenService;
import org.springframework.web.bind.annotation.CookieValue;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

	private final JwtService jwtService;

	private final UserService userService;

	private final RefreshTokenService refreshTokenService;

	private final PasswordEncoder passwordEncoder;

	@PostMapping("/login")
	public ResponseEntity<?> createAuthenticationToken(@Valid @RequestBody AuthenticationRequest authenticationRequest, HttpServletRequest request) {
		try {
			var userOpt = userService.findByUsername(authenticationRequest.getUsername());
			logger.warn("User present: {}", userOpt.isPresent());
			userOpt.ifPresent(user -> logger.warn("Password matches stored hash: {}", passwordEncoder.matches(authenticationRequest.getPassword(), user.getPassword())));
			final String jwt = jwtService.createJwtToken(
					authenticationRequest.getUsername(),
					authenticationRequest.getPassword()
			);
			var user = userService.findByUsername(authenticationRequest.getUsername()).orElseThrow();
			var refresh = refreshTokenService.issue(user);
			ResponseCookie cookie = buildRefreshCookie(refresh.getToken(), refresh.getExpiresAt(), request);
			return ResponseEntity.ok()
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(new AuthenticationResponse(jwt));
		} catch (Exception ex) {
            logger.warn("Login failed for username {}", authenticationRequest.getUsername(), ex);
			return ResponseEntity.status(401)
					.header("X-Error-Code", "AUTH_INVALID_CREDENTIALS")
					.header("X-Service", "auth-service")
					.body(java.util.Map.of(
							"code", "AUTH_INVALID_CREDENTIALS",
							"message", "Invalid username or password"
					));
		}
	}


	@PostMapping("/logout")
	public ResponseEntity<?> logout(@Valid @RequestBody(required = false) RefreshRequest body, @CookieValue(name = "refresh_token", required = false) String cookieToken, HttpServletRequest request) {
		try {
			String incoming = cookieToken != null && !cookieToken.isBlank() ? cookieToken : (body != null ? body.getRefreshToken() : null);
			if (incoming != null && !incoming.isBlank()) {
				var token = refreshTokenService.validateActive(incoming);
				refreshTokenService.revoke(token);
			}
		} catch (Exception ignored) {
		}

		ResponseCookie delete = ResponseCookie.from("refresh_token", "")
				.path("/auth")
				.maxAge(Duration.ZERO)
				.httpOnly(true)
				.sameSite(resolveSameSite(request))
				.secure(resolveSecure(request))
				.build();
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, delete.toString())
				.body(java.util.Map.of("ok", true));
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(@Valid @RequestBody(required = false) RefreshRequest body, @CookieValue(name = "refresh_token", required = false) String cookieToken, HttpServletRequest request) {
		String refreshToken = null;
		if (cookieToken != null && !cookieToken.isBlank()) {
			refreshToken = cookieToken;
		} else if (body != null && body.getRefreshToken() != null && !body.getRefreshToken().isBlank()) {
			refreshToken = body.getRefreshToken();
		}
		if (refreshToken == null || refreshToken.isBlank()) {
			return ResponseEntity.status(401)
					.header("X-Error-Code", "AUTH_REFRESH_REQUIRED")
					.header("X-Service", "auth-service")
					.body(java.util.Map.of(
							"code", "AUTH_REFRESH_REQUIRED",
							"message", "Refresh token required"
					));
		}
		try {
			var newToken = refreshTokenService.rotateByValue(refreshToken);
			var user = newToken.getUser();
			final String jwt = jwtService.generateJwtForUsername(user.getUsername());
			ResponseCookie cookie = buildRefreshCookie(newToken.getToken(), newToken.getExpiresAt(), request);
			return ResponseEntity.ok()
					.header(HttpHeaders.SET_COOKIE, cookie.toString())
					.body(new AuthenticationResponse(jwt));
		} catch (Exception ex) {
			return ResponseEntity.status(401)
					.header("X-Error-Code", "AUTH_REFRESH_INVALID")
					.header("X-Service", "auth-service")
					.body(java.util.Map.of(
							"code", "AUTH_REFRESH_INVALID",
							"message", "Invalid refresh token"
					));
		}
	}

	private ResponseCookie buildRefreshCookie(String token, Instant expiresAt, HttpServletRequest request) {
		Duration maxAge = Duration.between(Instant.now(), expiresAt);
		if (maxAge.isNegative()) maxAge = Duration.ZERO;
		return ResponseCookie.from("refresh_token", token)
				.path("/auth")
				.maxAge(maxAge)
				.httpOnly(true)
				.sameSite(resolveSameSite(request))
				.secure(resolveSecure(request))
				.build();
	}

	private boolean resolveSecure(HttpServletRequest request) {
		String proto = request.getHeader("X-Forwarded-Proto");
		if (proto != null) return "https".equalsIgnoreCase(proto);
		return request.isSecure();
	}

	private String resolveSameSite(HttpServletRequest request) {
		boolean secure = resolveSecure(request);
		return secure ? "None" : "Lax";
	}


}
