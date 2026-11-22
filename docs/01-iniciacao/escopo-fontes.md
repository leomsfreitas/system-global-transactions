# AT01: Escopo das fontes de dados externas e das variáveis geopolíticas

| Campo | Valor |
|---|---|
| Projeto | Sistema de transações que se adapta a questões geopolíticas globais |
| Patrocinador | YouPay (gerente do projeto: Gabriel Resende) |
| Atividade | AT01, Mapear escopo inicial dos bancos de dados externos e das variáveis geopolíticas |
| Data do levantamento | 22/11/2026 |
| Evidências | Respostas reais gravadas em `modulos/conectores/src/test/resources/fixtures/` (ver o README da pasta) |

Todas as URLs abaixo foram chamadas com `curl` na data do levantamento. Nada aqui foi copiado de documentação sem teste.

## 1. Resumo

| Fonte | Uso no projeto | Endpoint confirmado | Formato | Status |
|---|---|---|---|---|
| BCB, PTAX (Olinda) | Cotações X→BRL | `olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/` | OData JSON | OK |
| BCB, SGS | Selic e IPCA do Brasil | `api.bcb.gov.br/dados/serie/bcdata.sgs.{codigo}/dados` | JSON | OK, com ressalvas (seção 2.2) |
| BCE, ECB Data Portal | Cotações EUR→X | `data-api.ecb.europa.eu/service/data/EXR/` | SDMX-JSON ou CSV | OK |
| Banco Mundial, API v2 | Inflação, PIB, estabilidade política (WGI) | `api.worldbank.org/v2/country/{pais}/indicator/{indicador}` | JSON | OK, código WGI mudou |
| FMI, SDMX 3.0 | Inflação e juros de reserva (fallback) | `api.imf.org/external/sdmx/3.0/data/dataflow/IMF.STA/{fluxo}/+/{chave}` | SDMX-JSON | OK, portal antigo desativado |

## 2. Fontes em detalhe

### 2.1 Banco Central do Brasil, PTAX (API Olinda)

- **Endpoints usados**
  - Lista de moedas: `GET .../odata/Moedas?$format=json`
  - Cotação do dia: `GET .../odata/CotacaoMoedaDia(moeda=@moeda,dataCotacao=@dataCotacao)?@moeda='EUR'&@dataCotacao='11-19-2026'&$format=json`
  - Período: `GET .../odata/CotacaoMoedaPeriodo(moeda=@moeda,dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)?@moeda='USD'&@dataInicial='11-16-2026'&@dataFinalCotacao='11-20-2026'&$format=json`
- **Formato**: `{"value":[{"paridadeCompra","paridadeVenda","cotacaoCompra","cotacaoVenda","dataHoraCotacao","tipoBoletim"}]}`. Datas no parâmetro em `MM-DD-AAAA`; `dataHoraCotacao` em `AAAA-MM-DD HH:MM:SS.ffffff`, horário de Brasília, sem fuso explícito.
- **Valor usado**: `cotacaoVenda` (convenção de mercado para PTAX). Grava o par `X→BRL`.
- **Frequência**: dias úteis brasileiros, 4 boletins (Abertura por volta de 10h, Intermediários às 11h e 12h, Fechamento por volta de 13h) mais o `Fechamento PTAX` do dia. Nada em fins de semana e feriados (20/11/2026 veio vazio, é feriado nacional).
- **Moedas publicadas**: AUD, CAD, CHF, DKK, EUR, GBP, JPY, NOK, SEK, USD. **Não publica** CNY, ARS, MXN (testado, `value: []`).
- **Comportamentos a tratar no conector**
  - Ausência de dados, data em formato errado e moeda inexistente devolvem **HTTP 200 com `value: []`**, nunca erro. O conector não pode tratar vazio como falha.
  - Fuso: converter `dataHoraCotacao` de `America/Sao_Paulo` para `TIMESTAMPTZ`.
- **Limites de uso**: nenhum limite publicado. Respostas trazem `cache-control: max-age=900`. Usar intervalos de consulta pequenos e não repetir chamadas dentro do mesmo boletim.
- **Licença/termos**: dados públicos do Portal de Dados Abertos do BCB, uso livre com citação da fonte.

### 2.2 Banco Central do Brasil, SGS

- **Endpoint**: `GET api.bcb.gov.br/dados/serie/bcdata.sgs.{codigo}/dados?formato=json&dataInicial=DD/MM/AAAA&dataFinal=DD/MM/AAAA`
- **Formato**: `[{"data":"DD/MM/AAAA","valor":"texto decimal"}]`. Valor vem como string.
- **Séries confirmadas**

| Código | Série | Periodicidade | Uso |
|---|---|---|---|
| 432 | Meta Selic (% a.a.) | diária | `JUROS` do BR |
| 11 | Selic diária (% a.d.) | diária | referência, não usada no índice |
| 433 | IPCA, variação mensal (%) | mensal | `INFLACAO` mensal do BR |
| 13522 | IPCA acumulado 12 meses (%) | mensal | `INFLACAO` anualizada do BR |

- **Comportamentos a tratar**
  - A série 432 **traz datas futuras** (a meta vale até a próxima reunião do Copom; em 22/11 veio até 09/12/2026). O conector descarta pontos com data posterior a hoje para não violar a validação de "data plausível".
  - Com o atalho `/dados/ultimos/N`, a série 433 devolveu **HTTP 304 sem corpo** de forma repetida, mesmo sem cabeçalho condicional. Usar sempre `dataInicial/dataFinal`.
  - Série inexistente: HTTP 404 com corpo texto `A serie nao existe!`.
  - O BCB limita consultas de séries diárias a janelas de até 10 anos por requisição; as janelas do worker são de dias, então não há impacto.
- **Licença/termos**: os mesmos do PTAX.

### 2.3 Banco Central Europeu, ECB Data Portal

- **Endpoint**: `GET data-api.ecb.europa.eu/service/data/EXR/D.{MOEDAS}.EUR.SP00.A?startPeriod=AAAA-MM-DD&endPeriod=AAAA-MM-DD&format=jsondata`, com moedas separadas por `+`.
- **Formato**: SDMX-JSON. Valores em `dataSets[0].series["0:i:0:0:0"].observations[j][0]`; o índice `i` aponta para `structure.dimensions.series[CURRENCY].values[i]` e o `j` para `structure.dimensions.observation[0].values[j]` (data). CSV disponível com `format=csvdata`.
- **Semântica**: unidades da moeda X por 1 EUR (taxa de referência das 14h15 CET). Grava o par `EUR→X`.
- **Frequência**: uma vez por dia útil TARGET, publicada por volta das 16h CET.
- **Moedas confirmadas** (de interesse): AUD, BRL, CAD, CHF, CNY, GBP, JPY, MXN, USD. **ARS encerrada**: última observação em 30/10/2020.
- **Comportamentos a tratar**
  - Período sem dados (fim de semana): **HTTP 200 com corpo vazio (0 bytes)**, não JSON. O conector trata como "sem dados".
  - Série inexistente: HTTP 404 com `{"detail":"No Series was returned..."}`. Chave mal formada: HTTP 400.
- **Limites de uso**: não publicados; uso justo. Uma chamada diária com todas as moedas resolve.
- **Licença/termos**: reuso permitido com citação da fonte (aviso de copyright do BCE: "Source: ECB statistics"). As taxas de referência são informativas, o BCE não as recomenda para transações; isso vai para o guia do desenvolvedor.

### 2.4 Banco Mundial, API v2

- **Endpoint**: `GET api.worldbank.org/v2/country/{ISO2;ISO2;...}/indicator/{codigo}?format=json&mrnev=1&per_page=100`
- **Formato**: array de dois elementos, `[{metadados de paginação}, [{indicator, country, countryiso3code, date, value}]]`.
- **Indicadores confirmados**

| Código | Indicador | Periodicidade | Último ano disponível (22/11/2026) |
|---|---|---|---|
| `FP.CPI.TOTL.ZG` | Inflação, preços ao consumidor (% anual) | anual | 2025 para a maioria; **2024 para US e AR** |
| `NY.GDP.MKTP.CD` | PIB (US$ correntes) | anual | 2025 |
| `GOV_WGI_PV.EST` | Political Stability and Absence of Violence/Terrorism, estimativa (≈ -2,5 a +2,5) | anual | 2025 |

- **Mudança em relação à especificação**: o código `PV.EST` (com `source=3`) **não existe mais** e devolve erro 120. O indicador atual é `GOV_WGI_PV.EST`, sem parâmetro `source`. Registrado em ADR.
- **Comportamentos a tratar**
  - Erros vêm com **HTTP 200** e corpo `[{"message":[{"id":"120","key":"Invalid value",...}]}]`. O conector inspeciona o corpo, não só o status.
  - Sem dados: `[{"page":0,...,"total":0}, null]`.
  - Valores nulos aparecem para anos sem publicação; `mrnev=1` (mais recente não vazio) evita isso.
- **Limites de uso**: sem chave e sem limite publicado; recomenda-se paginação (`per_page`).
- **Licença/termos**: Creative Commons Attribution 4.0 (CC BY 4.0), citar "World Bank, World Development Indicators / Worldwide Governance Indicators".

### 2.5 Fundo Monetário Internacional, SDMX 3.0

- **Portal antigo**: `dataservices.imf.org/REST/SDMX_JSON.svc` **não responde** (falha de conexão). Não usar.
- **Endpoint novo**: `GET api.imf.org/external/sdmx/3.0/data/dataflow/IMF.STA/{fluxo}/+/{chave}` com `Accept: application/json`.
- **Fluxos confirmados**

| Fluxo | Chave | Conteúdo |
|---|---|---|
| `CPI` | `{ISO3}.CPI._T.YOY_PCH_PA_PT.A` | Inflação anual (% variação) |
| `MFS_IR` | `{ISO3}.MFS166_RT_PT_A_PT.M` | Taxa de política monetária mensal (só para alguns países: MEX sim, CHN não) |

- **Comportamentos a tratar**
  - Valores como **string**. Observações **fora de ordem cronológica** e `startPeriod` ignorado no teste: o conector lê a dimensão `TIME_PERIOD` e ordena.
  - Sem dados: HTTP 200 com `dataSets` sem `series`.
  - Países em ISO 3166 alfa-3 (o banco usa alfa-2; o conector converte).
- **Papel no projeto**: fonte secundária. O Banco Mundial cobre inflação de todos os países do escopo; o FMI preenche lacunas (ex.: US e AR, que no Banco Mundial ainda estão em 2024; para AR o fluxo `CPI` do FMI já traz 2025) e juros dos países fora do BCB. Regra: vence o período mais recente; empate fica com o Banco Mundial.
- **Limites de uso**: não publicados.
- **Licença/termos**: dados do FMI são gratuitos; reuso permitido com citação ("Source: International Monetary Fund"), conforme os termos de uso do site do FMI. Revisar na AT12.

## 3. Variáveis geopolíticas e econômicas (seção 5.1 da especificação)

| Variável | Fonte primária | Fonte secundária | Escala | Normalização | Peso padrão |
|---|---|---|---|---|---|
| Estabilidade política (`GOV_WGI_PV.EST`) | Banco Mundial (WGI) | nenhuma | ≈ -2,5 a +2,5 | `risco_politico = clamp((2,5 - PV) / 5 × 100, 0, 100)` | 0,6 |
| Inflação anual (%) | Banco Mundial `FP.CPI.TOTL.ZG` | FMI `CPI` (período mais novo vence); BCB 13522 para BR | % | `risco_economico = clamp(inflacao / 50 × 100, 0, 100)` | 0,4 |
| PIB (US$) | Banco Mundial `NY.GDP.MKTP.CD` | nenhuma | US$ | pesos da média do EU (seção 5.3) | n/a |
| Juros | BCB 432 (BR) | FMI `MFS_IR` | % a.a. | só exposto em `/indicadores`, não entra no índice | n/a |

**Índice**: `indice_risco = 0,6 × risco_politico + 0,4 × risco_economico` (2 casas). **Fator**: `fator_ajuste = 1 - indice_risco / 100 × 0,05` (4 casas).

### Simulação com os dados reais de 22/11/2026

| País | PV.EST | Inflação % | Risco político | Risco econômico | Índice | Fator |
|---|---|---|---|---|---|---|
| AU | 0,7806 | 2,87 | 34,39 | 5,75 | 22,93 | 0,9885 |
| BR | -0,3846 | 5,02 | 57,69 | 10,03 | 38,63 | 0,9807 |
| CA | 0,8096 | 2,07 | 33,81 | 4,14 | 21,94 | 0,9890 |
| CH | 1,1191 | 0,15 | 27,62 | 0,31 | 16,69 | 0,9917 |
| CN | 0,0225 | 0,06 | 49,55 | 0,12 | 29,78 | 0,9851 |
| GB | 0,2897 | 3,88 | 44,21 | 7,77 | 29,63 | 0,9852 |
| JP | 1,2586 | 3,17 | 24,83 | 6,35 | 17,43 | 0,9913 |
| MX | -0,6082 | 3,81 | 62,16 | 7,62 | 40,35 | 0,9798 |
| US | -0,3021 | 2,95 (2024) | 56,04 | 5,90 | 35,98 | 0,9820 |
| EU | média ponderada pelo PIB de DE, FR, IT, ES, NL | | | | 29,04 | 0,9855 |

Observações para o gerente do projeto:
- O componente político domina: mesmo países estáveis ficam com risco político entre 25 e 50, porque a escala do WGI é centrada em zero. Na prática o fator fica entre 0,979 e 0,992 para o escopo atual. A fórmula está em configuração; se a YouPay quiser mais contraste, basta mudar pesos ou `AJUSTE_MAXIMO`.
- A Argentina, fora do escopo de moedas, teria risco econômico 100 (inflação de 219,88% em 2024) e fator 0,9660.

## 4. Moedas e países suportados (lista final)

| Moeda | País no banco | Fonte(s) de cotação | Par gravado | Situação |
|---|---|---|---|---|
| BRL | BR, Brasil | BCB (implícito), BCE | EUR→BRL | Incluída |
| USD | US, Estados Unidos | BCB, BCE | USD→BRL, EUR→USD | Incluída |
| EUR | EU, União Europeia (seção 5.3) | BCB | EUR→BRL | Incluída |
| GBP | GB, Reino Unido | BCB, BCE | GBP→BRL, EUR→GBP | Incluída |
| JPY | JP, Japão | BCB, BCE | JPY→BRL, EUR→JPY | Incluída (0 casas decimais) |
| CHF | CH, Suíça | BCB, BCE | CHF→BRL, EUR→CHF | Incluída |
| CAD | CA, Canadá | BCB, BCE | CAD→BRL, EUR→CAD | Incluída |
| AUD | AU, Austrália | BCB, BCE | AUD→BRL, EUR→AUD | Incluída |
| CNY | CN, China | BCE | EUR→CNY | Incluída, só via BCE (cruzada por EUR) |
| MXN | MX, México | BCE | EUR→MXN | Incluída, só via BCE (cruzada por EUR) |
| ARS | AR, Argentina | nenhuma | n/a | **Excluída**: PTAX não publica; BCE encerrou em 30/10/2020. Ver risco R02 |

Países auxiliares (sem moeda própria no escopo, usados só no cálculo do índice do EU): DE, FR, IT, ES, NL.

Moedas publicadas por alguma fonte mas fora do escopo inicial: DKK, NOK, SEK (BCB e BCE). Podem entrar depois só com seed, sem código novo.

## 5. Ajustes à especificação decorrentes deste levantamento

1. `PV.EST` → `GOV_WGI_PV.EST` (Banco Mundial).
2. FMI pelo SDMX 3.0 em `api.imf.org`; portal antigo descartado.
3. ARS fora da lista inicial.
4. Agendamento do BCB: a especificação pede 15 em 15 minutos das 9h às 18h. Os boletins saem entre 10h e 13h30; o padrão será mantido (é configurável), mas a janela útil real é menor.
5. Séries do SGS sempre com `dataInicial/dataFinal`, nunca `/ultimos/N`.
