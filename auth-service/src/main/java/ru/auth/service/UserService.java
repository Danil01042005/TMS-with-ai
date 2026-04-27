package ru.auth.service;


import lombok.RequiredArgsConstructor;
import ru.auth.entity.User;
import ru.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.auth.dto.SignupRequest;
import ru.auth.service.exception.UserAlreadyExistsException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	public User save(User user) {
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		
		if (user.getRole() == null) {
			throw new IllegalArgumentException("User role cannot be null");
		}
		
		return userRepository.save(user);
	}

	public Optional<User> findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	public User register(SignupRequest request) {
		if (userRepository.findByUsername(request.getUsername()).isPresent()) {
			throw new UserAlreadyExistsException(request.getUsername());
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setPassword(request.getPassword());
		user.setRole(request.getRole());

		return save(user);
	}
}



