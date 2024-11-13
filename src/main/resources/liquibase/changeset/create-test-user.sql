--liquibase formatted sql
--changeset cloud_files_storage:2

INSERT INTO users (username, email, password)
VALUES ('User', 'user@gmail.com', '$2a$12$uUGwHnDSmiq.0feNvgarLOMpEyRILD3.9oTneF8TiYHtl/wJ03nty');

INSERT INTO users_roles (user_id, role)
VALUES ('1', 'ROLE_USER');