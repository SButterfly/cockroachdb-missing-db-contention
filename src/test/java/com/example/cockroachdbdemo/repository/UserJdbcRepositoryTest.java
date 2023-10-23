package com.example.cockroachdbdemo.repository;

import org.springframework.beans.factory.annotation.Autowired;

class UserJdbcRepositoryTest extends BaseUserRepositoryTest {
    
    @Autowired
    private UserJdbcRepository userRepository;

    @Override
    protected UserRepository getUserRepository() {
        return userRepository;
    }
}
