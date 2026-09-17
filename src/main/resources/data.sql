-- 시연용 최소 seed (03 §9). 원문에 기초 데이터 표 없음 — README 가정 기재
-- branch 1~2: 지점 이름만 (헤더 식별 · 지점 격리 데모)
INSERT IGNORE INTO branch (id, name, created_at, updated_at) VALUES (1, '강남점', NOW(6), NOW(6));
INSERT IGNORE INTO branch (id, name, created_at, updated_at) VALUES (2, '잠실점', NOW(6), NOW(6));
