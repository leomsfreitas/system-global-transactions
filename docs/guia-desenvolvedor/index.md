# Guia do desenvolvedor

API de conversão de moedas com ajuste geopolítico. Referência completa: [referencia.html](referencia.html) (gerada da [spec](openapi.yaml)).

## Início rápido
1. Peça sua chave de API à YouPay.
2. Faça a primeira conversão:
```bash
curl -X POST https://api.transacoes.youpay.com.br/api/v1/conversoes \
  -H 'X-API-Key: SUA_CHAVE' -H 'Content-Type: application/json' \
  -d '{"moedaOrigem":"USD","moedaDestino":"BRL","valor":100.00}'
```
```json
{"moedaOrigem":"USD","moedaDestino":"BRL","valorOriginal":100.00,"taxaCambio":5.421000,
 "fatorGeopolitico":0.9850,"valorConvertido":533.97,"dataHora":"2026-11-20T18:00:00Z"}
```

## Autenticação
Todas as rotas exigem o cabeçalho `X-API-Key`. Não coloque a chave em URL nem em logs. Sem chave ou com chave inválida: `401`.

## Endpoints
| Método | Caminho | Descrição |
|---|---|---|
| POST | `/api/v1/conversoes` | Converte valor (`aplicarAjusteGeopolitico` padrão `true`) |
| GET | `/api/v1/moedas` | Moedas suportadas: BRL, USD, EUR, GBP, JPY, CHF, CAD, AUD, CNY, MXN |
| GET | `/api/v1/cotacoes/{base}/{destino}` | Cotação atual (cabeçalho `X-Data-Referencia`) |
| GET | `/api/v1/cotacoes/{base}/{destino}/historico?dataInicio=&dataFim=` | Uma cotação por dia, até 366 dias |
| GET | `/api/v1/indicadores/{pais}` | Inflação, juros e PIB mais recentes |
| GET | `/api/v1/indices-geopoliticos/{pais}` | Índice de risco (0 a 100) e fator de ajuste |

Códigos aceitam minúsculas. País usa ISO alfa-2 (`EU` para a União Europeia).

### Java (java.net.http)
```java
var cliente = HttpClient.newHttpClient();
var req = HttpRequest.newBuilder(URI.create(URL + "/api/v1/conversoes"))
    .header("X-API-Key", chave).header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(
        "{\"moedaOrigem\":\"USD\",\"moedaDestino\":\"BRL\",\"valor\":100.00}"))
    .build();
String json = cliente.send(req, HttpResponse.BodyHandlers.ofString()).body();
// Leia os valores como BigDecimal, nunca double.
```

### JavaScript (fetch)
```js
const r = await fetch(`${URL}/api/v1/cotacoes/USD/BRL`, { headers: { 'X-API-Key': chave } });
const cotacao = await r.json();
console.log(cotacao.valor, r.headers.get('X-Data-Referencia'));
```

Os outros endpoints seguem o mesmo padrão (troque o caminho); veja a referência para os corpos.

## Erros
Corpo sempre `{"codigo": 400, "mensagem": "...", "dataHora": "..."}`.
| Código | Quando |
|---|---|
| 400 | Campo ausente ou inválido, intervalo de datas inválido |
| 401 | Chave ausente ou inválida |
| 404 | Moeda, país ou dado inexistente |
| 429 | Limite de requisições excedido (respeite `Retry-After`) |
| 503 | Cotação com mais de 4 dias ou dado indisponível |

## Como funciona o ajuste geopolítico
```
valorConvertido = valor × taxaCambio × fatorGeopolitico
fatorGeopolitico = 1 − indiceRisco/100 × 0,05          (redução máxima de 5%)
indiceRisco = 0,6 × riscoPolitico + 0,4 × riscoEconomico
riscoPolitico = (2,5 − PV)/5 × 100   (PV: Political Stability, Banco Mundial WGI)
riscoEconomico = inflação/50 × 100   (limitado a 100)
```
- O fator usa o país da moeda de **destino**. Para `EU`, média dos países da zona do euro ponderada pelo PIB.
- Arredondamento HALF_EVEN: taxa 6 casas, fator 4, valor nas casas da moeda (JPY sem casas).

**Limitações:** as cotações não são de tempo real (PTAX em dias úteis, BCE uma vez por dia); os indicadores são anuais, então o índice muda devagar; as taxas do BCE são informativas e não servem para liquidação.

**Fontes:** Banco Central do Brasil; Banco Central Europeu ("Source: ECB statistics"); Banco Mundial (CC BY 4.0); Fundo Monetário Internacional.

## Limites de uso
200 req/s por chave (rajada de 400) e 10 milhões de requisições por mês.

## Versionamento
A versão está no caminho (`/api/v1`). Mudanças compatíveis (campos novos na resposta) entram na v1; mudanças incompatíveis geram `/api/v2`, com a v1 mantida por pelo menos 6 meses.

## Changelog
- **1.0.0** (22/11/2026): primeira versão, 6 endpoints.
