-- ============================================================
-- Get-chu safe seed data
-- Minimal seed for checking frontend/backend boot without optional feature tables.
-- Test accounts:
--   buyer@test.com  / Test1234!
--   seller@test.com / Test1234!
-- ============================================================

-- Categories aligned with frontend category tabs.
INSERT INTO category (id, name) VALUES
(1, '디지털기기'),
(2, '생활가전'),
(3, '가구/인테리어'),
(4, '패션'),
(5, '도서'),
(6, '스포츠/레저'),
(7, '기타')
ON DUPLICATE KEY UPDATE
    name = VALUES(name);

-- Main frontend quick-login accounts.
INSERT INTO members (
    id, email, password, nickname, profile_image_url,
    average_rating, review_count, role, deleted,
    created_at, updated_at
) VALUES
(1, 'buyer@test.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', 'buyer', NULL, 4.8, 3, 'USER', false, NOW(), NOW()),
(2, 'seller@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', 'seller', NULL, 4.5, 3, 'USER', false, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    password = VALUES(password),
    nickname = VALUES(nickname),
    profile_image_url = VALUES(profile_image_url),
    average_rating = VALUES(average_rating),
    review_count = VALUES(review_count),
    role = VALUES(role),
    deleted = VALUES(deleted),
    updated_at = NOW();

-- Products only. No location, likes, trades, or chat seed data is used in safe mode.
INSERT INTO product (
    id, seller_id, category_id, title, description, price, status,
    like_count, is_deleted, created_at, updated_at
) VALUES
-- seller-owned products visible to buyer
(1, 2, 1, '아이패드 Pro 11인치', '2022년 모델, 거의 새것입니다. 애플펜슬 포함. 직거래 선호합니다.', 550000, 'SALE', 0, false, NOW(), NOW()),
(2, 2, 4, '나이키 맨투맨 L사이즈', '한 번 입었습니다. 사이즈 미스로 팝니다.', 35000, 'SALE', 0, false, NOW(), NOW()),
(3, 2, 3, '이케아 책상 120cm', '2년 사용, 상태 양호합니다. 이사로 인해 급처합니다.', 80000, 'SALE', 0, false, NOW(), NOW()),
(4, 2, 1, '갤럭시 버즈 Pro', '6개월 사용, 케이스 포함입니다.', 120000, 'RESERVED', 0, false, NOW(), NOW()),
(5, 2, 2, '다이슨 청소기 V11', '1년 사용, 흡입력 최상입니다.', 350000, 'RESERVED', 0, false, NOW(), NOW()),
(6, 2, 5, '토익 교재 세트', '최신판, 한 번만 봤습니다.', 25000, 'SOLD_OUT', 0, false, NOW(), NOW()),
(210, 2, 1, '갤럭시 S24 울트라 256GB', '3개월 사용, 케이스+필름 부착. 티타늄 블랙.', 950000, 'SALE', 0, false, NOW(), NOW()),
(211, 2, 1, '아이패드 에어 5세대 WiFi', '1년 사용, 애플펜슬 2세대 포함.', 480000, 'SALE', 0, false, NOW(), NOW()),
(212, 2, 4, '아디다스 트레이닝복 세트 M', '2회 착용, 상하의 세트. 네이비 컬러.', 45000, 'SALE', 0, false, NOW(), NOW()),
(213, 2, 6, '요가매트 + 필라테스 소품 세트', '6개월 사용, 세척 완료. 보관 상태 좋음.', 35000, 'SALE', 0, false, NOW(), NOW()),
(214, 2, 3, '이케아 KALLAX 책장 4x4', '2년 사용, 조립 상태 양호. 직거래만.', 55000, 'SALE', 0, false, NOW(), NOW()),
(215, 2, 5, '파이썬 프로그래밍 책 3권 세트', '밑줄 없음, 깨끗한 상태.', 18000, 'SALE', 0, false, NOW(), NOW()),
(216, 2, 2, '쿠쿠 전기밥솥 6인용', '1년 사용, 정상 작동. 박스 없음.', 65000, 'SALE', 0, false, NOW(), NOW()),
-- buyer-owned products visible to seller
(200, 1, 1, '맥북 프로 14인치 M3', '2023년 구매, 스크래치 없음. 충전기 포함.', 1800000, 'SALE', 0, false, NOW(), NOW()),
(201, 1, 1, '에어팟 프로 2세대', '6개월 사용, 케이스 포함. 배터리 95%.', 180000, 'SALE', 0, false, NOW(), NOW()),
(202, 1, 4, '무신사 스탠다드 후드티 XL', '한 번 세탁, 상태 좋음. 블랙 컬러.', 28000, 'SALE', 0, false, NOW(), NOW()),
(203, 1, 6, '나이키 에어맥스 270 270mm', '3회 착용, 박스 있음. 거의 새것.', 95000, 'SALE', 0, false, NOW(), NOW()),
(204, 1, 5, '토익 LC RC 최신판 세트', '2024년판, 필기 없음. 직거래 선호.', 22000, 'SALE', 0, false, NOW(), NOW()),
(205, 1, 3, '허먼밀러 의자 에어론', '3년 사용, 허리 지지대 정상. 직거래만.', 650000, 'SALE', 0, false, NOW(), NOW()),
(206, 1, 2, '다이슨 에어랩 완전체', '1년 사용, 모든 어태치먼트 포함.', 420000, 'SALE', 0, false, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    seller_id = VALUES(seller_id),
    category_id = VALUES(category_id),
    title = VALUES(title),
    description = VALUES(description),
    price = VALUES(price),
    status = VALUES(status),
    like_count = VALUES(like_count),
    is_deleted = VALUES(is_deleted),
    updated_at = NOW();

INSERT IGNORE INTO product_image (product_id, image_url) VALUES
(1, 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600'),
(2, 'https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=600'),
(3, 'https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=600'),
(4, 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=600'),
(5, 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=600'),
(6, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=600'),
(200, 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600'),
(201, 'https://images.unsplash.com/photo-1606220945770-b5b6c2c55bf1?w=600'),
(202, 'https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=600'),
(203, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600'),
(204, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=600'),
(205, 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=600'),
(206, 'https://images.unsplash.com/photo-1522338242992-e1a54906a8da?w=600'),
(210, 'https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=600'),
(211, 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600'),
(212, 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=600'),
(213, 'https://images.unsplash.com/photo-1518611012118-696072aa579a?w=600'),
(214, 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600'),
(215, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600'),
(216, 'https://images.unsplash.com/photo-1585515320310-259814833e62?w=600');
