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

// 임계값 탐색에서 찾은 전략별 안전한 VU로 설정
// ex) pessimistic이 VU 300까지 안전했다면 FIXED_VUS=300
const FIXED_VUS = Number(__ENV.FIXED_VUS || 300);

// 정합성 검증은 쿠폰 수량만큼만 요청 (100개 쿠폰 → 300명이 경쟁)
// 쿠폰 수량보다 많아야 경쟁 상황이 만들어짐
const REQUEST_COUNT = Number(__ENV.REQUEST_COUNT || 300);

const PASSWORD = __ENV.TEST_USER_PASSWORD || 'test1234!';
const TEST_USER_EMAIL_PREFIX = __ENV.TEST_USER_EMAIL_PREFIX || 'k6-user-';
const TEST_USER_EMAIL_DOMAIN = __ENV.TEST_USER_EMAIL_DOMAIN || 'test.com';
const TEST_USER_START_INDEX = Number(__ENV.TEST_USER_START_INDEX || 1);
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;

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
const SUMMARY_URL = `${BASE_URL}/api/test/coupons/${COUPON_ID}/summary`;

// =============================================
// 시나리오: fixed VU (정합성 검증)
// 각 VU가 정확히 1번씩만 요청 → 총 REQUEST_COUNT건 요청
// =============================================
export const options = {
    setupTimeout: '15m',

    scenarios: {
        coupon_concurrency: {
            executor: 'per-vu-iterations',
            vus: FIXED_VUS,
            iterations: 1,
            maxDuration: '5m',
        },
    },

    thresholds: {
        // 정합성 검증 핵심: 500 에러 0건
        coupon_500_errors:        ['count==0'],
        coupon_unexpected_errors: ['count==0'],
    },

    tags: {
        strategy: STRATEGY,
        coupon_id: String(COUPON_ID),
        run_id: RUN_ID,
        test_type: 'concurrency',
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
    console.log('========== [concurrency] setup 시작 ==========');
    console.log(`strategy     = ${STRATEGY}`);
    console.log(`couponId     = ${COUPON_ID}`);
    console.log(`targetUrl    = ${TARGET_URL}`);
    console.log(`fixedVUs     = ${FIXED_VUS}`);
    console.log(`requestCount = ${REQUEST_COUNT}`);
    console.log(`runId        = ${RUN_ID}`);
    console.log('===============================================');

    const tokens = [];

    for (let i = 0; i < REQUEST_COUNT; i++) {
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
// 메인 요청 — VU당 1번만 실행
// =============================================
export default function (data) {
    const token = data.tokens[exec.vu.idInTest - 1];

    if (!token) {
        couponUnexpectedErrors.add(1);
        console.error(`토큰 없음: vuId=${exec.vu.idInTest}`);
        return;
    }

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
                test_type: 'concurrency',
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
// Teardown: /summary API로 정합성 검증
// =============================================
export function teardown(data) {
    // ADMIN 토큰으로 summary 조회 (첫 번째 토큰 사용 또는 별도 ADMIN 토큰 설정)
    const adminToken = __ENV.ADMIN_TOKEN || data.tokens[0];

    const res = http.get(
        SUMMARY_URL,
        {
            headers: {
                Authorization: `Bearer ${adminToken}`,
                'Content-Type': 'application/json',
            },
        }
    );

    if (res.status !== 200) {
        console.error(`[summary] 조회 실패: status=${res.status}`);
        return;
    }

    const body = parseJson(res.body);
    const summary = body?.data;

    console.log('========== [concurrency] 정합성 검증 결과 ==========');
    console.log(`strategy              = ${STRATEGY}`);
    console.log(`couponId              = ${COUPON_ID}`);
    console.log(`totalQuantity         = ${summary?.totalQuantity}`);
    console.log(`remainingQuantity     = ${summary?.remainingQuantity}`);
    console.log(`issuedCount           = ${summary?.issuedCount}`);
    console.log(`quantityNotExceeded   = ${summary?.quantityNotExceeded}`);
    console.log(`remainingQuantityMatched = ${summary?.remainingQuantityMatched}`);
    console.log(`consistent            = ${summary?.consistent}`);
    console.log('====================================================');

    // 정합성 최종 판단
    if (summary?.consistent) {
        console.log('✅ 정합성 검증 통과: 초과 발급 없음');
    } else {
        console.error('❌ 정합성 검증 실패: 초과 발급 발생');
    }
}

// =============================================
// 응답 분류
// =============================================
function classifyResponse(res) {
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
        console.error(`[500 error] status=${res.status}, body=${res.body}`);
        return;
    }

    couponUnexpectedErrors.add(1);
    console.error(`[unexpected] status=${res.status}, body=${res.body}`);
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

    const threshold500   = m.coupon_500_errors?.thresholds?.['count==0']?.ok ?? false;
    const thresholdUnexp = m.coupon_unexpected_errors?.thresholds?.['count==0']?.ok ?? false;

    const line = '='.repeat(60);
    const sep  = '-'.repeat(60);

    console.log('\n' + line);
    console.log(' 쿠폰 발급 정합성 검증 결과');
    console.log(line);
    console.log(` strategy     : ${STRATEGY}`);
    console.log(` couponId     : ${COUPON_ID}`);
    console.log(` fixedVUs     : ${FIXED_VUS}`);
    console.log(` requestCount : ${REQUEST_COUNT}`);
    console.log(` runId        : ${RUN_ID}`);
    console.log(sep);

    console.log(' [응답 시간]');
    console.log(`   avg         : ${avgMs.toFixed(2)} ms`);
    console.log(`   p95         : ${p95Ms.toFixed(2)} ms`);
    console.log(`   p99         : ${p99Ms.toFixed(2)} ms`);
    console.log(`   max         : ${maxMs.toFixed(2)} ms`);
    console.log(sep);

    console.log(' [요청 분류]');
    console.log(`   전체 요청         : ${totalReqs}`);
    console.log(`   발급 성공 (201)   : ${issued201}`);
    console.log(`   쿠폰 소진         : ${soldOut}`);
    console.log(`   lock 실패         : ${lockFailed}`);
    console.log(`   500 에러          : ${err500}`);
    console.log(`   예상치 못한 에러  : ${unexpected}`);
    console.log(`   error rate        : ${errorRate} %`);
    console.log(sep);

    console.log(' [임계값 판정]');
    console.log(`   500 에러 0건      : ${threshold500   ? '✅ PASS' : '❌ FAIL'}`);
    console.log(`   예상치 못한 에러  : ${thresholdUnexp ? '✅ PASS' : '❌ FAIL'}`);
    console.log(sep);

    console.log(' [정합성 판정]');
    const issuedOk   = issued201 <= 100;
    const noErr500   = err500 === 0;
    const noLockFail = lockFailed === 0;

    console.log(`   발급 수 ≤ 100     : ${issuedOk   ? `✅ ${issued201}건` : `❌ ${issued201}건 (초과 발급!)`}`);
    console.log(`   500 에러 없음     : ${noErr500   ? '✅ PASS' : `❌ FAIL (${err500}건)`}`);
    console.log(`   lock 실패 없음    : ${noLockFail ? '✅ PASS' : `⚠  ${lockFailed}건 발생`}`);
    console.log(sep);

    const allPassed = issuedOk && noErr500;
    if (allPassed) {
        console.log(` ✅ 정합성 검증 통과 — 초과 발급 없음`);
    } else {
        console.log(` ❌ 정합성 검증 실패 — 결과를 확인하세요`);
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