// Estresse: aumenta a taxa em degraus ate a quebra (erro > 5% ou p95 > 1 s). Os thresholds abortam o teste
// no ponto de quebra; o relatorio registra a ultima taxa estavel.
import { misto } from './comum.js';

export const options = {
  scenarios: {
    estresse: {
      executor: 'ramping-arrival-rate',
      startRate: 250,
      timeUnit: '1s',
      preAllocatedVUs: 300,
      maxVUs: 3000,
      stages: [
        { target: 500, duration: '30s' },
        { target: 1000, duration: '30s' },
        { target: 1000, duration: '30s' },
        { target: 1500, duration: '30s' },
        { target: 1500, duration: '30s' },
        { target: 2000, duration: '30s' },
        { target: 2000, duration: '30s' },
        { target: 3000, duration: '30s' },
        { target: 3000, duration: '30s' },
        { target: 4000, duration: '30s' },
        { target: 4000, duration: '30s' },
      ],
    },
  },
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.05', abortOnFail: true, delayAbortEval: '20s' }],
    http_req_duration: [{ threshold: 'p(95)<1000', abortOnFail: true, delayAbortEval: '20s' }],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default misto;
