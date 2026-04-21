-- ============================================================
-- 테스트용 더미 데이터 (INSERT IGNORE: 중복 실행 안전)
-- 비밀번호: Test1234! (BCrypt 해시)
-- ============================================================

-- ─── 카테고리 ────────────────────────────────────────────
INSERT IGNORE INTO category (id, name) VALUES
(1, '디지털기기'),
(2, '생활가전'),
(3, '가구/인테리어'),
(4, '의류'),
(5, '도서'),
(6, '스포츠/레저'),
(7, '기타');

-- ─── 회원 ────────────────────────────────────────────────
-- 비밀번호: Test1234!
INSERT IGNORE INTO members (id, email, password, nickname, profile_image_url, average_rating, review_count, role, deleted, created_at, updated_at) VALUES
(1, 'buyer@test.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', '구매자테스터', NULL, 0.0, 0, 'USER', false, NOW(), NOW()),
(2, 'seller@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', '판매자테스터', NULL, 4.5, 3, 'USER', false, NOW(), NOW());

-- ─── 상품 ────────────────────────────────────────────────
INSERT IGNORE INTO PRODUCT (id, seller_id, category_id, title, description, price, status, like_count, is_deleted, created_at, updated_at) VALUES
-- 판매중 상품 (구매자가 예약/채팅 테스트용)
(1, 2, 1, '아이패드 Pro 11인치', '2022년 모델, 거의 새것입니다.\n애플펜슬 포함.\n직거래 선호합니다.', 550000, 'SALE', 5, false, NOW(), NOW()),
(2, 2, 4, '나이키 맨투맨 L사이즈', '한 번 입었습니다. 사이즈 미스로 팝니다.', 35000, 'SALE', 2, false, NOW(), NOW()),
(3, 2, 3, '이케아 책상 120cm', '2년 사용, 상태 양호합니다. 이사로 인해 급처합니다.', 80000, 'SALE', 8, false, NOW(), NOW()),
-- 예약중 상품 (판매자가 거래시작 버튼 테스트용)
(4, 2, 1, '갤럭시 버즈 Pro', '6개월 사용, 케이스 포함입니다.', 120000, 'RESERVED', 3, false, NOW(), NOW()),
-- 거래중 상품 (판매자가 거래완료 버튼 테스트용)
(5, 2, 2, '다이슨 청소기 V11', '1년 사용, 흡입력 최상입니다.', 350000, 'RESERVED', 1, false, NOW(), NOW()),
-- 판매완료 상품 (구매자가 리뷰 작성 테스트용)
(6, 2, 5, '토익 교재 세트', '최신판, 한 번만 봤습니다.', 25000, 'SOLD_OUT', 0, false, NOW(), NOW());

-- ─── 상품 이미지 ─────────────────────────────────────────
INSERT IGNORE INTO product_image (product_id, image_url, sort_order) VALUES
(1, 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600', 0),
(2, 'https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=600', 0),
(3, 'https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=600', 0),
(4, 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=600', 0),
(5, 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=600', 0),
(6, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=600', 0);

-- ─── 거래 ────────────────────────────────────────────────
-- 예약중 거래 (상품4: 판매자가 거래시작 가능)
INSERT IGNORE INTO trades (trade_id, product_id, buyer_id, seller_id, status, reserved_at, traded_at, sold_at, created_at, updated_at) VALUES
(1, 4, 1, 2, 'RESERVED', NOW(), NULL, NULL, NOW(), NOW()),
-- 거래중 거래 (상품5: 판매자가 거래완료 가능)
(2, 5, 1, 2, 'TRADING',  NOW(), NOW(), NULL, NOW(), NOW()),
-- 판매완료 거래 (상품6: 구매자가 리뷰 작성 가능)
(3, 6, 1, 2, 'SOLD',     NOW(), NOW(), NOW(), NOW(), NOW());

-- ─── 채팅방 ──────────────────────────────────────────────
-- 구매자(1)↔판매자(2), 상품1 채팅방
INSERT IGNORE INTO chat_room (id, product_id, buyer_id, seller_id, last_message_at, created_at, updated_at) VALUES
(1, 1, 1, 2, NOW(), NOW(), NOW());

-- ─── 채팅 메시지 ─────────────────────────────────────────
INSERT IGNORE INTO chat_message (id, chat_room_id, sender_id, sender_nickname, content, is_read, created_at, updated_at) VALUES
(1, 1, 1, '구매자테스터', '안녕하세요! 아직 판매 중인가요?', true,  NOW(), NOW()),
(2, 1, 2, '판매자테스터', '네, 판매 중입니다 :)',             true,  NOW(), NOW()),
(3, 1, 1, '구매자테스터', '직거래 가능한가요?',               false, NOW(), NOW());

-- ─── 추가 더미 데이터 (짱구 id:9 / 햅삐 id:10) ──────────
-- 짱구 판매 상품
INSERT IGNORE INTO PRODUCT (id, seller_id, category_id, title, description, price, status, like_count, is_deleted, created_at, updated_at) VALUES
(200, 9, 1, '맥북 프로 14인치 M3', '2023년 구매, 스크래치 없음. 충전기 포함.', 1800000, 'SALE', 12, false, NOW(), NOW()),
(201, 9, 1, '에어팟 프로 2세대', '6개월 사용, 케이스 포함. 배터리 95%.', 180000, 'SALE', 7, false, NOW(), NOW()),
(202, 9, 4, '무신사 스탠다드 후드티 XL', '한 번 세탁, 상태 좋음. 블랙 컬러.', 28000, 'SALE', 3, false, NOW(), NOW()),
(203, 9, 6, '나이키 에어맥스 270 270mm', '3회 착용, 박스 있음. 거의 새것.', 95000, 'SALE', 9, false, NOW(), NOW()),
(204, 9, 5, '토익 LC RC 최신판 세트', '2024년판, 필기 없음. 직거래 선호.', 22000, 'SALE', 2, false, NOW(), NOW()),
(205, 9, 3, '허먼밀러 의자 에어론', '3년 사용, 허리 지지대 정상. 직거래만.', 650000, 'SALE', 15, false, NOW(), NOW()),
(206, 9, 2, '다이슨 에어랩 완전체', '1년 사용, 모든 어태치먼트 포함.', 420000, 'SALE', 11, false, NOW(), NOW()),

-- 햅삐 판매 상품
(210, 10, 1, '갤럭시 S24 울트라 256GB', '3개월 사용, 케이스+필름 부착. 티타늄 블랙.', 950000, 'SALE', 8, false, NOW(), NOW()),
(211, 10, 1, '아이패드 에어 5세대 WiFi', '1년 사용, 애플펜슬 2세대 포함.', 480000, 'SALE', 6, false, NOW(), NOW()),
(212, 10, 4, '아디다스 트레이닝복 세트 M', '2회 착용, 상하의 세트. 네이비 컬러.', 45000, 'SALE', 4, false, NOW(), NOW()),
(213, 10, 6, '요가매트 + 필라테스 소품 세트', '6개월 사용, 세척 완료. 보관 상태 좋음.', 35000, 'SALE', 5, false, NOW(), NOW()),
(214, 10, 3, '이케아 KALLAX 책장 4x4', '2년 사용, 조립 상태 양호. 직거래만.', 55000, 'SALE', 3, false, NOW(), NOW()),
(215, 10, 5, '파이썬 프로그래밍 책 3권 세트', '밑줄 없음, 깨끗한 상태.', 18000, 'SALE', 1, false, NOW(), NOW()),
(216, 10, 2, '쿠쿠 전기밥솥 6인용', '1년 사용, 정상 작동. 박스 없음.', 65000, 'SALE', 7, false, NOW(), NOW());

-- ─── 상품 이미지 ─────────────────────────────────────────
INSERT IGNORE INTO product_image (product_id, image_url, sort_order) VALUES
(200, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600', 0),
(201, 'https://images.unsplash.com/photo-1606220945770-b5b6c2c55bf1?w=600', 0),
(202, 'https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=600', 0),
(203, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600', 0),
(204, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=600', 0),
(205, 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=600', 0),
(206, 'https://images.unsplash.com/photo-1522338242992-e1a54906a8da?w=600', 0),
(210, 'https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=600', 0),
(211, 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600', 0),
(212, 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=600', 0),
(213, 'https://images.unsplash.com/photo-1518611012118-696072aa579a?w=600', 0),
(214, 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600', 0),
(215, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600', 0),
(216, 'https://images.unsplash.com/photo-1585515320310-259814833e62?w=600', 0);
