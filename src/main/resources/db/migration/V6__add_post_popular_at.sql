ALTER TABLE post ADD COLUMN popular_at DATETIME NULL;

CREATE INDEX idx_post_board_popular_at ON post (board_id, popular_at);

-- 좋아요 10개 이상인 기존 게시글을 인기글로 1회 백필(popular_at 이 아직 비어 있는 것만).
-- MySQL/H2 양쪽에서 동작하도록 상관 서브쿼리 사용, post_like.post_id → post.id 로 조인.
UPDATE post
SET popular_at = CURRENT_TIMESTAMP
WHERE popular_at IS NULL
  AND (SELECT COUNT(*) FROM post_like pl WHERE pl.post_id = post.id) >= 10;
