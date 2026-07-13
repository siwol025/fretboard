CREATE TABLE IF NOT EXISTS post_like (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT NOT NULL,
    member_id  BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uq_post_like UNIQUE (post_id, member_id)
);

CREATE INDEX IF NOT EXISTS idx_post_like_post_id ON post_like (post_id);
