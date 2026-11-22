// Carga: 500 req/s sustentadas por 10 minutos (meta: erro < 1%, p95 cotacao < 100 ms, p95 conversao < 300 ms).
import { misto } from './comum.js';

export const options = {
  scenarios: {
    carga: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.TAXA || 500),
      timeUnit: '1s',
      duration: __ENV.DURACAO || '10m',
      preAllocatedVUs: 200,
      maxVUs: 1000,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{endpoint:cotacao}': ['p(95)<100'],
    'http_req_duration{endpoint:conversao}': ['p(95)<300'],
    dropped_iterations: ['count<3000'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default misto;
