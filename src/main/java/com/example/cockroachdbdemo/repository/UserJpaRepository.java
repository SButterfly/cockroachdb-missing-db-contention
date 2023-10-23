package com.example.cockroachdbdemo.repository;

import com.example.cockroachdbdemo.dto.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<User, UUID>, UserRepository {

    @Override
    @Query(value = "select * from users", nativeQuery = true)
    List<User> listAll();

    @Override
    default User insert(User user) {
        return save(user);
    }

    @Override
    default void update(User user) {
        save(user);
    }

    @Override
    default void softDelete(UUID id) {
        doSoftDelete(id);
    }

    // TODO do we need explicitly tell specify delete operation?
    @Modifying
    @Transactional
    @Query("UPDATE User rr " +
            "SET rr.deletedAt = CURRENT_TIMESTAMP " +
            "WHERE rr.id = :id")
    int doSoftDelete(@Param("id") UUID id);

    @Override
    @Modifying
    @Transactional
    @Query(value = "truncate users", nativeQuery = true)
    void truncateAll();
}
