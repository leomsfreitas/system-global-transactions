# 0005: Fórmula do índice de risco e do fator de ajuste

## Contexto
O documento define `indice_risco` (0 a 100) e `fator_ajuste`, mas não a fórmula nem a fonte do risco político (lacuna crítica, seção 5.1).

## Decisão
Implementada a decisão padrão da seção 5.1 em `CalculadoraIndiceGeopolitico` (classe pura):

```
risco_politico  = clamp((2,5 - PV) / 5 × 100, 0, 100)
risco_economico = clamp(inflacao_% / LIMITE_INFLACAO × 100, 0, 100)
indice_risco    = PESO_POLITICO × risco_politico + PESO_ECONOMICO × risco_economico   (2 casas, HALF_EVEN)
fator_ajuste    = 1 - indice_risco / 100 × AJUSTE_MAXIMO                            (4 casas, HALF_EVEN)
```

| Parâmetro (`ajuste-geopolitico.*`) | Padrão |
|---|---|
| `peso-politico` / `peso-economico` | 0,6 / 0,4 (soma validada = 1) |
| `limite-inflacao` | 50 |
| `ajuste-maximo` | 0,05 |
| `estabilidade-minima` / `estabilidade-maxima` | -2,5 / 2,5 |

- **PV**: Political Stability and Absence of Violence/Terrorism (WGI). Na AT01 o código `PV.EST` com `source=3` passou a devolver erro; o indicador atual é **`GOV_WGI_PV.EST`** (configurável em `conectores.indicador-estabilidade-politica`).
- **Inflação**: valor mais recente entre as fontes (BCB 13522 para BR, Banco Mundial `FP.CPI.TOTL.ZG`, FMI `CPI`).
- O PV bruto é guardado em `indicador_economico` com `tipo = 'ESTABILIDADE_POLITICA'` (o CHECK da V1 inclui esse valor). O endpoint `/indicadores` continua expondo só `INFLACAO | JUROS | PIB`: contrato preservado.
- O worker (`CalculoIndiceGeopoliticoJob`) grava nova linha só quando há período novo: `data_referencia` = período mais recente entre os insumos; unicidade `(pais, data_referencia, fonte)`.

## Alternativas
- Índices comerciais de risco-país (pagos, licença restritiva).
- Só o componente político: ignora hiperinflação (ex.: Argentina).

## Consequências
- Com dados de 2025, os fatores do escopo ficam entre 0,9798 (MX) e 0,9917 (CH). O componente político domina porque a escala do WGI é centrada em zero; ver `docs/01-iniciacao/escopo-fontes.md` seção 3.
- Defasagem anual dos insumos (risco R03) e possível contestação do índice (R05) estão no registro de riscos.
