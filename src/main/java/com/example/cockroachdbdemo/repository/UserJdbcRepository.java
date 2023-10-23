package com.example.cockroachdbdemo.repository;

import com.example.cockroachdbdemo.dto.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserJdbcRepository implements UserRepository {
    private static final RowMapper<User> ROW_MAPPER = (rs, __) -> new User()
            .setId(rs.getObject("id", UUID.class))
            .setCode(rs.getObject("code", UUID.class))
            .setName(rs.getString("name"))
            .setDeletedAt(rs.getObject("deleted_at", OffsetDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    @Override
    public User insert(User user) {
        jdbcTemplate.update("insert into users (id, code, name, deleted_at) values (?,?,?,?)",
                user.getId(), user.getCode(), user.getName(), user.getDeletedAt());
        return findById(user.getId()).orElseThrow();
    }

    @Override
    public Optional<User> findById(UUID id) {
        var list = jdbcTemplate.query("select * from users where deleted_at is null and id = ?",
                ROW_MAPPER,
                id);
        if (list.size() >= 2) {
            throw new IllegalStateException("list of size " + list.size() + ", expected 1");
        }
        return list.stream().findFirst();
    }

    @Override
    public List<User> listAll() {
        return jdbcTemplate.query("select * from users", ROW_MAPPER);
    }

    @Override
    public void update(User user) {
        jdbcTemplate.update("update users set code = ?, name = ?, deleted_at = ? where id = ?",
                user.getCode(), user.getName(), user.getDeletedAt(), user.getId());
    }

    @Override
    public void softDelete(UUID id) {
        jdbcTemplate.update("update users set deleted_at = now() where id = ?", id);
    }


    @Override
    public void truncateAll() {
        jdbcTemplate.execute("truncate users");
    }
}
