package com.yuecai.fraud.user;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }
}
