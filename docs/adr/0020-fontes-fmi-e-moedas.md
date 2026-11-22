# 0020: FMI pelo SDMX 3.0 e lista final de moedas

## Contexto
A AT01 testou as fontes de verdade (`docs/01-iniciacao/escopo-fontes.md`).

## Decisão
- FMI: o portal antigo (`dataservices.imf.org`) não responde; usar `api.imf.org/external/sdmx/3.0` com fluxos `CPI` (inflação anual) e `MFS_IR` (`MFS166_RT_PT_A_PT`, política monetária mensal). Fonte secundária: o conector ordena observações pelo período (vêm fora de ordem) e filtra a janela no cliente (o `startPeriod` é ignorado).
- SGS do BCB sempre com `dataInicial/dataFinal` (o atalho `/ultimos/N` devolveu 304 sem corpo); série 432 com datas futuras descartadas.
- Moedas: BRL, USD, EUR, GBP, JPY, CHF, CAD, AUD, CNY, MXN. **ARS excluída** (PTAX não publica; BCE encerrou em 30/10/2020); risco R02.

## Consequências
Incluir DKK, NOK ou SEK (publicadas por BCB e BCE) é só seed.
