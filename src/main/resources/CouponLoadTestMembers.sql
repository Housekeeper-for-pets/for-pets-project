-- k6 coupon load test users.
-- k6 defaults:
--   email: k6-user-{n}@test.com
--   password: test1234!
--
-- This script copies the BCrypt password from an existing seed user created
-- with passwordEncoder.encode("test1234!").

SET @load_test_password = (
    SELECT password
    FROM member
    WHERE email IN (
        'dragonRock@test.com',
        'giljung@test.com',
        'jiwon@test.com',
        'jimin@test.com',
        'gyeongahn@test.com',
        'yeongsoo@test.com'
    )
    LIMIT 1
);

SET @start_index = 1;
SET @user_count = 3000;

DROP PROCEDURE IF EXISTS insert_coupon_load_test_members;

DELIMITER //

CREATE PROCEDURE insert_coupon_load_test_members()
BEGIN
    DECLARE i INT DEFAULT @start_index;
    DECLARE last_index INT DEFAULT @start_index + @user_count - 1;

    IF @load_test_password IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No seed member password found. Insert a member with password test1234! first.';
    END IF;

    WHILE i <= last_index DO
        INSERT INTO member (
            email,
            password,
            nickname,
            phone,
            region,
            gender,
            role,
            status,
            deleted,
            deleted_at,
            created_at,
            updated_at
        )
        VALUES (
            CONCAT('k6-user-', i, '@test.com'),
            @load_test_password,
            CONCAT('k6-user-', i),
            CONCAT('010-', LPAD(MOD(i, 10000), 4, '0'), '-', LPAD(MOD(i + 1000, 10000), 4, '0')),
            'UNKNOWN',
            'UNKNOWN',
            'MEMBER',
            'ACTIVE',
            false,
            NULL,
            NOW(),
            NOW()
        )
        ON DUPLICATE KEY UPDATE
            password = VALUES(password),
            status = 'ACTIVE',
            deleted = false,
            deleted_at = NULL,
            updated_at = NOW();

        SET i = i + 1;
    END WHILE;
END //

DELIMITER ;

CALL insert_coupon_load_test_members();

DROP PROCEDURE IF EXISTS insert_coupon_load_test_members;
