-- [테스트/데모 전용] 복제 지연을 결정적으로 주입한다.
-- 자동 로드되지 않는다. read-your-writes 재현(마킹 없으면 stale) 시 replica 에 수동 적용:
--
--   docker exec -i fretboard-mysql-replica \
--     mysql -uroot -pfretboard < docker/mysql/optional/source-delay.sql
--
-- 해제(지연 0 복귀):
--   docker exec -i fretboard-mysql-replica \
--     mysql -uroot -pfretboard -e "STOP REPLICA; CHANGE REPLICATION SOURCE TO SOURCE_DELAY=0; START REPLICA;"

STOP REPLICA;
CHANGE REPLICATION SOURCE TO SOURCE_DELAY = 10;  -- 초 단위(테스트에서 조정)
START REPLICA;
