package com.example.cockroachdbdemo.repository;

import com.example.cockroachdbdemo.dto.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID id);

    List<User> listAll();

    User insert(User user);

    void update(User user);

    void softDelete(UUID id);

    void truncateAll();
}
