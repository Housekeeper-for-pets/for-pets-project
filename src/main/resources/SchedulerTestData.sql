-- ============================================================
-- 예약 만료 스케줄러 테스트용 더미 데이터 (v3 — 정합성 수정)
--
-- PortOne 호출 없이 스케줄러 동작 검증 (전부 READY/미결제)
--
-- 시나리오:
--   예약A (9001) Proposal  + 시터2 → 보호자 READY + 쿠폰 사용
--   예약B (9002) Proposal  + 시터3 → Payment 없음 (결제 시도 X)
--   예약C (9003) CareReq   + 시터2 → 시터 READY
--   예약D (9004) CareReq   + 시터3 → 양쪽 READY
-- ============================================================

use for_pets;

SET @EXPIRE_TIME = DATE_SUB(NOW(), INTERVAL 115 MINUTE);

SET @GUARDIAN_ID         = 100;
SET @SITTER_MEMBER_ID2   = 200;
SET @SITTER_MEMBER_ID3   = 300;
SET @SITTER_PROFILE_ID2  = 222;
SET @SITTER_PROFILE_ID3  = 333;
SET @PET_ID              = 400;
SET @COUPON_ID           = 500;


-- ============================================================
-- 1. Proposal 출처 예약 2건
-- ============================================================

INSERT INTO proposal (id, post_id, sitter_profile_id, sitter_member_id, member_id,
                      proposed_price, message, status, created_at, updated_at)
VALUES
    (9001, 999, @SITTER_PROFILE_ID2, @SITTER_MEMBER_ID2, @GUARDIAN_ID,
     50000, '스케줄러 테스트 제안 1', 'ACCEPTED', @EXPIRE_TIME, @EXPIRE_TIME),
    (9002, 999, @SITTER_PROFILE_ID3, @SITTER_MEMBER_ID3, @GUARDIAN_ID,
     40000, '스케줄러 테스트 제안 2', 'ACCEPTED', @EXPIRE_TIME, @EXPIRE_TIME);

INSERT INTO reservation (id, member_id, sitter_member_id, sitter_profile_id,
                         care_type, status, source, source_id,
                         created_at, updated_at)
VALUES
    (9001, @GUARDIAN_ID, @SITTER_MEMBER_ID2, @SITTER_PROFILE_ID2,
     'VISIT', 'PENDING', 'PROPOSAL', 9001,
     @EXPIRE_TIME, @EXPIRE_TIME),
    (9002, @GUARDIAN_ID, @SITTER_MEMBER_ID3, @SITTER_PROFILE_ID3,
     'BOARDING', 'PENDING', 'PROPOSAL', 9002,
     @EXPIRE_TIME, @EXPIRE_TIME);


-- ============================================================
-- 2. CareRequest 출처 예약 2건
-- ============================================================

INSERT INTO care_request (id, member_id, sitter_profile_id, sitter_member_id,
                          care_type, message, request_price, status,
                          created_at, updated_at)
VALUES
    (9001, @GUARDIAN_ID, @SITTER_PROFILE_ID2, @SITTER_MEMBER_ID2,
     'VISIT', '스케줄러 테스트 돌봄요청 1', 60000, 'ACCEPTED',
     @EXPIRE_TIME, @EXPIRE_TIME),
    (9002, @GUARDIAN_ID, @SITTER_PROFILE_ID3, @SITTER_MEMBER_ID3,
     'BOARDING', '스케줄러 테스트 돌봄요청 2', 45000, 'ACCEPTED',
     @EXPIRE_TIME, @EXPIRE_TIME);

INSERT INTO reservation (id, member_id, sitter_member_id, sitter_profile_id,
                         care_type, status, source, source_id,
                         created_at, updated_at)
VALUES
    (9003, @GUARDIAN_ID, @SITTER_MEMBER_ID2, @SITTER_PROFILE_ID2,
     'VISIT', 'PENDING', 'CARE_REQUEST', 9001,
     @EXPIRE_TIME, @EXPIRE_TIME),
    (9004, @GUARDIAN_ID, @SITTER_MEMBER_ID3, @SITTER_PROFILE_ID3,
     'BOARDING', 'PENDING', 'CARE_REQUEST', 9002,
     @EXPIRE_TIME, @EXPIRE_TIME);


-- ============================================================
-- 3. ReservationPayment (전부 미결제 — READY는 아직 confirm 안 된 상태)
-- ============================================================
INSERT INTO reservation_payment (id, reservation_id, guardian_paid, guardian_price,
                                 sitter_paid, sitter_price)
VALUES
    (9001, 9001, false, 50000, false, 10000),  -- 예약A: 보호자 READY (아직 PAID 아님)
    (9002, 9002, false, 40000, false,  8000),  -- 예약B: 둘 다 미결제
    (9003, 9003, false, 60000, false, 12000),  -- 예약C: 시터 READY (아직 PAID 아님)
    (9004, 9004, false, 45000, false,  9000);  -- 예약D: 양쪽 READY (아직 PAID 아님)


-- ============================================================
-- 4. ReservationTimeSlot
-- ============================================================
INSERT INTO reservation_time_slot (id, reservation_id, care_date, start_time, end_time, sequence)
VALUES
    (9001, 9001, DATE_ADD(CURDATE(), INTERVAL 7 DAY),  '10:00:00', '14:00:00', 1),
    (9002, 9002, DATE_ADD(CURDATE(), INTERVAL 8 DAY),  '00:00:00', '23:59:00', 1),
    (9003, 9003, DATE_ADD(CURDATE(), INTERVAL 9 DAY),  '09:00:00', '18:00:00', 1),
    (9004, 9004, DATE_ADD(CURDATE(), INTERVAL 10 DAY), '00:00:00', '23:59:00', 1);


-- ============================================================
-- 5. ReservationPet
-- ============================================================
INSERT INTO reservation_pet (id, reservation_id, pet_id,
                             name, species, breed, size, age, gender)
VALUES
    (9001, 9001, @PET_ID, '테스트멍멍이', 'DOG', '골든리트리버', 'LARGE', 3, 'MALE'),
    (9002, 9002, @PET_ID, '테스트멍멍이', 'DOG', '골든리트리버', 'LARGE', 3, 'MALE'),
    (9003, 9003, @PET_ID, '테스트멍멍이', 'DOG', '골든리트리버', 'LARGE', 3, 'MALE'),
    (9004, 9004, @PET_ID, '테스트멍멍이', 'DOG', '골든리트리버', 'LARGE', 3, 'MALE');


-- ============================================================
-- 6. UserCoupon (보호자가 예약A 결제에 쿠폰 사용)
-- ============================================================
INSERT INTO user_coupons (id, user_id, coupon_id, status, created_at, updated_at)
VALUES
    (9001, @GUARDIAN_ID, @COUPON_ID, 'USED', @EXPIRE_TIME, @EXPIRE_TIME);


-- ============================================================
-- 7. Payment (전부 READY — PortOne 호출 안 일어남)
-- ============================================================

-- 예약A: 보호자 READY + 쿠폰 적용 → 만료 시 Payment EXPIRED + 쿠폰 ACTIVE 복원
INSERT INTO payments (id, reservation_id, member_id, payment_role, payment_type,
                      original_amount, discount_amount, final_amount,
                      user_coupon_id, status, provider, merchant_uid,
                      requested_at, created_at, updated_at)
VALUES
    (9001, 9001, @GUARDIAN_ID, 'GUARDIAN', 'FULL',
     50000, 5000, 45000,
     9001, 'READY', 'PORTONE', 'TEST-EXP-9001-G',
     @EXPIRE_TIME, @EXPIRE_TIME, @EXPIRE_TIME);

-- 예약B: Payment 없음 (결제 시도 자체를 안 한 케이스)

-- 예약C: 시터 READY (쿠폰 없음 — 시터는 쿠폰 사용 불가)
INSERT INTO payments (id, reservation_id, member_id, payment_role, payment_type,
                      original_amount, discount_amount, final_amount,
                      user_coupon_id, status, provider, merchant_uid,
                      requested_at, created_at, updated_at)
VALUES
    (9002, 9003, @SITTER_MEMBER_ID2, 'SITTER', 'DEPOSIT',
     12000, 0, 12000,
     NULL, 'READY', 'PORTONE', 'TEST-EXP-9003-S',
     @EXPIRE_TIME, @EXPIRE_TIME, @EXPIRE_TIME);

-- 예약D: 보호자 READY + 시터 READY (양쪽 다 결제창만 열어놓음)
INSERT INTO payments (id, reservation_id, member_id, payment_role, payment_type,
                      original_amount, discount_amount, final_amount,
                      user_coupon_id, status, provider, merchant_uid,
                      requested_at, created_at, updated_at)
VALUES
    (9003, 9004, @GUARDIAN_ID, 'GUARDIAN', 'FULL',
     45000, 0, 45000,
     NULL, 'READY', 'PORTONE', 'TEST-EXP-9004-G',
     @EXPIRE_TIME, @EXPIRE_TIME, @EXPIRE_TIME),
    (9004, 9004, @SITTER_MEMBER_ID3, 'SITTER', 'DEPOSIT',
     9000, 0, 9000,
     NULL, 'READY', 'PORTONE', 'TEST-EXP-9004-S',
     @EXPIRE_TIME, @EXPIRE_TIME, @EXPIRE_TIME);


-- ============================================================
-- ✅ 검증 쿼리 (스케줄러 실행 전후 비교)
-- ============================================================
-- [before] Reservation=PENDING, Payment=READY, Proposal/CareRequest=ACCEPTED, UserCoupon=USED
-- [after]  Reservation=EXPIRED, Payment=EXPIRED, Proposal/CareRequest=PENDING,  UserCoupon=ACTIVE

SELECT '-- Reservation --' AS '';
SELECT id, status, source, source_id FROM reservation WHERE id IN (9001, 9002, 9003, 9004);

SELECT '-- Payment --' AS '';
SELECT id, reservation_id, payment_role, status, user_coupon_id FROM payments WHERE id IN (9001, 9002, 9003, 9004);

SELECT '-- ReservationPayment --' AS '';
SELECT id, reservation_id, guardian_paid, sitter_paid FROM reservation_payment WHERE reservation_id IN (9001, 9002, 9003, 9004);

SELECT '-- Proposal --' AS '';
SELECT id, status FROM proposal WHERE id IN (9001, 9002);

SELECT '-- CareRequest --' AS '';
SELECT id, status FROM care_request WHERE id IN (9001, 9002);

SELECT '-- UserCoupon --' AS '';
SELECT id, user_id, status FROM user_coupons WHERE id = 9001;


-- ============================================================
-- 🧹 정리 쿼리 (테스트 후 실행)
-- ============================================================
/*
DELETE FROM payments WHERE id IN (9001, 9002, 9003, 9004);
DELETE FROM reservation_pet WHERE reservation_id IN (9001, 9002, 9003, 9004);
DELETE FROM reservation_time_slot WHERE reservation_id IN (9001, 9002, 9003, 9004);
DELETE FROM reservation_payment WHERE reservation_id IN (9001, 9002, 9003, 9004);
DELETE FROM reservation WHERE id IN (9001, 9002, 9003, 9004);
DELETE FROM proposal WHERE id IN (9001, 9002);
DELETE FROM care_request WHERE id IN (9001, 9002);
DELETE FROM user_coupons WHERE id = 9001;
*/