#!/usr/bin/env bash
# Smoke test nos 6 endpoints. Uso: URL=https://... CHAVE=... smoke-test.sh
set -euo pipefail
: "${URL:?}" "${CHAVE:?}"
falhas=0
checar() { # metodo caminho status_esperado [corpo]
  local metodo=$1 caminho=$2 esperado=$3 corpo=${4:-}
  local args=(-s -o /tmp/resposta -w '%{http_code}' -X "$metodo" -H "X-API-Key: $CHAVE")
  [ -n "$corpo" ] && args+=(-H 'Content-Type: application/json' -d "$corpo")
  local status
  status=$(curl "${args[@]}" "$URL$caminho")
  if [ "$status" = "$esperado" ]; then echo "OK   $metodo $caminho -> $status"; else echo "FALHA $metodo $caminho -> $status (esperado $esperado): $(head -c 300 /tmp/resposta)"; falhas=$((falhas+1)); fi
}
hoje=$(date -u +%F); semana=$(date -u -d '-7 days' +%F 2>/dev/null || date -u -v-7d +%F)
checar POST /api/v1/conversoes 200 '{"moedaOrigem":"USD","moedaDestino":"BRL","valor":100.00}'
checar GET /api/v1/moedas 200
checar GET /api/v1/cotacoes/USD/BRL 200
checar GET "/api/v1/cotacoes/USD/BRL/historico?dataInicio=$semana&dataFim=$hoje" 200
checar GET /api/v1/indicadores/BR 200
checar GET /api/v1/indices-geopoliticos/BR 200
status=$(curl -s -o /dev/null -w '%{http_code}' "$URL/api/v1/moedas"); [ "$status" = 401 ] || [ "$status" = 403 ] || { echo "FALHA sem chave -> $status"; falhas=$((falhas+1)); }
[ "$falhas" -eq 0 ] || { echo "$falhas verificacao(oes) falharam"; exit 1; }
echo "Smoke test aprovado"
