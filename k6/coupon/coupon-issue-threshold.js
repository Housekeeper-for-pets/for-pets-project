import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const STRATEGY = __ENV.STRATEGY || 'pessimistic';
const COUPON_ID = __ENV.COUPON_ID || '1';

const PASSWORD = __ENV.TEST_USER_PASSWORD || 'test1234!';
const TEST_USER_EMAIL_PREFIX = __ENV.TEST_USER_EMAIL_PREFIX || 'k6-user-';
const TEST_USER_EMAIL_DOMAIN = __ENV.TEST_USER_EMAIL_DOMAIN || 'test.com';
const TEST_USER_START_INDEX = Number(__ENV.TEST_USER_START_INDEX || 1);
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;

const TOKEN_COUNT = Number(__ENV.TOKEN_COUNT || 10000);

const STAGE_1_VUS = Number(__ENV.STAGE_1_VUS || 50);
const STAGE_2_VUS = Number(__ENV.STAGE_2_VUS || 100);
const STAGE_3_VUS = Number(__ENV.STAGE_3_VUS || 200);
const STAGE_4_VUS = Number(__ENV.STAGE_4_VUS || 400);
const STAGE_5_VUS = Number(__ENV.STAGE_5_VUS || 700);
const STAGE_6_VUS = Number(__ENV.STAGE_6_VUS || 1000);

const STAGE_DURATION = __ENV.STAGE_DURATION || '1m';
const RECOVERY_DURATION = __ENV.RECOVERY_DURATION || '30s';

const endpointMap = {
    'no-lock': `/api/test/coupons/${COUPON_ID}/issue/no-lock`,
    'pessimistic': `/api/test/coupons/${COUPON_ID}/issue/pessimistic`,
    'optimistic': `/api/test/coupons/${COUPON_ID}/issue/optimistic`,
    'distributed': `/api/test/coupons/${COUPON_ID}/issue/distributed`,
};

if (!endpointMap[STRATEGY]) {
    throw new Error(`Unknown STRATEGY: ${STRATEGY}`);
}

const TARGET_URL = `${BASE_URL}${endpointMap[STRATEGY]}`;

const couponIssued201 = new Counter('coupon_issued_201');

const couponRejected400 = new Counter('coupon_rejected_400');
const couponSoldOut = new Counter('coupon_sold_out');
const couponAlreadyIssued = new Counter('coupon_already_issued');
const couponLockFailed = new Counter('coupon_lock_failed');
const couponBusinessErrors = new Counter('coupon_business_errors');

const couponServerErrors = new Counter('coupon_500_errors');
const couponUnexpectedErrors = new Counter('coupon_unexpected_errors');

const couponIssueDuration = new Trend('coupon_issue_duration_ms');

export const options = {
    setupTimeout: '20m',

    scenarios: {
        coupon_issue_threshold: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: STAGE_DURATION, target: STAGE_1_VUS },
                { duration: STAGE_DURATION, target: STAGE_2_VUS },
                { duration: STAGE_DURATION, target: STAGE_3_VUS },
                { duration: STAGE_DURATION, target: STAGE_4_VUS },
                { duration: STAGE_DURATION, target: STAGE_5_VUS },
                { duration: STAGE_DURATION, target: STAGE_6_VUS },
                { duration: RECOVERY_DURATION, target: 0 },
            ],
            gracefulRampDown: '30s',
        },
    },

    thresholds: {
        coupon_500_errors: ['count<1000000'],
        coupon_unexpected_errors: ['count<1000000'],
    },

    tags: {
        strategy: STRATEGY,
        coupon_id: String(COUPON_ID),
        run_id: RUN_ID,
        test_type: 'threshold',
    },
};

export function setup() {
    console.log('========== k6 coupon threshold setup ==========');
    console.log(`strategy=${STRATEGY}`);
    console.log(`couponId=${COUPON_ID}`);
    console.log(`tokenCount=${TOKEN_COUNT}`);
    console.log(`targetUrl=${TARGET_URL}`);
    console.log(`testUserStartIndex=${TEST_USER_START_INDEX}`);
    console.log(`runId=${RUN_ID}`);
    console.log(`stages=${STAGE_1_VUS},${STAGE_2_VUS},${STAGE_3_VUS},${STAGE_4_VUS},${STAGE_5_VUS},${STAGE_6_VUS}`);
    console.log('================================================');

    const tokens = [];

    for (let i = 0; i < TOKEN_COUNT; i++) {
        const userIndex = TEST_USER_START_INDEX + i;
        const email = `${TEST_USER_EMAIL_PREFIX}${userIndex}@${TEST_USER_EMAIL_DOMAIN}`;

        const loginRes = http.post(
            `${BASE_URL}/api/auth/login`,
            JSON.stringify({
                email,
                password: PASSWORD,
            }),
            {
                headers: {
                    'Content-Type': 'application/json',
                },
            }
        );

        if (loginRes.status !== 200) {
            throw new Error(`login failed. email=${email}, status=${loginRes.status}, body=${loginRes.body}`);
        }

        const body = parseJson(loginRes.body);

        if (!body || !body.data || !body.data.accessToken) {
            throw new Error(`accessToken missing. email=${email}, body=${loginRes.body}`);
        }

        tokens.push(body.data.accessToken);
    }

    console.log(`tokens=${tokens.length}`);

    return {
        tokens,
    };
}

export default function (data) {
    const index = exec.scenario.iterationInTest;

    // 임계값 테스트는 duration 동안 계속 반복되므로 준비한 토큰을 순환해서 사용한다.
    const token = data.tokens[index % data.tokens.length];

    const res = http.post(
        TARGET_URL,
        null,
        {
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
            timeout: '10s',
            tags: {
                strategy: STRATEGY,
                coupon_id: String(COUPON_ID),
                run_id: RUN_ID,
                test_type: 'threshold',
            },
        }
    );

    couponIssueDuration.add(res.timings.duration, {
        strategy: STRATEGY,
        coupon_id: String(COUPON_ID),
        run_id: RUN_ID,
        test_type: 'threshold',
    });

    classifyCouponIssueResponse(res);

    check(res, {
        '201 or business fail': (r) => r.status === 201 || r.status === 400 || r.status === 409,
        'no 401': (r) => r.status !== 401,
        'no 403': (r) => r.status !== 403,
    });
}

function classifyCouponIssueResponse(res) {
    if (res.status === 201) {
        couponIssued201.add(1);
        return;
    }

    if (res.status === 400 || res.status === 409) {
        couponRejected400.add(1);

        const errorCode = extractErrorCode(res.body);

        if (errorCode === 'COUPON_QUANTITY_EXHAUSTED') {
            couponSoldOut.add(1);
            return;
        }

        if (errorCode === 'COUPON_ALREADY_ISSUED') {
            couponAlreadyIssued.add(1);
            return;
        }

        if (errorCode === 'COUPON_ISSUE_LOCK_FAILED') {
            couponLockFailed.add(1);
            return;
        }

        couponBusinessErrors.add(1);
        console.error(`business error. status=${res.status}, code=${errorCode}, body=${res.body}`);
        return;
    }

    if (res.status >= 500) {
        couponServerErrors.add(1);
        console.error(`server error. status=${res.status}, body=${res.body}`);
        return;
    }

    couponUnexpectedErrors.add(1);
    console.error(`unexpected status. status=${res.status}, body=${res.body}`);
}

function extractErrorCode(bodyText) {
    const body = parseJson(bodyText);

    if (!body) {
        return 'BODY_PARSE_FAILED';
    }

    if (body.error && body.error.code) {
        return body.error.code;
    }

    if (body.code) {
        return body.code;
    }

    return 'UNKNOWN_ERROR_CODE';
}

function parseJson(bodyText) {
    try {
        return JSON.parse(bodyText);
    } catch (e) {
        return null;
    }
}


