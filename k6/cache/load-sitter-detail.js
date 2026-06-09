import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// 커스텀 메트릭
const reqDuration = new Trend('req_duration', true);
const errorRate = new Rate('error_rate');
const requestCount = new Counter('request_count');

export const options = {
    stages: [
        { duration: '10s', target: 50 },
        { duration: '30s', target: 50 },
        { duration: '5s',  target: 0  },
    ],
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    thresholds: {
        'http_req_duration': ['p(95)<200', 'p(99)<200'],
        'error_rate': ['rate<0.01'],
    },
};

const BASE_URL = 'http://localhost:8080';

// ⚠️ DB에 실제 존재하는 sitterId로 교체 필요
// SELECT id FROM sitter_profile LIMIT 10;
const SITTER_IDS = [1, 5, 10];

export default function () {
    // 매 요청마다 sitterId 랜덤 선택
    const sitterId = SITTER_IDS[Math.floor(Math.random() * SITTER_IDS.length)];
    const url = `${BASE_URL}/api/sitters/${sitterId}`;

    const res = http.get(url, {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'GET /api/sitters/{sitterId}' },
    });

    // 메트릭 수집
    reqDuration.add(res.timings.duration);
    requestCount.add(1);
    errorRate.add(res.status !== 200);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 200ms': (r) => r.timings.duration < 200,
        'response has data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.success === true;
            } catch (_) {
                return false;
            }
        },
    });

    sleep(0.1);
}

export function handleSummary(data) {
    const dur = data.metrics.http_req_duration.values;
    const reqs = data.metrics.http_reqs.values;
    const failed = data.metrics.http_req_failed.values;

    console.log('\n========================================');
    console.log(' [GET /api/sitters/{sitterId}] 부하 테스트 결과');
    console.log('========================================');
    console.log(`  avg    : ${(dur.avg ?? 0).toFixed(2)} ms`);
    console.log(`  P95    : ${(dur['p(95)'] ?? 0).toFixed(2)} ms`);
    console.log(`  P99    : ${(dur['p(99)'] ?? 0).toFixed(2)} ms`);
    console.log(`  RPS    : ${(reqs.rate ?? 0).toFixed(2)} req/s`);
    console.log(`  에러율  : ${((failed.rate ?? 0) * 100).toFixed(2)} %`);
    console.log('========================================\n');

    return {};
}