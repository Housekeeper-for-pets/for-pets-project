import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';
import exec from 'k6/execution';
import { Counter, Trend, Rate } from 'k6/metrics';

// =============================================
// 환경변수
// =============================================
const BASE_URL        = __ENV.BASE_URL || 'http://localhost:8080';
const STRATEGY        = __ENV.STRATEGY || 'pessimistic';
const COUPON_ID       = __ENV.COUPON_ID || '1';
const COUPON_QUANTITY = Number(__ENV.COUPON_QUANTITY || 100);  // 정합성 threshold용

const TOKENS_FILE = __ENV.TOKENS_FILE || './tokens.json';
const ADMIN_TOKEN = __ENV.ADMIN_TOKEN || '';

const VUS    = Number(__ENV.VUS || 300);  // 정합성 테스트에서 동시 발급 시도 수 = VU 수
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;

const endpointMap = {
    'no-lock':     `/api/test/coupons/${COUPON_ID}/issue/no-lock`,
    'optimistic':  `/api/test/coupons/${COUPON_ID}/issue/optimistic`,
    'pessimistic': `/api/test/coupons/${COUPON_ID}/issue/pessimistic`,
    'distributed': `/api/test/coupons/${COUPON_ID}/issue/distributed`,
};
if (!endpointMap[STRATEGY]) {
    throw new Error(`Unknown STRATEGY: ${STRATEGY}`);
}

const TARGET_URL  = `${BASE_URL}${endpointMap[STRATEGY]}`;
const SUMMARY_URL = `${BASE_URL}/api/test/coupons/${COUPON_ID}/summary`;

// =============================================
// 토큰 사전 로드 (SharedArray: 모든 VU가 공유)
// =============================================
const tokens = new SharedArray('tokens', () => {
    const data = JSON.parse(open(TOKENS_FILE));
    if (!Array.isArray(data) || data.length === 0) {
        throw new Error(`토큰 파일 비어있음: ${TOKENS_FILE}`);
    }
    if (data.length < VUS) {
        throw new Error(`토큰 수(${data.length}) < VU 수(${VUS})`);
    }
    return data;
});

// =============================================
// 옵션
// =============================================
export const options = {
    scenarios: {
        coupon_concurrency: {
            executor: 'per-vu-iterations',
            vus: VUS,
            iterations: 1,           // VU당 1회 → 총 VUS건 요청
            maxDuration: '5m',
        },
    },

    thresholds: {
        // 정합성 핵심 — 발급 성공 수가 쿠폰 총 수량을 넘으면 안 됨
        coupon_issued_201:        [`count<=${COUPON_QUANTITY}`],

        // 시스템/분류 오류는 0건
        coupon_500_errors:        ['count==0'],
        coupon_unexpected_errors: ['count==0'],
        coupon_business_errors:   ['count==0'],
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
const couponIssueDuration  = new Trend('coupon_issue_duration_ms', true);

const couponIssued201      = new Counter('coupon_issued_201');
const couponSoldOut        = new Counter('coupon_sold_out_400');        // COUPON_QUANTITY_EXHAUSTED
const couponLockFailed     = new Counter('coupon_lock_failed_409');     // COUPON_ISSUE_LOCK_FAILED
const couponAlreadyIssued  = new Counter('coupon_already_issued_409');  // COUPON_ALREADY_ISSUED
const couponBusinessErrors = new Counter('coupon_business_errors');     // 분류 안 된 4xx
const couponServerErrors   = new Counter('coupon_500_errors');
const couponUnexpectedErrors = new Counter('coupon_unexpected_errors');

const couponErrorRate = new Rate('coupon_error_rate');

// =============================================
// Setup
// =============================================
export function setup() {
    console.log('========== [concurrency] setup ==========');
    console.log(`strategy        = ${STRATEGY}`);
    console.log(`couponId        = ${COUPON_ID}`);
    console.log(`couponQuantity  = ${COUPON_QUANTITY}`);
    console.log(`targetUrl       = ${TARGET_URL}`);
    console.log(`vus             = ${VUS}`);
    console.log(`tokensFile      = ${TOKENS_FILE}`);
    console.log(`tokensAvailable = ${tokens.length}`);
    console.log(`adminToken      = ${ADMIN_TOKEN ? 'set' : 'NOT set (teardown summary 호출 불가)'}`);
    console.log(`runId           = ${RUN_ID}`);
    console.log('=========================================');
}

// =============================================
// 메인 요청 — VU당 1번
// =============================================
export default function () {
    const token = tokens[exec.vu.idInTest - 1];

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
// Teardown: /summary로 정합성 검증
// =============================================
export function teardown() {
    if (!ADMIN_TOKEN) {
        console.warn('[teardown] ADMIN_TOKEN 미설정 — summary 호출 생략');
        return;
    }

    const res = http.get(
        SUMMARY_URL,
        {
            headers: {
                Authorization: `Bearer ${ADMIN_TOKEN}`,
                'Content-Type': 'application/json',
            },
            tags: { name: 'teardown_summary' },  // 추가
        }
    );

    if (res.status !== 200) {
        console.error(`[teardown] summary 조회 실패: status=${res.status}, body=${res.body}`);
        return;
    }

    const body = parseJson(res.body);
    const s = body?.data;

    console.log('========== [teardown] /summary 정합성 검증 ==========');
    console.log(`totalQuantity            = ${s?.totalQuantity}`);
    console.log(`remainingQuantity        = ${s?.remainingQuantity}`);
    console.log(`issuedCount              = ${s?.issuedCount}`);
    console.log(`quantityNotExceeded      = ${s?.quantityNotExceeded}`);
    console.log(`remainingQuantityMatched = ${s?.remainingQuantityMatched}`);
    console.log(`consistent               = ${s?.consistent}`);
    console.log('====================================================');

    if (s?.consistent) {
        console.log('V /summary 정합성 통과');
    } else {
        console.error('X /summary 정합성 실패 — 초과 발급 or 잔여 수량 불일치');
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
// 결과 요약
// =============================================
export function handleSummary(data) {
    const m = data.metrics;
    const totalReqs   = (m.http_reqs?.values?.count ?? 0) - 1;
    const avgMs       = m.coupon_issue_duration_ms?.values?.avg ?? 0;
    const p95Ms       = m.coupon_issue_duration_ms?.values?.['p(95)'] ?? 0;
    const p99Ms       = m.coupon_issue_duration_ms?.values?.['p(99)'] ?? 0;
    const maxMs       = m.coupon_issue_duration_ms?.values?.max ?? 0;

    const issued201      = m.coupon_issued_201?.values?.count ?? 0;
    const soldOut        = m.coupon_sold_out_400?.values?.count ?? 0;
    const lockFailed     = m.coupon_lock_failed_409?.values?.count ?? 0;
    const alreadyIssued  = m.coupon_already_issued_409?.values?.count ?? 0;
    const businessErr    = m.coupon_business_errors?.values?.count ?? 0;
    const err500         = m.coupon_500_errors?.values?.count ?? 0;
    const unexpected     = m.coupon_unexpected_errors?.values?.count ?? 0;
    const errorRate      = ((m.coupon_error_rate?.values?.rate ?? 0) * 100).toFixed(2);

    const sum = issued201 + soldOut + lockFailed + alreadyIssued + businessErr + err500 + unexpected;

    const line = '='.repeat(60);
    const sep  = '-'.repeat(60);

    console.log('\n' + line);
    console.log(' 쿠폰 발급 정합성 검증 결과');
    console.log(line);
    console.log(` strategy        : ${STRATEGY}`);
    console.log(` couponId        : ${COUPON_ID}`);
    console.log(` couponQuantity  : ${COUPON_QUANTITY}`);
    console.log(` vus             : ${VUS}`);
    console.log(` runId           : ${RUN_ID}`);
    console.log(sep);

    console.log(' [발급 API 응답 시간]');
    console.log(`   avg : ${avgMs.toFixed(2)} ms`);
    console.log(`   p95 : ${p95Ms.toFixed(2)} ms`);
    console.log(`   p99 : ${p99Ms.toFixed(2)} ms`);
    console.log(`   max : ${maxMs.toFixed(2)} ms`);
    console.log(sep);

    console.log(' [응답 분류]');
    console.log(`   전체 요청            : ${totalReqs}`);
    console.log(`    발급 성공 (201)   : ${issued201}`);
    console.log(`    쿠폰 소진 (400)   : ${soldOut}`);
    console.log(`    중복 발급 (409)   : ${alreadyIssued}`);
    console.log(`    lock 실패 (409)   : ${lockFailed}`);
    console.log(`    분류 안된 4xx     : ${businessErr}`);
    console.log(`    500 에러          : ${err500}`);
    console.log(`    예상치 못한 에러  : ${unexpected}`);
    console.log(`   분류 합계            : ${sum} (전체와 일치해야 함)`);
    console.log(`   error rate (5xx)     : ${errorRate} %`);
    console.log(sep);

    console.log(' [정합성 판정]');
    const noOverIssue  = issued201 <= COUPON_QUANTITY;
    const noErr500     = err500 === 0;
    const noUnexpected = unexpected === 0;
    const noBizErr     = businessErr === 0;
    const sumMatches   = sum === totalReqs;

    console.log(`   발급 ≤ ${COUPON_QUANTITY}             : ${noOverIssue  ? `V ${issued201}건` : `X ${issued201}건 (초과!)`}`);
    console.log(`   500 에러 없음        : ${noErr500     ? ' V ' : ` X  ${err500}건`}`);
    console.log(`   예상치 못한 에러 0   : ${noUnexpected ? 'V' : `X ${unexpected}건`}`);
    console.log(`   분류 안된 4xx 없음   : ${noBizErr     ? 'V' : `X ${businessErr}건`}`);
    console.log(`   분류 합 = 전체 요청  : ${sumMatches   ? 'V' : `X (${sum}/${totalReqs})`}`);
    console.log(sep);

    const ok = noOverIssue && noErr500 && noUnexpected && noBizErr && sumMatches;
    console.log(ok ? ' V 정합성 검증 통과' : ' X 정합성 검증 실패');
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