// Funcoes compartilhadas pelos cenarios de carga (AT11). Metas: ADR 0019 / secao 5.12.
import http from 'k6/http';
import { check } from 'k6';

export const URL = __ENV.URL || 'http://api:8080';
const CHAVE = __ENV.CHAVE || 'chave-local-de-exemplo';
const CABECALHOS = { 'X-API-Key': CHAVE, 'Content-Type': 'application/json' };

const PARES = [['USD', 'BRL'], ['EUR', 'BRL'], ['GBP', 'USD'], ['CNY', 'MXN'], ['JPY', 'BRL'], ['BRL', 'EUR']];
const PAISES = ['BR', 'US', 'EU', 'JP', 'MX'];
// EU e agregado: tem indice, mas nao indicadores proprios (404 esperado), por isso fica fora daqui.
const PAISES_INDICADORES = ['BR', 'US', 'JP', 'MX', 'DE'];

function sortear(lista) {
  return lista[Math.floor(Math.random() * lista.length)];
}

export function cotacao() {
  const [b, d] = sortear(PARES);
  const r = http.get(`${URL}/api/v1/cotacoes/${b}/${d}`, { headers: CABECALHOS, tags: { endpoint: 'cotacao' } });
  check(r, { 'cotacao 200': (x) => x.status === 200 });
}

export function conversao() {
  const [b, d] = sortear(PARES);
  const corpo = JSON.stringify({ moedaOrigem: b, moedaDestino: d, valor: (Math.random() * 10000 + 1).toFixed(2) });
  const r = http.post(`${URL}/api/v1/conversoes`, corpo, { headers: CABECALHOS, tags: { endpoint: 'conversao' } });
  check(r, { 'conversao 200': (x) => x.status === 200 });
}

export function leituraVariada() {
  const sorteio = Math.random();
  let r;
  if (sorteio < 0.4) {
    r = http.get(`${URL}/api/v1/moedas`, { headers: CABECALHOS, tags: { endpoint: 'moedas' } });
  } else if (sorteio < 0.7) {
    r = http.get(`${URL}/api/v1/indices-geopoliticos/${sortear(PAISES)}`, { headers: CABECALHOS, tags: { endpoint: 'indice' } });
  } else {
    r = http.get(`${URL}/api/v1/indicadores/${sortear(PAISES_INDICADORES)}`, { headers: CABECALHOS, tags: { endpoint: 'indicadores' } });
  }
  check(r, { 'leitura 200': (x) => x.status === 200 });
}

/** Mistura: 50% cotacao, 30% conversao, 20% demais leituras. */
export function misto() {
  const s = Math.random();
  if (s < 0.5) cotacao();
  else if (s < 0.8) conversao();
  else leituraVariada();
}
