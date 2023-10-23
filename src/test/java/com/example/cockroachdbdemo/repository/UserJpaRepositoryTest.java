package com.example.cockroachdbdemo.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserJpaRepositoryTest extends BaseUserRepositoryTest {

    @Autowired
    private UserJpaRepository userRepository;

    @Override
    protected UserRepository getUserRepository() {
        return userRepository;
    }
}
