# AT12: Auditoria de segurança e conformidade

Data: 22/11/2026. Ferramentas: Gitleaks 8.28, Trivy 0.76.2, Checkov 3.4.3, OWASP ZAP 2.16.1 (API scan guiado pela spec).

## Achados

| # | Ferramenta | Achado | Severidade | Situação |
|---|---|---|---|---|
| 1 | Trivy (imagem) | brotli-libs CVE-2026-111497 na imagem base Alpine | Crítica | Corrigido (`apk upgrade` no Dockerfile) |
| 2 | Trivy (imagem) | libexpat, musl, zlib, libfontenc (10 CVEs) | Alta | Corrigido (idem); reescaneamento com 0 HIGH/CRITICAL |
| 3 | Gitleaks | 2 "generic-api-key" | Falso positivo | São hashes SHA-256 de chaves de exemplo públicas; liberados por valor exato em `.gitleaks.toml` |
| 4 | Checkov | 17 falhas iniciais em 306 checagens | Média/baixa | Corrigidas: KMS no EventBridge Scheduler, regra Log4j explícita no WAF, `create_before_destroy` no Gateway, política do CD restrita ao cluster, AZs fixas, política KMS do state. Restantes viraram exceções justificadas em `infra/.checkov.yaml` |
| 5 | Checkov | ALB interno em HTTP | Baixa | Risco aceito: tráfego Gateway → VPC Link → ALB fica dentro da VPC (ADR 0017); TLS ponta a ponta exigiria CA privada |
| 6 | ZAP | 114 regras aprovadas, 0 falhas; avisos só informativos (respostas 4xx a payloads de ataque, `no-store`) | Informativa | Nenhuma ação |
| 7 | Trivy (dependências Maven) | 0 HIGH/CRITICAL | n/a | n/a |

Não há vulnerabilidade alta ou crítica aberta.

## Controles implementados
- HTTPS no API Gateway (ACM, TLS 1.2); TLS obrigatório no RDS (`rds.force_ssl`) e no Redis (in-transit + AUTH).
- RDS e Redis criptografados com KMS; backup do RDS por 7 dias; deletion protection em prod.
- Security groups mínimos: só API e worker acessam RDS e Redis; ALB só aceita o VPC Link; nada público além do Gateway.
- IAM separado por tarefa (execução e tarefa da API, do worker e da migração); cada uma lê só os seus segredos.
- WAF com regras gerenciadas (Common, KnownBadInputs, IP reputation, anonymous IP) e limite por IP.
- Cabeçalhos de segurança (`nosniff`, `DENY`, HSTS, CSP, `no-store`), validação de toda entrada, SQL só parametrizado.
- Chave de API nunca em log: aplicação e auditoria usam só o prefixo do hash; o WAF mascara o cabeçalho `x-api-key`.
- Segredos fora do repositório (`.env.example` sem valores reais; Secrets Manager na AWS); OIDC no GitHub Actions.
- Varreduras no CI (`seguranca.yml`): Gitleaks, Trivy (deps, Dockerfile e imagem), Checkov, ZAP.

## Conformidade

**LGPD.** A API não trata dado pessoal: recebe códigos de moeda e valores, devolve cotações e índices de países. A auditoria guarda valores e o prefixo do hash da chave (identifica a empresa cliente, não uma pessoa). Os logs não registram IP do cliente na aplicação; o IP aparece só nos logs do Gateway e do WAF, com retenção de 90 dias, para segurança. Conclusão: fora do escopo da LGPD, desde que os clientes não enviem dados pessoais em campos livres (não há campos livres no contrato).

**Licenças e termos das fontes.**
| Fonte | Termos | Obrigação |
|---|---|---|
| BCB (PTAX, SGS) | Dados abertos, uso livre | Citar a fonte |
| BCE | Reuso permitido com citação; taxas de referência são só informativas | Citar e avisar no guia que não são para liquidação |
| Banco Mundial | CC BY 4.0 | Atribuição |
| FMI | Uso gratuito com citação | Citar a fonte |

O guia do desenvolvedor traz as atribuições e o aviso do BCE.

**OWASP API Security Top 10 (2023).**
| Risco | Situação |
|---|---|
| API1 Autorização a nível de objeto | Não há objetos por usuário; todos os dados são públicos por chave |
| API2 Autenticação quebrada | Chave validada no Gateway e na aplicação (hash SHA-256, fail-closed) |
| API3 Autorização a nível de propriedade | DTOs explícitos; campos desconhecidos rejeitados (400) |
| API4 Consumo irrestrito de recursos | Usage plan e throttling no Gateway, limite por IP no WAF, Bucket4j local, `valor ≤ 1e12`, histórico ≤ 366 dias |
| API5 Autorização a nível de função | Só leitura e uma escrita (auditoria); nenhuma rota administrativa exposta |
| API6 Acesso irrestrito a fluxos de negócio | Conversão não tem efeito financeiro; limitada por cota |
| API7 SSRF | A API não busca URLs vindas do cliente; o worker só chama URLs fixas de configuração |
| API8 Má configuração de segurança | Cabeçalhos, erro sem stack trace, actuator restrito a health/info/metrics, Checkov |
| API9 Gestão de inventário | Versão no caminho (`/api/v1`), spec única versionada |
| API10 Consumo inseguro de APIs | Respostas das fontes validadas (valor, data, moeda), timeout, circuit breaker |
