-- auto-generated definition
create table users
(
    -- Unique id of the row
    id         uuid primary key,
    -- unique id of the user (excluding deleted)
    code       uuid not null,
    -- user name
    name       text not null,
    -- soft delete flag
    deleted_at timestamptz
);

CREATE UNIQUE INDEX idx_users_code ON users (code)
    WHERE deleted_at is null;
