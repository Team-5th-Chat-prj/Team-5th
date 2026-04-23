-- Get-chu local seed data for E2E and smoke tests.
-- Encoding: UTF-8 without BOM. Keep this file BOM-free for Spring SQL init.
-- Test accounts:
--   buyer@test.com  / Test1234!
--   seller@test.com / Test1234!

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

INSERT INTO members (
    id, email, password, nickname, profile_image_url,
    average_rating, review_count, role, deleted,
    location, location_name, location_radius,
    created_at, updated_at
) VALUES
(1, 'buyer@test.com',  '$2a$10$I1GqC7m17M3sNXo30lNZyeMPhutqB.loYQlpN4XTekUxEO4G5Cli2', 'buyer', NULL, 0.0, 0, 'USER', false, ST_GeomFromText('POINT(126.7247 37.5328)', 4326, 'axis-order=long-lat'), '계양구 작전동', 3, NOW(), NOW()),
(2, 'seller@test.com', '$2a$10$I1GqC7m17M3sNXo30lNZyeMPhutqB.loYQlpN4XTekUxEO4G5Cli2', 'seller', NULL, 4.8, 2, 'USER', false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', 3, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    password = VALUES(password),
    nickname = VALUES(nickname),
    profile_image_url = VALUES(profile_image_url),
    average_rating = VALUES(average_rating),
    review_count = VALUES(review_count),
    role = VALUES(role),
    deleted = VALUES(deleted),
    location = VALUES(location),
    location_name = VALUES(location_name),
    location_radius = VALUES(location_radius),
    updated_at = NOW();

INSERT INTO product (
    id, seller_id, category_id, title, description, price, status,
    like_count, is_deleted, location, location_name, created_at, updated_at
) VALUES
(1, 2, 1, 'iPad Pro 11', '2022년형 아이패드 프로 11인치입니다. 애플펜슬 포함, 상태 좋아요.', 550000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', NOW(), NOW()),
(2, 2, 4, '나이키 맨투맨 L', '한 번 착용했습니다. 사이즈가 맞지 않아 판매해요.', 35000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', NOW(), NOW()),
(3, 2, 3, '이케아 책상 120cm', '2년 사용했고 상태 양호합니다. 직접 가져가실 분 선호해요.', 80000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', NOW(), NOW()),
(4, 2, 1, '갤럭시 버즈 Pro', '6개월 사용했습니다. 케이스 포함입니다.', 120000, 'RESERVED', 0, false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', NOW(), NOW()),
(5, 2, 2, '다이슨 청소기 V11', '1년 사용, 흡입력 좋습니다.', 350000, 'SOLD_OUT', 0, false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', NOW(), NOW()),
(6, 2, 5, '토익 교재 세트', '최신판이고 한 번만 봤습니다.', 25000, 'SOLD_OUT', 0, false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', NOW(), NOW()),
(210, 2, 1, '갤럭시 S24 울트라 256GB', '3개월 사용했습니다. 티타늄 블랙, 케이스와 필름 포함입니다.', 950000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', NOW(), NOW()),
(211, 2, 1, '아이패드 에어 5세대 WiFi', '1년 사용했고 애플펜슬 2세대 포함입니다.', 480000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', NOW(), NOW()),
(212, 2, 4, '아디다스 트레이닝복 세트 M', '2회 착용했습니다. 네이비 컬러입니다.', 45000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', NOW(), NOW()),
(213, 2, 6, '요가매트 세트', '6개월 사용했고 세척 완료했습니다.', 35000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', NOW(), NOW()),
(214, 2, 3, '이케아 KALLAX 책장 4x4', '2년 사용했습니다. 직거래만 가능해요.', 55000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', NOW(), NOW()),
(215, 2, 5, '파이썬 프로그래밍 책 3권', '밑줄 없는 깨끗한 책 3권 세트입니다.', 18000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', NOW(), NOW()),
(216, 2, 2, '쿠쿠 전기밥솥 6인용', '1년 사용했습니다. 정상 작동합니다.', 65000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7319 37.4627)', 4326, 'axis-order=long-lat'), '남동구 만수2동', NOW(), NOW()),
(200, 1, 1, 'MacBook Pro 14 M3', '2023년 구매한 맥북 프로 14인치입니다. 충전기 포함, 스크래치 거의 없어요.', 1800000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7247 37.5328)', 4326, 'axis-order=long-lat'), '계양구 작전동', NOW(), NOW()),
(201, 1, 1, '에어팟 프로 2세대', '6개월 사용했습니다. 배터리 상태 좋고 케이스 포함입니다.', 180000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7247 37.5328)', 4326, 'axis-order=long-lat'), '계양구 작전동', NOW(), NOW()),
(202, 1, 4, '블랙 후드티 XL', '평상시 한 번 입고 상태 좋습니다.', 28000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7247 37.5328)', 4326, 'axis-order=long-lat'), '계양구 작전동', NOW(), NOW()),
(203, 1, 6, '나이키 에어맥스 270', '3회 착용했습니다. 박스 포함입니다.', 95000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7247 37.5328)', 4326, 'axis-order=long-lat'), '계양구 작전동', NOW(), NOW()),
(204, 1, 5, '토익 LC RC 교재 세트', '2024년판이고 필기 없습니다.', 22000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7247 37.5328)', 4326, 'axis-order=long-lat'), '계양구 작전동', NOW(), NOW()),
(205, 1, 3, '허먼밀러 에어론 의자', '3년 사용했습니다. 직접 가져가실 분만 연락 주세요.', 650000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7247 37.5328)', 4326, 'axis-order=long-lat'), '계양구 작전동', NOW(), NOW()),
(206, 1, 2, '다이슨 에어랩 컴플리트', '1년 사용했습니다. 모든 어태치먼트 포함입니다.', 420000, 'SALE', 0, false, ST_GeomFromText('POINT(126.7247 37.5328)', 4326, 'axis-order=long-lat'), '계양구 작전동', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    seller_id = VALUES(seller_id),
    category_id = VALUES(category_id),
    title = VALUES(title),
    description = VALUES(description),
    price = VALUES(price),
    status = VALUES(status),
    like_count = VALUES(like_count),
    is_deleted = VALUES(is_deleted),
    location = VALUES(location),
    location_name = VALUES(location_name),
    updated_at = NOW();

DELETE FROM likes
WHERE member_id IN (1, 2)
  AND product_id IN (1, 2, 3, 4, 5, 6, 200, 201, 202, 203, 204, 205, 206, 210, 211, 212, 213, 214, 215, 216);

DELETE FROM product_image
WHERE product_id IN (1, 2, 3, 4, 5, 6, 200, 201, 202, 203, 204, 205, 206, 210, 211, 212, 213, 214, 215, 216);

INSERT INTO product_image (product_id, image_url) VALUES
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
(216, 'https://images.unsplash.com/photo-1585515320310-259814833e62?w=600')
ON DUPLICATE KEY UPDATE
    image_url = VALUES(image_url);

DELETE FROM chat_message WHERE chat_room_id = 1;
DELETE FROM chat_room WHERE id = 1;

INSERT INTO chat_room (
    id, product_id, buyer_id, seller_id,
    deleted_by_buyer, deleted_by_seller,
    last_message_at, created_at, updated_at
) VALUES
(1, 1, 1, 2, false, false, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
    product_id = VALUES(product_id),
    buyer_id = VALUES(buyer_id),
    seller_id = VALUES(seller_id),
    deleted_by_buyer = VALUES(deleted_by_buyer),
    deleted_by_seller = VALUES(deleted_by_seller),
    last_message_at = VALUES(last_message_at),
    updated_at = NOW();

INSERT INTO chat_message (
    id, chat_room_id, sender_id, sender_nickname,
    content, is_read, created_at, updated_at
) VALUES
(1, 1, 1, 'buyer', '안녕하세요! 아직 판매 중인가요?', true,  NOW(), NOW()),
(2, 1, 2, 'seller', '네, 판매 중입니다 :)', true,  NOW(), NOW()),
(3, 1, 2, 'seller', '편하신 시간에 연락 주세요.', false, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    chat_room_id = VALUES(chat_room_id),
    sender_id = VALUES(sender_id),
    sender_nickname = VALUES(sender_nickname),
    content = VALUES(content),
    is_read = VALUES(is_read),
    updated_at = NOW();

DELETE FROM reviews WHERE id IN (9001, 9002);
DELETE FROM trades WHERE trade_id IN (9001, 9002);

INSERT INTO trades (
    trade_id, product_id, buyer_id, seller_id, status,
    reserved_at, traded_at, sold_at, created_at, updated_at
) VALUES
(9001, 5, 1, 2, 'REVIEWED', NOW(), NOW(), NOW(), NOW(), NOW()),
(9002, 6, 1, 2, 'REVIEWED', NOW(), NOW(), NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE
    product_id = VALUES(product_id),
    buyer_id = VALUES(buyer_id),
    seller_id = VALUES(seller_id),
    status = VALUES(status),
    reserved_at = VALUES(reserved_at),
    traded_at = VALUES(traded_at),
    sold_at = VALUES(sold_at),
    updated_at = NOW();

INSERT INTO reviews (
    id, trade_id, reviewer_id, reviewee_id,
    rating, content, created_at, updated_at
) VALUES
(9001, 9001, 1, 2, 5.0, '친절하게 거래해주셔서 좋았어요.', NOW(), NOW()),
(9002, 9002, 1, 2, 4.5, '상품 상태가 설명과 같고 응답도 빨랐습니다.', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    trade_id = VALUES(trade_id),
    reviewer_id = VALUES(reviewer_id),
    reviewee_id = VALUES(reviewee_id),
    rating = VALUES(rating),
    content = VALUES(content),
    updated_at = NOW();
