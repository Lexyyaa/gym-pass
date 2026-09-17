-- 시연용 최소 seed (03 §9). 원문에 기초 데이터 표 없음 — README 가정 기재
-- branch 1~2: 지점 이름만 (헤더 식별 · 지점 격리 데모)
INSERT IGNORE INTO branch (id, name, created_at, updated_at) VALUES (1, '강남점', NOW(6), NOW(6));
INSERT IGNORE INTO branch (id, name, created_at, updated_at) VALUES (2, '잠실점', NOW(6), NOW(6));

-- member 1~4: 이름 · 연락처만 (전사 공유, D-10)
INSERT IGNORE INTO member (id, name, phone, created_at, updated_at) VALUES (1, '김지영', '010-1234-5678', NOW(6), NOW(6));
INSERT IGNORE INTO member (id, name, phone, created_at, updated_at) VALUES (2, '이민수', '010-2345-6789', NOW(6), NOW(6));
INSERT IGNORE INTO member (id, name, phone, created_at, updated_at) VALUES (3, '박서연', '010-3456-7890', NOW(6), NOW(6));
INSERT IGNORE INTO member (id, name, phone, created_at, updated_at) VALUES (4, '최준호', '010-4567-8901', NOW(6), NOW(6));

-- membership 1~4: 회원당 유효권(종료일 ≥ 오늘인 ACTIVE · PAUSED) 최대 1건 (FR-2.3)
-- ACTIVE 종료일은 평가 시점보다 충분히 뒤로 둔다 (00:00 상태 동기화 배치가 EXPIRED로 바꾸지 않게)
-- 1: 회원 1 · 지점 1 · 기간제 12개월 · ACTIVE (출입 · 목록 데모)
INSERT IGNORE INTO membership (id, member_id, branch_id, type, status, start_date, end_date, months, total_count, remaining_count, price, created_at, updated_at)
VALUES (1, 1, 1, 'PERIOD', 'ACTIVE', '2026-09-01', '2027-09-01', 12, NULL, NULL, 900000, NOW(6), NOW(6));
-- 2: 회원 2 · 지점 1 · 횟수제 10회(잔여 10) · ACTIVE, 종료일 = 시작일 + 6개월 (D-7) (차감 데모)
INSERT IGNORE INTO membership (id, member_id, branch_id, type, status, start_date, end_date, months, total_count, remaining_count, price, created_at, updated_at)
VALUES (2, 2, 1, 'COUNT', 'ACTIVE', '2026-09-01', '2027-03-01', 6, 10, 10, 200000, NOW(6), NOW(6));
-- 3: 회원 3 · 지점 1 · 기간제 3개월 · EXPIRED (상태 필터 · 출입 거부 데모)
INSERT IGNORE INTO membership (id, member_id, branch_id, type, status, start_date, end_date, months, total_count, remaining_count, price, created_at, updated_at)
VALUES (3, 3, 1, 'PERIOD', 'EXPIRED', '2026-01-01', '2026-04-01', 3, NULL, NULL, 300000, NOW(6), NOW(6));
-- 4: 회원 4 · 지점 2 · 기간제 12개월 · ACTIVE (지점 격리 403 데모)
INSERT IGNORE INTO membership (id, member_id, branch_id, type, status, start_date, end_date, months, total_count, remaining_count, price, created_at, updated_at)
VALUES (4, 4, 2, 'PERIOD', 'ACTIVE', '2026-09-01', '2027-09-01', 12, NULL, NULL, 900000, NOW(6), NOW(6));
