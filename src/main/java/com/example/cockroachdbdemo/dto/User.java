package com.example.cockroachdbdemo.dto;

import lombok.Data;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE id=?")
@Where(clause = "deleted_at IS NULL")
public class User {
    @Id
    @Column(name = "id", updatable = false)
    @GeneratedValue
    private UUID id;

    @NaturalId
    @Column(name = "code", nullable = false, updatable = false, unique = true)
    private UUID code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void prePersist() {
        if (code == null) {
            code = UUID.randomUUID();
        }
    }
}
