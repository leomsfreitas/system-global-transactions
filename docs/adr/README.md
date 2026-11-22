# Registros de decisão de arquitetura (ADR)

Formato: contexto, decisão, alternativas, consequências. Toda lacuna da especificação preenchida pelo time vira um ADR. Mudar uma decisão marcada como "configurável" exige só configuração ou uma classe de estratégia.

| Nº | Título | Seção da spec | Status |
|---|---|---|---|
| [0001](0001-stack-spring-boot.md) | Stack: Java 21, Spring Boot 4.1 e Maven multi-módulo | 4 | Aceito |
| [0002](0002-politica-de-versoes.md) | Política de versões de dependências e imagens (14 dias) | 4, 6.7 | Aceito |
| [0003](0003-mapeamento-manual.md) | Mapeamento manual entre domínio e DTO | 4 | Aceito |
| [0004](0004-timestamptz.md) | TIMESTAMPTZ em vez de TIMESTAMP | 6.2 | Aceito |
| [0005](0005-formula-indice-geopolitico.md) | Fórmula do índice de risco e do fator de ajuste | 5.1 | Aceito (configurável) |
| [0006](0006-pais-que-determina-o-fator.md) | País que determina o fator: estratégia selecionável | 5.2 | Aceito (configurável) |
| [0007](0007-contract-first.md) | Contract-first: spec manual como fonte da verdade | 4, 5.10, 6.6 | Aceito |
| [0008](0008-euro-pais-eu.md) | Euro e o país agregado EU | 5.3 | Aceito (configurável) |
| [0009](0009-taxa-cruzada.md) | Pares gravados e taxa cruzada | 5.4 | Aceito |
| [0010](0010-arredondamento.md) | Arredondamento e casas decimais por moeda | 5.5 | Aceito |
| [0011](0011-frescor-e-agendamento.md) | Frescor dos dados e agendamento das fontes | 5.6 | Aceito (configurável) |
| [0012](0012-particionamento-cotacao.md) | Particionamento mensal de cotação e PK composta | 5.9 | Aceito |
| [0013](0013-escrita-idempotente-e-papeis.md) | Escrita idempotente, migrations como tarefa e papéis do banco | 6.2, 6.4 | Aceito |
| [0014](0014-auditoria-conversao.md) | Auditoria de conversão com fonte e prefixo do hash da chave | 5.11 | Aceito |
| [0015](0015-endpoint-indicadores.md) | Endpoint de indicadores devolve o valor mais recente por tipo | 3.4 | Aceito |
| [0016](0016-chave-api-e-limite.md) | Chave de API e limite de requisições na aplicação | 5.7 | Aceito |
| [0017](0017-api-gateway-vpc-link.md) | API Gateway REST com VPC Link v2 e ALB interno | 5.8 | Aceito |
| [0018](0018-cache-redis.md) | Cache Redis: chaves, TTL, fallback e stampede | 6.4 | Aceito (configurável) |
| [0019](0019-metas-de-desempenho.md) | Metas de desempenho | 5.12 | Aceito (ajustável) |
| [0020](0020-fontes-fmi-e-moedas.md) | FMI pelo SDMX 3.0 e lista final de moedas | 6.5, AT01 | Aceito |
