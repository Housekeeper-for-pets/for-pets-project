-- ============================================================
-- 예약 만료 스케줄러 테스트용 더미 데이터
-- 실행 전 기존 member, sitter_profile, pet, coupon 데이터가 있어야 함
-- ID 충돌 시 시작 번호(9001~)를 조정할 것
-- ============================================================
create database for_pets;
use for_pets;

-- 시간 변수: 1시간 55분 전 (5분 뒤 스케줄러가 만료 처리)
SET @EXPIRE_TIME = DATE_SUB(NOW(), INTERVAL 115 MINUTE);

-- ============================================================
-- 기존 데이터에서 사용할 ID (환경에 맞게 조정)
-- ============================================================
SET @GUARDIAN_ID     = 100;   -- 보호자 member_id
SET @SITTER_MEMBER_ID = 200; -- 시터 member_id
SET @SITTER_PROFILE_ID = 300; -- sitter_profile id
SET @PET_ID          = 400;   -- pet id
SET @COUPON_ID       = 500;   -- coupon id



-- ============================================================
-- 1. Proposal 출처 예약 2건
-- ============================================================

-- 1-1. Proposal (ACCEPTED 상태 — 만료 시 PENDING 복원 대상)
INSERT INTO proposal (id, post_id, sitter_profile_id, sitter_member_id, member_id,
                      proposed_price, message, status, created_at, updated_at)
VALUES
    (9001, 1, @SITTER_PROFILE_ID, @SITTER_MEMBER_ID, @SITTER_MEMBER_ID,
     50000, '스케줄러 테스트 제안 1', 'ACCEPTED', @EXPIRE_TIME, @EXPIRE_TIME),
    (9002, 1, @SITTER_PROFILE_ID, @SITTER_MEMBER_ID, @SITTER_MEMBER_ID,
     40000, '스케줄러 테스트 제안 2', 'ACCEPTED', @EXPIRE_TIME, @EXPIRE_TIME);

-- 1-2. Reservation (PENDING, Proposal 출처)
--   예약 A: 보호자만 결제 완료 (환불 대상)
--   예약 B: 둘 다 미결제 (EXPIRED 처리만)
INSERT INTO reservation (id, member_id, sitter_member_id, sitter_profile_id,
                         care_type, status, source, source_id,
                         created_at, updated_at)
VALUES
    (9001, @GUARDIAN_ID, @SITTER_MEMBER_ID, @SITTER_PROFILE_ID,
     'VISIT', 'PENDING', 'PROPOSAL', 9001,
     @EXPIRE_TIME, @EXPIRE_TIME),
    (9002, @GUARDIAN_ID, @SITTER_MEMBER_ID, @SITTER_PROFILE_ID,
     'BOARDING', 'PENDING', 'PROPOSAL', 9002,
     @EXPIRE_TIME, @EXPIRE_TIME);


-- ============================================================
-- 2. CareRequest 출처 예약 2건
-- ============================================================

-- 2-1. CareRequest (ACCEPTED 상태 — 만료 시 PENDING 복원 대상)
INSERT INTO care_request (id, member_id, sitter_profile_id, sitter_member_id,
                          care_type, message, request_price, status,
                          created_at, updated_at)
VALUES
    (9001, @GUARDIAN_ID, @SITTER_PROFILE_ID, @SITTER_MEMBER_ID,
     'VISIT', '스케줄러 테스트 돌봄요청 1', 60000, 'ACCEPTED',
     @EXPIRE_TIME, @EXPIRE_TIME),
    (9002, @GUARDIAN_ID, @SITTER_PROFILE_ID, @SITTER_MEMBER_ID,
     'BOARDING', '스케줄러 테스트 돌봄요청 2', 45000, 'ACCEPTED',
     @EXPIRE_TIME, @EXPIRE_TIME);

-- 2-2. Reservation (PENDING, CareRequest 출처)
--   예약 C: 시터만 결제 완료 + 쿠폰 사용 (환불 + 쿠폰 복원 대상)
--   예약 D: 보호자 결제 READY 상태 (EXPIRED 처리 대상)
INSERT INTO reservation (id, member_id, sitter_member_id, sitter_profile_id,
                         care_type, status, source, source_id,
                         created_at, updated_at)
VALUES
    (9003, @GUARDIAN_ID, @SITTER_MEMBER_ID, @SITTER_PROFILE_ID,
     'VISIT', 'PENDING', 'CARE_REQUEST', 9001,
     @EXPIRE_TIME, @EXPIRE_TIME),
    (9004, @GUARDIAN_ID, @SITTER_MEMBER_ID, @SITTER_PROFILE_ID,
     'BOARDING', 'PENDING', 'CARE_REQUEST', 9002,
     @EXPIRE_TIME, @EXPIRE_TIME);


-- ============================================================
-- 3. ReservationPayment (예약별 결제 현황)
-- ============================================================
INSERT INTO reservation_payment (id, reservation_id, guardian_paid, guardian_price,
                                 sitter_paid, sitter_price)
VALUES
    (9001, 9001, true,  50000, false, 10000),  -- 예약A: 보호자만 결제
    (9002, 9002, false, 40000, false,  8000),  -- 예약B: 둘 다 미결제
    (9003, 9003, false, 60000, true,  12000),  -- 예약C: 시터만 결제
    (9004, 9004, false, 45000, false,  9000);  -- 예약D: 둘 다 미결제


-- ============================================================
-- 4. ReservationTimeSlot (예약별 타임슬롯 — 최소 1개씩)
-- ============================================================
INSERT INTO reservation_time_slot (id, reservation_id, care_date, start_time, end_time, sequence)
VALUES
    (9001, 9001, DATE_ADD(CURDATE(), INTERVAL 7 DAY), '10:00:00', '14:00:00', 1),
    (9002, 9002, DATE_ADD(CURDATE(), INTERVAL 8 DAY), '00:00:00', '23:59:00', 1),
    (9003, 9003, DATE_ADD(CURDATE(), INTERVAL 9 DAY), '09:00:00', '18:00:00', 1),
    (9004, 9004, DATE_ADD(CURDATE(), INTERVAL 10 DAY), '00:00:00', '23:59:00', 1);


-- ============================================================
-- 5. ReservationPet (예약별 펫 스냅샷 — 최소 1개씩)
-- ============================================================
INSERT INTO reservation_pet (id, reservation_id, pet_id,
                             name, species, breed, size, age, gender)
VALUES
    (9001, 9001, @PET_ID, '테스트멍멍이', 'DOG', '골든리트리버', 'LARGE', 3, 'MALE'),
    (9002, 9002, @PET_ID, '테스트멍멍이', 'DOG', '골든리트리버', 'LARGE', 3, 'MALE'),
    (9003, 9003, @PET_ID, '테스트멍멍이', 'DOG', '골든리트리버', 'LARGE', 3, 'MALE'),
    (9004, 9004, @PET_ID, '테스트멍멍이', 'DOG', '골든리트리버', 'LARGE', 3, 'MALE');


-- ============================================================
-- 6. UserCoupon (쿠폰 사용 — 예약C 시터 결제에 사용됨)
-- ============================================================
INSERT INTO user_coupons (id, user_id, coupon_id, status, created_at, updated_at)
VALUES
    (9001, @SITTER_MEMBER_ID, @COUPON_ID, 'USED', @EXPIRE_TIME, @EXPIRE_TIME);


-- ============================================================
-- 7. Payment (실제 결제 기록)
-- ============================================================

-- 예약A: 보호자 PAID (환불 대상)
INSERT INTO payments (id, reservation_id, member_id, payment_role, payment_type,
                      original_amount, discount_amount, final_amount,
                      user_coupon_id, status, provider, merchant_uid,
                      portone_payment_id, requested_at, approved_at,
                      created_at, updated_at)
VALUES
    (9001, 9001, @GUARDIAN_ID, 'GUARDIAN', 'FULL',
     50000, 0, 50000,
     NULL, 'PAID', 'PORTONE', 'TEST-EXP-9001-GUARDIAN',
     'portone-test-9001', @EXPIRE_TIME, @EXPIRE_TIME,
     @EXPIRE_TIME, @EXPIRE_TIME);

-- 예약B: 둘 다 미결제 (아무 Payment 없음 — 결제 요청조차 안 한 케이스)
-- (Payment 행 없음)

-- 예약C: 시터 PAID + 쿠폰 사용 (환불 + 쿠폰 복원 대상)
INSERT INTO payments (id, reservation_id, member_id, payment_role, payment_type,
                      original_amount, discount_amount, final_amount,
                      user_coupon_id, status, provider, merchant_uid,
                      portone_payment_id, requested_at, approved_at,
                      created_at, updated_at)
VALUES
    (9002, 9003, @SITTER_MEMBER_ID, 'SITTER', 'DEPOSIT',
     12000, 2400, 9600,
     9001, 'PAID', 'PORTONE', 'TEST-EXP-9003-SITTER',
     'portone-test-9003', @EXPIRE_TIME, @EXPIRE_TIME,
     @EXPIRE_TIME, @EXPIRE_TIME);

-- 예약D: 보호자 READY (결제창 열었지만 완료 안 함 — EXPIRED 처리 대상)
INSERT INTO payments (id, reservation_id, member_id, payment_role, payment_type,
                      original_amount, discount_amount, final_amount,
                      user_coupon_id, status, provider, merchant_uid,
                      requested_at, created_at, updated_at)
VALUES
    (9003, 9004, @GUARDIAN_ID, 'GUARDIAN', 'FULL',
     45000, 0, 45000,
     NULL, 'READY', 'PORTONE', 'TEST-EXP-9004-GUARDIAN',
     @EXPIRE_TIME, @EXPIRE_TIME, @EXPIRE_TIME);


-- ============================================================
-- 검증 쿼리 (실행 전/후 비교용)
-- ============================================================

-- 만료 대상 예약 확인
SELECT id, status, source, source_id, created_at
FROM reservation
WHERE id IN (9001, 9002, 9003, 9004);

-- 결제 상태 확인
SELECT id, reservation_id, payment_role, status, user_coupon_id
FROM payments
WHERE id IN (9001, 9002, 9003);

-- ReservationPayment 확인
SELECT id, reservation_id, guardian_paid, sitter_paid
FROM reservation_payment
WHERE reservation_id IN (9001, 9002, 9003, 9004);

-- Proposal 상태 확인
SELECT id, status FROM proposal WHERE id IN (9001, 9002);

-- CareRequest 상태 확인
SELECT id, status FROM care_request WHERE id IN (9001, 9002);

-- 쿠폰 상태 확인
SELECT id, user_id, status FROM user_coupons WHERE id = 9001;


-- ============================================================
-- 정리 쿼리 (테스트 후 삭제)
-- ============================================================

/*
DELETE FROM payments WHERE id IN (9001, 9002, 9003);
DELETE FROM reservation_pet WHERE reservation_id IN (9001, 9002, 9003, 9004);
DELETE FROM reservation_time_slot WHERE reservation_id IN (9001, 9002, 9003, 9004);
DELETE FROM reservation_payment WHERE reservation_id IN (9001, 9002, 9003, 9004);
DELETE FROM reservation WHERE id IN (9001, 9002, 9003, 9004);
DELETE FROM proposal WHERE id IN (9001, 9002);
DELETE FROM care_request WHERE id IN (9001, 9002);
DELETE FROM user_coupons WHERE id = 9001;
*/