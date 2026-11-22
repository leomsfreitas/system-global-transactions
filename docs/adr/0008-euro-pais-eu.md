# 0008: Euro e o país agregado EU

## Contexto
`moeda.pais_id` é FK obrigatória, mas o euro pertence a vários países (seção 5.3).

## Decisão
- Registro `pais` com `codigo_iso = 'EU'` (código reservado ISO 3166 para a União Europeia), `regiao = 'Europa'`, `codigo_iso3` nulo.
- Membros configuráveis em `ajuste-geopolitico.agregados.EU` (padrão `[DE, FR, IT, ES, NL]`, os maiores PIBs da zona do euro, presentes no seed V4).
- O índice do EU é a média dos índices dos membros ponderada pelo PIB (`NY.GDP.MKTP.CD`); se faltar PIB de algum membro, média simples dos disponíveis. Fator pela mesma fórmula do ADR 0005.
- Com os dados reais de 2025: índice 29,04, fator 0,9855 (confirmado pelo teste `JobsIntegracaoTest`).

## Alternativas
- Usar a Alemanha como "país do euro": simplifica, mas distorce.
- Tornar `pais_id` opcional: quebra a FK e a estratégia por país.

## Consequências
Adicionar membros é configuração e seed. Risco R14 (poucos membros) aceito.
