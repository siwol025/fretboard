-- Analytics replica 최초 초기화 시 1회 실행 (/docker-entrypoint-initdb.d).
-- replica-init 과 동일한 복제 구성(같은 primary, 같은 repl 계정) — 독립 노드(server-id 3).
-- 이 시점엔 read-only 가 꺼져 있어(엔트리포인트 초기화를 위해) 자유롭게 구성할 수 있다.
-- compose 의 depends_on(condition: service_healthy) 로 primary 가 준비된 뒤 실행된다.
--
-- SOURCE_AUTO_POSITION=1 : GTID 기반 자동 포지션(파일/오프셋 지정 불필요)
-- GET_SOURCE_PUBLIC_KEY=1 : 비TLS 에서 caching_sha2_password 공개키 취득 허용
CHANGE REPLICATION SOURCE TO
    SOURCE_HOST        = 'mysql-primary',
    SOURCE_PORT        = 3306,
    SOURCE_USER        = 'repl',
    SOURCE_PASSWORD    = 'replpass',
    SOURCE_AUTO_POSITION = 1,
    GET_SOURCE_PUBLIC_KEY = 1;

START REPLICA;

-- 복제 구성 후 읽기 전용으로 잠근다. SET PERSIST 는 mysqld-auto.cnf 에 기록되어
-- 재시작에도 유지되며, 복제 적용 스레드는 read-only 를 우회하므로 복제는 계속 동작한다.
SET PERSIST read_only        = ON;
SET PERSIST super_read_only  = ON;
