CREATE TABLE completions (
    id CHAR(36) NOT NULL,
    task_id CHAR(36) NOT NULL,
    marked_at DATETIME(6) NOT NULL,
    marked_on DATE NOT NULL,
    policy VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    once_scope CHAR(36) AS (
        CASE
            WHEN policy = 'ONCE_PER_DAY' THEN task_id
            ELSE id
        END
    ) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uq_completions_daily (marked_on, once_scope),
    KEY idx_completions_task_time (task_id, marked_at),
    CONSTRAINT fk_completions_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE RESTRICT,
    CONSTRAINT chk_completions_policy CHECK (policy IN ('ONCE_PER_DAY', 'UNLIMITED')),
    CONSTRAINT chk_completions_id CHECK (id <> '00000000-0000-0000-0000-000000000000')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
