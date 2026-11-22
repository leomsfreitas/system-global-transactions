# Fixtures dos conectores

Respostas **reais** das fontes externas, gravadas em 22/11/2026 com `curl` durante a AT01.
Elas alimentam os stubs do WireMock nos testes dos conectores e o ambiente local (`make up`).

Arquivos marcados como **sintético** reproduzem cenários que não dá para provocar sob demanda
(erro 500, JSON truncado). Todos os outros são cópia byte a byte da resposta da fonte.

## BCB (`bcb/`)

| Arquivo | Requisição | Observação |
|---|---|---|
| `ptax-moedas.json` | `GET olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/Moedas?$format=json` | 10 moedas: AUD CAD CHF DKK EUR GBP JPY NOK SEK USD |
| `ptax-cotacao-periodo-usd.json` | `CotacaoMoedaPeriodo(moeda=@moeda,dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)?@moeda='USD'&@dataInicial='11-16-2026'&@dataFinalCotacao='11-20-2026'` | 5 boletins/dia (Abertura, 3 Intermediários, Fechamento); 20/11 é feriado nacional, sem dados |
| `ptax-cotacao-dia-eur.json` | `CotacaoMoedaDia(...)?@moeda='EUR'&@dataCotacao='11-19-2026'` | Inclui `paridadeCompra/Venda` (USD por EUR) e boletim `Fechamento PTAX` |
| `ptax-cotacao-dia-jpy.json` | idem, `JPY` | Moeda tipo A: paridade em JPY por USD |
| `ptax-cotacao-dia-vazio.json` | idem, `USD` em 21/11/2026 (sábado) | `value: []` com HTTP 200. Data em formato errado ou moeda inexistente também devolve vazio com 200, nunca erro |
| `sgs-selic-meta-432.json` | `api.bcb.gov.br/dados/serie/bcdata.sgs.432/dados/ultimos/3?formato=json` | **Contém datas futuras** (meta vigente até a próxima reunião do Copom) |
| `sgs-selic-diaria-11.json` | `bcdata.sgs.11/dados/ultimos/3` | Taxa diária (% ao dia) |
| `sgs-ipca-433.json` | `bcdata.sgs.433/dados?formato=json&dataInicial=01/01/2025&dataFinal=22/11/2026` | IPCA mensal. Com `/ultimos/N` a série devolveu **HTTP 304 sem corpo** de forma reproduzível |
| `sgs-ipca-12m-13522.json` | `bcdata.sgs.13522/dados?...` | IPCA acumulado 12 meses |
| `sgs-serie-inexistente-404.txt` | `bcdata.sgs.99999999/...` | HTTP 404, corpo texto `A serie nao existe!` |
| `ptax-erro-500.html` | sintético | HTML de erro de proxy |
| `ptax-malformado.json` | sintético | JSON truncado |

## BCE (`bce/`)

| Arquivo | Requisição | Observação |
|---|---|---|
| `exr-diario-multi.json` | `data-api.ecb.europa.eu/service/data/EXR/D.USD+BRL+GBP+JPY+CHF+CAD+AUD+CNY+MXN+ARS.EUR.SP00.A?startPeriod=2026-11-16&endPeriod=2026-11-20&format=jsondata` | SDMX-JSON. ARS não vem (série encerrada em 30/10/2020) |
| `exr-diario-usd.csv` | `D.USD.EUR.SP00.A?...&format=csvdata` | Alternativa em CSV |
| `exr-vazio-200.json` | `D.USD.EUR.SP00.A?startPeriod=2026-11-21&endPeriod=2026-11-22` | **HTTP 200 com corpo vazio** (0 bytes) em fim de semana |
| `exr-serie-inexistente-404.txt` | `D.XXX.EUR.SP00.A` | HTTP 404, JSON `{"detail": "No Series was returned..."}` |
| `exr-malformado.json` | sintético | JSON truncado |

## Banco Mundial (`bancomundial/`)

| Arquivo | Requisição | Observação |
|---|---|---|
| `inflacao-br.json` | `api.worldbank.org/v2/country/BR/indicator/FP.CPI.TOTL.ZG?format=json&date=2019:2026&per_page=100` | Anual; último ano 2025 |
| `inflacao-multi.json` | `country/BR;US;GB;JP;CH;CA;AU;CN;MX;AR;DE;FR;IT;ES;NL/indicator/FP.CPI.TOTL.ZG?format=json&mrnev=1&per_page=100` | US e AR com último dado em 2024 |
| `pib-multi.json` | `country/DEU;FRA;ITA;ESP;NLD/indicator/NY.GDP.MKTP.CD?format=json&mrnev=1` | Pesos da média do EU |
| `pv-est-br.json` | `country/BR/indicator/GOV_WGI_PV.EST?format=json&date=2019:2026` | **O código `PV.EST` não existe mais**; o atual é `GOV_WGI_PV.EST` |
| `pv-est-multi.json` | idem, 15 países, `mrnev=1` | Último ano 2025 para todos |
| `vazio.json` | `FP.CPI.TOTL.ZG?date=2030` | `[{"page":0,...,"total":0}, null]` com HTTP 200 |
| `pais-invalido.json` | `country/ZZ/...` | HTTP 200 com `[{"message":[{"id":"120",...}]}]`: erro vem no corpo, não no status |
| `erro-parametro-invalido.json` | `country/BR/indicator/PV.EST?source=3` | Mesmo formato de erro (código antigo) |
| `malformado.json` | sintético | JSON truncado |

## FMI (`fmi/`)

O antigo `dataservices.imf.org` (SDMX_JSON.svc) **não responde mais** (falha de conexão). O portal novo expõe SDMX 2.1 e 3.0 em `api.imf.org/external/sdmx/`.

| Arquivo | Requisição | Observação |
|---|---|---|
| `cpi-inflacao-anual-ar-br.json` | `api.imf.org/external/sdmx/3.0/data/dataflow/IMF.STA/CPI/+/ARG+BRA.CPI._T.YOY_PCH_PA_PT.A?startPeriod=2022` (`Accept: application/json`) | Valores como **string**; `startPeriod` foi ignorado e a ordem das observações não é cronológica, então o conector precisa ler a dimensão `TIME_PERIOD` e ordenar |
| `cpi-vazio.json` | idem, país `ZZZ` | HTTP 200 com `dataSets` sem `series` |
| `mfs-ir-juros-politica-mex-chn.json` | `.../IMF.STA/MFS_IR/+/MEX+CHN.MFS166_RT_PT_A_PT.M?lastNObservations=3` | Taxa de política monetária mensal; só MEX tem a série (CHN não aparece) |
| `malformado.json` | sintético | JSON truncado |
