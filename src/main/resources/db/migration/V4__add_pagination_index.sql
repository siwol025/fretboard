CREATE INDEX idx_post_board_created_id  ON post (board_id, created_at, id);
CREATE INDEX idx_post_member_created_id ON post (member_id, created_at, id);

-- 복합 인덱스가 leftmost prefix로 커버하므로 중복된 단일 인덱스 제거,
-- created_at 단독 정렬 쿼리가 없어 idx_post_created_at도 제거
DROP INDEX idx_post_board_id ON post;
DROP INDEX idx_post_member_id ON post;
DROP INDEX idx_post_created_at ON post;
