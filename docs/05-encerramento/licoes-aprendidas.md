# AT15: Lições aprendidas

## O que funcionou
- **Testar as fontes antes de codificar.** Três problemas apareceram na AT01, não em produção: o indicador `PV.EST` virou `GOV_WGI_PV.EST`, o portal antigo do FMI saiu do ar e não há fonte para ARS.
- **Gravar respostas reais como fixtures.** Os conectores passaram nos testes de primeira contra o WireMock, porque os casos estranhos (vazio com 200, corpo vazio, erro no corpo com 200, 304 sem corpo) já estavam gravados.
- **Motor puro e resolvedor de taxa sem Spring.** Testes rápidos e exaustivos (exemplo canônico, propriedade com 5.000 casos).
- **Teste de contrato contra a spec.** Pegou um cabeçalho fora do RFC 3339 (`OffsetDateTime.toString()` omite os segundos) e erros 500 que deviam ser 400.
- **ADR para cada lacuna.** As decisões ficaram rastreáveis e trocáveis por configuração.

## O que não funcionou ou custou mais que o previsto
- **Spring Boot 4 recém-lançado:** starters modulares, Jackson 3 com outro pacote e bibliotecas renomeadas (`swagger-request-validator` → `openapi-request-validator`) exigiram investigação.
- **Arquivo de configuração no lugar errado:** o `application.yml` da API foi criado em `src/main/java/resources` e os testes passaram assim mesmo, porque os padrões cobriam o caso. Só apareceu quando o Swagger UI quebrou.
- **Isolamento dos testes:** testes que compartilham banco e cache precisaram de pares de moedas exclusivos.
- **Imagem base desatualizada:** 1 vulnerabilidade crítica e 10 altas vieram da imagem Alpine, não do nosso código.
- **Escopo:** a especificação pedia nível de produção; para um trabalho acadêmico, parte desse esforço (soak, varreduras extras) foi além do necessário.

## Recomendações
1. Testar contratos de terceiros de verdade logo no início e versionar as respostas.
2. Fixar versões exatas, com idade mínima (14 dias), para dependências e imagens.
3. Rodar a varredura de imagem no CI desde o primeiro Dockerfile.
4. Calibrar a profundidade das entregas ao objetivo do projeto antes de começar.
