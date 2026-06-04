import http from 'k6/http';

// =============================================
// 환경변수
// =============================================
const BASE_URL    = __ENV.BASE_URL     || 'http://localhost:8080';
const PASSWORD    = __ENV.PASSWORD     || 'test1234!';
const START_INDEX = Number(__ENV.START_INDEX || 1);
const USER_COUNT  = Number(__ENV.USER_COUNT  || 3000);
const EMAIL_PREFIX = __ENV.EMAIL_PREFIX || 'k6-user-';
const EMAIL_DOMAIN = __ENV.EMAIL_DOMAIN || 'test.com';

export const options = {
    vus: 1,
    iterations: 1,
    setupTimeout: '15m',   // 600번 직렬 로그인, 여유있게
};

// =============================================
// setup: 600번 로그인 → tokens 배열 반환
// 반환값은 handleSummary의 data.setup_data로 전달됨
// =============================================
export function setup() {
    const tokens = [];
    const failed = [];
    const end = START_INDEX + USER_COUNT - 1;

    console.log(`토큰 발급 시작: ${EMAIL_PREFIX}${START_INDEX} ~ ${EMAIL_PREFIX}${end}`);

    for (let i = START_INDEX; i <= end; i++) {
        const email = `${EMAIL_PREFIX}${i}@${EMAIL_DOMAIN}`;

        const res = http.post(
            `${BASE_URL}/api/auth/login`,
            JSON.stringify({ email, password: PASSWORD }),
            { headers: { 'Content-Type': 'application/json' } }
        );

        if (res.status !== 200) {
            failed.push(email);
            console.error(`[${i}] 로그인 실패: status=${res.status}, email=${email}`);
            continue;
        }

        const body = JSON.parse(res.body);
        const token = body?.data?.accessToken;

        if (!token) {
            failed.push(email);
            console.error(`[${i}] 토큰 없음: email=${email}`);
            continue;
        }

        tokens.push(token);

        if ((i - START_INDEX + 1) % 100 === 0) {
            console.log(`진행: ${i - START_INDEX + 1}/${USER_COUNT}`);
        }
    }

    console.log(`발급 완료 — 성공: ${tokens.length}개 / 실패: ${failed.length}개`);

    if (tokens.length === 0) {
        throw new Error('토큰이 하나도 발급되지 않았습니다. 이메일/비밀번호를 확인하세요.');
    }

    return { tokens, failed };
}

// 토큰 발급 전용 스크립트 — VU 실행 없음
export default function () {}

// =============================================
// handleSummary: data.setup_data.tokens → tokens.json 저장
// =============================================
export function handleSummary(data) {
    const tokens = data.setup_data?.tokens || [];
    const failed = data.setup_data?.failed || [];

    if (failed.length > 0) {
        console.warn(`실패 계정 ${failed.length}개:`);
        failed.slice(0, 10).forEach(e => console.warn(`  - ${e}`));
        if (failed.length > 10) console.warn(`  ... 외 ${failed.length - 10}개`);
    }

    return {
        '/scripts/coupon/tokens.json': JSON.stringify(tokens, null, 2),
        stdout: `\n✅ 토큰 ${tokens.length}개 → /scripts/coupon/tokens.json 저장 완료\n`,
    };
}