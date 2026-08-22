-- Primary 최초 초기화 시 1회 실행 (/docker-entrypoint-initdb.d).
-- 복제 계정 생성. GTID auto-position 사용을 위해 이 계정 생성은
-- binlog 에 남기지 않는다(replica 의 GTID 집합과 충돌 방지).
SET @@SESSION.SQL_LOG_BIN = 0;

-- MySQL 8.4 기본 인증 플러그인 caching_sha2_password 사용.
-- replica 는 CHANGE REPLICATION SOURCE ... GET_SOURCE_PUBLIC_KEY=1 로
-- 비TLS 환경에서도 공개키를 받아 비밀번호를 암호화해 접속한다.
CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED BY 'replpass';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;

SET @@SESSION.SQL_LOG_BIN = 1;
