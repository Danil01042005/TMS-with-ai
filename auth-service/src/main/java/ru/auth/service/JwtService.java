package ru.auth.service;


import lombok.RequiredArgsConstructor;
import ru.auth.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final AuthenticationManager authenticationManager;

    private final CustomUserDetailsService userDetailsService;

    private final JwtUtil jwtUtil;

    private final UserService userService;

    public String createJwtToken(String username, String password) throws Exception {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Long userId = userService.findByUsername(username)
                .map(ru.auth.entity.User::getId)
                .orElseThrow(() -> new IllegalStateException("User not found for username: " + username));
        return jwtUtil.generateToken(userDetails, userId);
    }

    public String generateJwtForUsername(String username) {
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Long userId = userService.findByUsername(username)
                .map(ru.auth.entity.User::getId)
                .orElseThrow(() -> new IllegalStateException("User not found for username: " + username));
        return jwtUtil.generateToken(userDetails, userId);
    }
}



