CREATE TABLE users (
    id CHAR(36) NOT NULL,
    account_status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_users_account_status CHECK (account_status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
