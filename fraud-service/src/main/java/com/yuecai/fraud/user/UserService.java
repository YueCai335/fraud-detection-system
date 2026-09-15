package com.yuecai.fraud.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegistrationForm form) {
        String username = form.getUsername().trim();
        if (users.existsByUsername(username)) {
            throw new UserAlreadyExistsException(username);
        }
        User user = new User(
                username,
                passwordEncoder.encode(form.getPassword()),
                form.getFirstName().trim(),
                form.getLastName().trim(),
                form.getEmail().trim());
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Lost the race with a concurrent registration for the same name.
            throw new UserAlreadyExistsException(username);
        }
    }

    @Transactional(readOnly = true)
    public User requireByUsername(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("No user record for authenticated principal " + username));
    }
}
