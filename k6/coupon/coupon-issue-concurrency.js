import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const STRATEGY = __ENV.STRATEGY || 'no-lock';
const COUPON_ID = __ENV.COUPON_ID || '1';

const REQUEST_COUNT = Number(__ENV.REQUEST_COUNT || 300);
const VUS = Number(__ENV.VUS || 100);

const PASSWORD = __ENV.TEST_USER_PASSWORD || 'test1234!';
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;

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
const couponServerErrors = new Counter('coupon_500_errors');
const couponUnexpectedErrors = new Counter('coupon_unexpected_errors');
const couponIssueDuration = new Trend('coupon_issue_duration_ms');

export const options = {
    scenarios: {
        coupon_issue_once_per_user: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: REQUEST_COUNT,
            maxDuration: '3m',
        },
    },
    thresholds: {
        coupon_500_errors: ['count==0'],
        coupon_unexpected_errors: ['count==0'],
        http_req_duration: ['p(95)<3000', 'p(99)<5000'],
    },
    tags: {
        strategy: STRATEGY,
        coupon_id: String(COUPON_ID),
        run_id: RUN_ID,
    },
};

export function setup() {
    console.log(`strategy=${STRATEGY}`);
    console.log(`couponId=${COUPON_ID}`);
    console.log(`requestCount=${REQUEST_COUNT}`);
    console.log(`vus=${VUS}`);
    console.log(`targetUrl=${TARGET_URL}`);

    const tokens = [];

    for (let i = 0; i < REQUEST_COUNT; i++) {
        const email = `k6-${STRATEGY}-${RUN_ID}-${i}@test.com`;
        const nickname = `k6-${STRATEGY}-${RUN_ID}-${i}`;

        const signupRes = http.post(
            `${BASE_URL}/api/auth/signup`,
            JSON.stringify({
                email,
                password: PASSWORD,
                nickname,
                phone: null,
                gender: 'UNKNOWN',
                region: 'UNKNOWN',
            }),
            {
                headers: {
                    'Content-Type': 'application/json',
                },
            }
        );

        if (signupRes.status !== 201 && signupRes.status !== 400) {
            throw new Error(`signup failed. status=${signupRes.status}, body=${signupRes.body}`);
        }

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
            throw new Error(`login failed. status=${loginRes.status}, body=${loginRes.body}`);
        }

        const body = JSON.parse(loginRes.body);

        if (!body.data || !body.data.accessToken) {
            throw new Error(`accessToken missing. body=${loginRes.body}`);
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
    const token = data.tokens[index];

    if (!token) {
        couponUnexpectedErrors.add(1);
        throw new Error(`token missing. iteration=${index}`);
    }

    const res = http.post(
        TARGET_URL,
        null,
        {
            headers: {
                Authorization: `Bearer ${token}`,
                'Content-Type': 'application/json',
            },
            tags: {
                strategy: STRATEGY,
                coupon_id: String(COUPON_ID),
                run_id: RUN_ID,
            },
        }
    );

    couponIssueDuration.add(res.timings.duration, {
        strategy: STRATEGY,
        coupon_id: String(COUPON_ID),
        run_id: RUN_ID,
    });

    if (res.status === 201) {
        couponIssued201.add(1);
    } else if (res.status === 400) {
        couponRejected400.add(1);
    } else if (res.status >= 500) {
        couponServerErrors.add(1);
        console.error(`server error. status=${res.status}, body=${res.body}`);
    } else {
        couponUnexpectedErrors.add(1);
        console.error(`unexpected status. status=${res.status}, body=${res.body}`);
    }

    check(res, {
        '201 or 400': (r) => r.status === 201 || r.status === 400,
        'no 401': (r) => r.status !== 401,
        'no 403': (r) => r.status !== 403,
        'no 5xx': (r) => r.status < 500,
    });
}