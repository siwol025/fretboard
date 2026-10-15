ALTER TABLE post ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0;

-- 기존 게시글의 like_count 를 post_like 개수로 1회 백필한다.
-- MySQL/H2 양쪽에서 동작하도록 상관 서브쿼리 사용, post_like.post_id → post.id 로 조인.
UPDATE post p
SET like_count = (SELECT COUNT(*) FROM post_like pl WHERE pl.post_id = p.id);
