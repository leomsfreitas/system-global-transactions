# AT15: Relatório final

## Planejado × realizado
O cronograma (LB-1, 59 dias relativos) foi executado como entrega única, a pedido do patrocinador; não houve controle por datas.

| Atividade | Entrega | Situação |
|---|---|---|
| AT01 | `docs/01-iniciacao/escopo-fontes.md`, fixtures reais | Concluída |
| AT02 | `docs/01-iniciacao/cronograma.md` | Concluída (dias relativos) |
| AT03 | `docs/01-iniciacao/registro-riscos.md` | Concluída |
| AT04 | diagramas em `docs/02-arquitetura`, ADRs 0001–0020, Terraform | Concluída |
| AT05 | migrations V1–V5, modelo ER, testes de partição e restrições | Concluída |
| AT06 | `api-spec/openapi.yaml` (Spectral limpo), 6 endpoints | Concluída |
| AT07 | conectores BCB, BCE, Banco Mundial, FMI | Concluída |
| AT08 | motor, estratégias, taxa cruzada, índice, auditoria | Concluída |
| AT09 | cache Redis, worker com 5 jobs, ambiente local | Concluída |
| AT10 | 168 testes, cobertura 86,7% (domínio) e 90,1% (conectores) | Concluída |
| AT11 | carga de 500 req/s aprovada | Parcial: estresse e soak não executados |
| AT12 | auditoria; críticas/altas corrigidas | Concluída |
| AT13 | workflows de CD, Terraform validado, checklist de go-live | Parcial: sem apply nem deploy (sem conta AWS) |
| AT14 | guia do desenvolvedor e workflow do GitHub Pages | Concluída (publicação depende do repositório no GitHub) |
| AT15 | este relatório e lições aprendidas | Concluída |

## Desvios e motivos
- **Fontes:** `PV.EST` foi trocado por `GOV_WGI_PV.EST`; o FMI passou a ser consultado pelo SDMX 3.0; ARS ficou fora (sem fonte).
- **Versões:** Spring Boot 4.1.2 em vez de 4.1.3/4.2.0, pela política de 14 dias (ADR 0002).
- **Modelo de dados:** colunas novas (`casas_decimais`, `codigo_iso3`, `codigo`, auditoria) e a migration V5 de papéis.
- **Testes:** sem estresse nem soak, por decisão do patrocinador.
- **AWS:** nenhum recurso criado; `terraform plan` real pendente de credenciais.

## Dívidas técnicas
- Rotação automática das senhas do banco (hoje feita por runbook).
- Lock distribuído contra cache stampede (hoje só por instância).
- Atualizar o Spring Boot para 4.2.x após a janela de 14 dias.
- TLS entre o VPC Link e o ALB interno.

## Próximos passos
Seguir o `docs/04-testes/checklist-go-live.md`: criar o bucket do state, rodar o apply, migrar, configurar as chaves e os environments do GitHub.
