import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter, Trend, Rate } from 'k6/metrics';

// =============================================
// 환경변수
// =============================================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const STRATEGY = __ENV.STRATEGY || 'pessimistic';
const COUPON_ID = __ENV.COUPON_ID || '1';

const PASSWORD = __ENV.TEST_USER_PASSWORD || 'test1234!';
const TEST_USER_EMAIL_PREFIX = __ENV.TEST_USER_EMAIL_PREFIX || 'k6-user-';
const TEST_USER_EMAIL_DOMAIN = __ENV.TEST_USER_EMAIL_DOMAIN || 'test.com';
const TEST_USER_START_INDEX = Number(__ENV.TEST_USER_START_INDEX || 1);
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;

// 임계값 탐색은 최대 VU 500 + 여유분
const TOKEN_COUNT = Number(__ENV.TOKEN_COUNT || 600);

const endpointMap = {
    'no-lock':     `/api/test/coupons/${COUPON_ID}/issue/no-lock`,
    'optimistic':  `/api/test/coupons/${COUPON_ID}/issue/optimistic`,
    'pessimistic': `/api/test/coupons/${COUPON_ID}/issue/pessimistic`,
    'distributed': `/api/test/coupons/${COUPON_ID}/issue/distributed`,
};

if (!endpointMap[STRATEGY]) {
    throw new Error(`Unknown STRATEGY: ${STRATEGY}. 사용 가능: no-lock, optimistic, pessimistic, distributed`);
}

const TARGET_URL = `${BASE_URL}${endpointMap[STRATEGY]}`;

// =============================================
// 시나리오: ramp-up (임계값 탐색)
// =============================================
export const options = {
    setupTimeout: '15m',

    scenarios: {
        coupon_threshold: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50  },  // 웜업
                { duration: '1m',  target: 100 },  // 저부하 — 쿠폰 수량과 동일
                { duration: '1m',  target: 200 },  // 중부하 — 쿠폰 수량의 2배
                { duration: '1m',  target: 300 },  // 고부하 — 쿠폰 수량의 3배 (목표 구간)
                { duration: '1m',  target: 500 },  // 최대   — 한계 탐색
                { duration: '30s', target: 0   },  // 복구
            ],
            gracefulRampDown: '30s',
        },
    },

    thresholds: {
        coupon_500_errors: ['count==0'],
        coupon_unexpected_errors: [
            { threshold: 'count<100', abortOnFail: true, delayAbortEval: '30s' }
        ],
    },

    tags: {
        strategy: STRATEGY,
        coupon_id: String(COUPON_ID),
        run_id: RUN_ID,
        test_type: 'threshold',
    },
};

// =============================================
// 커스텀 메트릭
// =============================================

// 응답 시간 (avg, p95, p99)
const couponIssueDuration = new Trend('coupon_issue_duration_ms', true);

// 발급 성공 (201)
const couponIssued201 = new Counter('coupon_issued_201');

// 비즈니스 실패 (400, 409)
const couponRejected = new Counter('coupon_rejected');
const couponSoldOut = new Counter('coupon_sold_out');       // 쿠폰 소진
const couponLockFailed = new Counter('coupon_lock_failed'); // lock 획득 실패
const couponAlreadyIssued = new Counter('coupon_already_issued');
const couponBusinessErrors = new Counter('coupon_business_errors');

// 서버 에러 (500)
const couponServerErrors = new Counter('coupon_500_errors');

// 예상치 못한 에러
const couponUnexpectedErrors = new Counter('coupon_unexpected_errors');

// Error Rate — 전체 요청 중 5xx 비율
const couponErrorRate = new Rate('coupon_error_rate');

// =============================================
// Setup: 토큰 사전 발급
// =============================================
export function setup() {
    console.log('========== [threshold] setup 시작 ==========');
    console.log(`strategy   = ${STRATEGY}`);
    console.log(`couponId   = ${COUPON_ID}`);
    console.log(`targetUrl  = ${TARGET_URL}`);
    console.log(`tokenCount = ${TOKEN_COUNT}`);
    console.log(`runId      = ${RUN_ID}`);
    console.log('=============================================');

    const tokens = [];

    for (let i = 0; i < TOKEN_COUNT; i++) {
        const userIndex = TEST_USER_START_INDEX + i;
        const email = `${TEST_USER_EMAIL_PREFIX}${userIndex}@${TEST_USER_EMAIL_DOMAIN}`;

        const res = http.post(
            `${BASE_URL}/api/auth/login`,
            JSON.stringify({ email, password: PASSWORD }),
            { headers: { 'Content-Type': 'application/json' } }
        );

        if (res.status !== 200) {
            throw new Error(`로그인 실패: email=${email}, status=${res.status}`);
        }

        const body = parseJson(res.body);
        if (!body?.data?.accessToken) {
            throw new Error(`토큰 없음: email=${email}`);
        }

        tokens.push(body.data.accessToken);
    }

    console.log(`토큰 발급 완료: ${tokens.length}개`);
    return { tokens };
}

// =============================================
// 메인 요청
// =============================================
export default function (data) {
    // 임계값 탐색은 duration 동안 반복되므로 토큰을 순환 사용
    const token = data.tokens[exec.scenario.iterationInTest % data.tokens.length];

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

    couponIssueDuration.add(res.timings.duration);
    classifyResponse(res);

    check(res, {
        '201 or business fail': (r) => r.status === 201 || r.status === 400 || r.status === 409,
        'no 5xx': (r) => r.status < 500,
        'no 401': (r) => r.status !== 401,
        'no 403': (r) => r.status !== 403,
    });
}

// =============================================
// 응답 분류
// =============================================
function classifyResponse(res) {
    // Error Rate 집계 — 5xx 여부
    couponErrorRate.add(res.status >= 500);

    if (res.status === 201) {
        couponIssued201.add(1);
        return;
    }

    if (res.status === 400 || res.status === 409) {
        couponRejected.add(1);
        const code = extractErrorCode(res.body);

        if (code === 'COUPON_QUANTITY_EXHAUSTED') { couponSoldOut.add(1); return; }
        if (code === 'COUPON_ISSUE_LOCK_FAILED')  { couponLockFailed.add(1); return; }
        if (code === 'COUPON_ALREADY_ISSUED')     { couponAlreadyIssued.add(1); return; }

        couponBusinessErrors.add(1);
        console.error(`[business error] status=${res.status}, code=${code}`);
        return;
    }

    if (res.status >= 500) {
        couponServerErrors.add(1);
        console.error(`[500] vu=${exec.scenario.activeVUs}, status=${res.status}`);
        return;
    }

    couponUnexpectedErrors.add(1);
    console.error(`[unexpected] vu=${exec.scenario.activeVUs}, status=${res.status}`);
}

// =============================================
// handleSummary: 테스트 종료 후 결과 출력
// =============================================
export function handleSummary(data) {
    const m = data.metrics;

    const totalReqs   = m.http_reqs?.values?.count ?? 0;
    const avgMs       = m.coupon_issue_duration_ms?.values?.avg ?? 0;
    const p95Ms       = m.coupon_issue_duration_ms?.values?.['p(95)'] ?? 0;
    const p99Ms       = m.coupon_issue_duration_ms?.values?.['p(99)'] ?? 0;
    const maxMs       = m.coupon_issue_duration_ms?.values?.max ?? 0;

    const issued201   = m.coupon_issued_201?.values?.count ?? 0;
    const soldOut     = m.coupon_sold_out?.values?.count ?? 0;
    const lockFailed  = m.coupon_lock_failed?.values?.count ?? 0;
    const err500      = m.coupon_500_errors?.values?.count ?? 0;
    const unexpected  = m.coupon_unexpected_errors?.values?.count ?? 0;
    const errorRate   = ((m.coupon_error_rate?.values?.rate ?? 0) * 100).toFixed(2);

    const threshold500    = m.coupon_500_errors?.thresholds?.['count==0']?.ok ?? false;
    const thresholdUnexp  = m.coupon_unexpected_errors?.thresholds?.['count==0']?.ok ?? false;
    const allPassed       = threshold500 && thresholdUnexp;

    const line = '='.repeat(60);
    const sep  = '-'.repeat(60);

    console.log('\n' + line);
    console.log(' 쿠폰 발급 임계값 탐색 결과');
    console.log(line);
    console.log(` strategy   : ${STRATEGY}`);
    console.log(` couponId   : ${COUPON_ID}`);
    console.log(` runId      : ${RUN_ID}`);
    console.log(sep);

    console.log(' [응답 시간]');
    console.log(`   avg      : ${avgMs.toFixed(2)} ms`);
    console.log(`   p95      : ${p95Ms.toFixed(2)} ms`);
    console.log(`   p99      : ${p99Ms.toFixed(2)} ms`);
    console.log(`   max      : ${maxMs.toFixed(2)} ms`);
    console.log(sep);

    console.log(' [요청 분류]');
    console.log(`   전체 요청       : ${totalReqs}`);
    console.log(`   발급 성공 (201) : ${issued201}`);
    console.log(`   쿠폰 소진       : ${soldOut}`);
    console.log(`   lock 실패       : ${lockFailed}`);
    console.log(`   500 에러        : ${err500}`);
    console.log(`   예상치 못한 에러: ${unexpected}`);
    console.log(`   error rate      : ${errorRate} %`);
    console.log(sep);

    console.log(' [임계값 판정]');
    console.log(`   500 에러 0건    : ${threshold500   ? '✅ PASS' : '❌ FAIL'}`);
    console.log(`   예상치 못한 에러: ${thresholdUnexp ? '✅ PASS' : '❌ FAIL'}`);
    console.log(sep);

    if (err500 === 0 && unexpected === 0) {
        console.log(' ✅ 전 구간 500 에러 없음 — VU 500까지 안전');
    } else {
        console.log(` ❌ 에러 발생 — 500 에러: ${err500}건, 예상치 못한 에러: ${unexpected}건`);
        console.log('    → 에러 발생 직전 VU 구간을 정합성 검증의 FIXED_VUS 기준으로 사용할 것');
    }

    console.log(line + '\n');

    return {};
}

function extractErrorCode(bodyText) {
    const body = parseJson(bodyText);
    return body?.error?.code || body?.code || 'UNKNOWN';
}

function parseJson(text) {
    try { return JSON.parse(text); } catch (_) { return null; }
}