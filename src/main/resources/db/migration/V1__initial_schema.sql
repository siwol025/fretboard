CREATE TABLE IF NOT EXISTS member (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    nickname   VARCHAR(255) NOT NULL UNIQUE,
    role       VARCHAR(50)  NOT NULL,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS board (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    slug        VARCHAR(50)  NOT NULL UNIQUE,
    board_type  VARCHAR(50),
    created_at  DATETIME,
    updated_at  DATETIME
);

CREATE TABLE IF NOT EXISTS post (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id  BIGINT,
    board_id   BIGINT,
    title      VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    view_count BIGINT       NOT NULL DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE INDEX IF NOT EXISTS idx_post_board_id   ON post (board_id);
CREATE INDEX IF NOT EXISTS idx_post_member_id  ON post (member_id);
CREATE INDEX IF NOT EXISTS idx_post_created_at ON post (created_at);

CREATE TABLE IF NOT EXISTS comment (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    content    VARCHAR(255) NOT NULL,
    post_id    BIGINT,
    member_id  BIGINT,
    parent_id  BIGINT,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE INDEX IF NOT EXISTS idx_comment_post_id   ON comment (post_id);
CREATE INDEX IF NOT EXISTS idx_comment_member_id ON comment (member_id);
