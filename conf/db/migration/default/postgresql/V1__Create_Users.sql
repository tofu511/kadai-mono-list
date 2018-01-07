CREATE TABLE users (
    id        BIGSERIAL       NOT NULL,
    name      VARCHAR(64)     NOT NULL,
    email     VARCHAR(255)    NOT NULL,
    password  VARCHAR(64)     NOT NULL,
    create_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (email)
);