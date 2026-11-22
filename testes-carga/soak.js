// Soak curto: 300 req/s por 30 minutos, procurando degradacao (vazamento de memoria, pool, crescimento de latencia).
import { misto } from './comum.js';

export const options = {
  scenarios: {
    soak: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.TAXA || 300),
      timeUnit: '1s',
      duration: __ENV.DURACAO || '30m',
      preAllocatedVUs: 100,
      maxVUs: 600,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{endpoint:cotacao}': ['p(95)<100'],
    'http_req_duration{endpoint:conversao}': ['p(95)<300'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default misto;
