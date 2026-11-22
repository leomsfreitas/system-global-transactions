# AT03: Registro de riscos

| Campo | Valor |
|---|---|
| Atividade | AT03, Identificar e analisar riscos geopolíticos e de integração com APIs de terceiros |
| Base | Levantamento real das fontes na AT01 (`escopo-fontes.md`) |
| Revisão | Atualizado ao fim de cada fase; última revisão em 22/11/2026 |

## Escalas

- **Probabilidade (P)**: 1 rara, 2 improvável, 3 possível, 4 provável, 5 quase certa.
- **Impacto (I)**: 1 desprezível, 2 baixo, 3 moderado, 4 alto, 5 crítico.
- **Exposição** = P × I. Alta ≥ 12, média 6 a 11, baixa ≤ 5.
- **Respostas**: evitar, mitigar, transferir, aceitar.

Donos: **GP** gerente do projeto (YouPay), **ENG** engenharia (implementação), **SEG** segurança, **FIN** financeiro/negócio YouPay.

## Matriz

| ID | Risco | Categoria | P | I | Exp. | Resposta | Ações | Dono | Gatilho / indicador |
|---|---|---|---|---|---|---|---|---|---|
| R01 | Fonte externa fora do ar ou mudando contrato (formato, URL, código de indicador) | Integração | 4 | 4 | **16** | Mitigar | Circuit breaker e retry por fonte; dado existente nunca é apagado; regra de frescor devolve 503 explícito; testes com fixtures reais; alarme de falha do worker; `fonte_dados.ativa` desliga a fonte sem deploy. **Já ocorreu** no levantamento: `PV.EST` virou `GOV_WGI_PV.EST` e o portal antigo do FMI saiu do ar | ENG | Métrica de falhas por fonte > 0 por 3 execuções seguidas |
| R02 | Moeda sem cotação em nenhuma fonte | Escopo | 5 | 3 | **15** | Evitar | ARS excluída da lista inicial (PTAX não publica; BCE encerrou em 2020). CNY e MXN dependem só do BCE: se o BCE parar, só restam taxas cruzadas antigas, e a regra de frescor bloqueia. Avaliar fonte comercial se a YouPay precisar de ARS | GP | Pedido de cliente por moeda fora da lista |
| R03 | Defasagem dos indicadores anuais (inflação e WGI só com o ano anterior; US e AR ainda em 2024 no Banco Mundial) | Dados | 5 | 3 | **15** | Aceitar e mitigar | Combinar Banco Mundial e FMI (vence o período mais recente); expor `dataReferencia` no índice; documentar no guia que o índice é estrutural, não de curto prazo | ENG | Indicador com mais de 18 meses |
| R04 | Sanções ou controle cambial alterando cotações (países com múltiplas taxas oficiais, ex.: Argentina; câmbio administrado, ex.: China) | Geopolítico | 3 | 4 | **12** | Mitigar | Usar só taxas de referência oficiais de bancos centrais; para CNY a taxa do BCE reflete o mercado onshore, sem distinção CNH; documentar que a API não cobre câmbio paralelo; lista de moedas controlada pelo seed e revisada pelo GP | GP | Notícia de sanção ou desdobramento cambial em país do escopo |
| R05 | Viés e contestação do índice de risco (escolha de PV.EST, pesos, inclusão de inflação) | Negócio / reputação | 3 | 4 | **12** | Mitigar | Fórmula pública no guia do desenvolvedor; pesos em configuração; ADR com justificativa; resposta da conversão traz `fatorGeopolitico` separado; flag `aplicarAjusteGeopolitico=false`; revisão anual dos pesos | GP | Reclamação de cliente ou parceiro sobre o fator |
| R06 | Limite de requisições das fontes (nenhuma publica limite formal; bloqueio por uso abusivo é possível) | Integração | 2 | 3 | 6 | Mitigar | Consultas agrupadas (BCE com todas as moedas numa chamada, Banco Mundial com vários países); agendamento conservador; respeito a `cache-control`; backoff exponencial; user agent genérico do projeto | ENG | HTTP 429 ou 403 de alguma fonte |
| R07 | Custo de nuvem acima do previsto (RDS Multi-AZ, ElastiCache com réplica, NAT Gateway em 2 AZs, WAF) | Financeiro | 3 | 3 | 9 | Mitigar | Ambiente dev com instâncias menores e sem Multi-AZ; tags de custo em todos os recursos; AWS Budgets com alarme; estimativa antes do primeiro apply; um NAT só em dev | FIN | Gasto mensal > 80% do orçamento |
| R08 | Vazamento de chave de API | Segurança | 3 | 4 | **12** | Mitigar | Chaves só no API Gateway e no Secrets Manager; a aplicação guarda só hash SHA-256; logs nunca registram a chave (só prefixo de 8 caracteres do hash); Gitleaks no CI; rotação documentada; usage plan limita o estrago | SEG | Uso anômalo de uma chave; achado do Gitleaks |
| R09 | Comportamentos não documentados das fontes (vazio com HTTP 200, corpo vazio sem JSON, erro no corpo com status 200, 304 sem corpo no SGS, datas futuras na série 432, observações fora de ordem no FMI) | Integração | 5 | 3 | **15** | Mitigar | Cada caso tem fixture real e teste dedicado; validação de data plausível; parsers tolerantes a vazio e estritos com valor | ENG | Teste de conector falhando com fixture nova |
| R10 | Fuso e calendário (PTAX sem fuso explícito; feriados brasileiros e TARGET diferentes) | Dados | 3 | 3 | 9 | Mitigar | `TIMESTAMPTZ` em todas as colunas; conversão explícita de `America/Sao_Paulo` e `Europe/Berlin`; regra de frescor de 4 dias cobre feriados prolongados; taxa cruzada só com cotações da mesma data | ENG | Cotação "do futuro" ou divergência de data entre fontes |
| R11 | Uso indevido das taxas de referência do BCE para liquidação (o BCE avisa que são informativas) | Conformidade | 2 | 4 | 8 | Transferir / aceitar | Aviso no guia do desenvolvedor e nos termos da API; a YouPay decide se usa como cotação de liquidação | GP | Contrato de cliente usando a API para liquidação |
| R12 | Desempenho abaixo da meta (500 req/s, p95 < 300 ms na conversão) por causa da auditoria síncrona e da taxa cruzada | Técnico | 3 | 3 | 9 | Mitigar | Cache Redis com proteção contra stampede; índices compostos; medir na AT11; plano B de auditoria assíncrona documentado em ADR | ENG | p95 acima da meta no teste de carga |
| R13 | Equipe reduzida e entrega única (o patrocinador pediu todas as fases de uma vez) reduz pontos de revisão | Gestão | 3 | 3 | 9 | Mitigar | Commits pequenos por atividade; ADR para cada lacuna; relatório por fase no fim; CI obrigatório | GP | Revisão do GP encontrando desvio de escopo |
| R14 | Dependência do índice EU de poucos países (média ponderada só dos países da zona do euro presentes no banco: DE, FR, IT, ES, NL) | Dados | 3 | 2 | 6 | Aceitar | Documentado no ADR do EU; adicionar países é só seed | ENG | Mudança relevante num país da zona do euro fora da lista |

## Mapa de calor (exposição)

| P \ I | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| **5** | | | R02, R03, R09 | | |
| **4** | | | | R01 | |
| **3** | | R14 | R07, R10, R12, R13 | R04, R05, R08 | |
| **2** | | | R06 | R11 | |
| **1** | | | | | |

## Riscos que já se materializaram no levantamento

| Risco | Fato | Tratamento |
|---|---|---|
| R01 | `PV.EST` com `source=3` devolve erro 120; indicador atual é `GOV_WGI_PV.EST` | Conector usa o código novo, configurável |
| R01 | `dataservices.imf.org` não responde | Conector usa SDMX 3.0 em `api.imf.org` |
| R02 | ARS sem fonte | Fora da lista inicial |
| R09 | IPCA (SGS 433) com `/ultimos/N` devolve 304 sem corpo | Conector usa `dataInicial/dataFinal` |
