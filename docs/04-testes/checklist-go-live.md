# AT13: Checklist de go-live

O apply e o deploy em produção **não foram executados**: dependem de conta AWS e de autorização explícita. Tudo abaixo está pronto para ser feito por um humano.

## Antes do primeiro deploy
- [ ] `infra/bootstrap-estado`: `terraform apply` com state local (cria o bucket do state).
- [ ] Preencher `infra/ambientes/prod/terraform.tfvars` (domínio, certificado, clientes, e-mails); `make plan AMBIENTE=prod` e revisar o plano.
- [ ] Apply pelo workflow `Infra` (environment `infra-prod`, com aprovação).
- [ ] Rodar a tarefa de migração uma vez e `infra/scripts/bootstrap-usuarios.sql` (senhas do Secrets Manager).
- [ ] Gravar no segredo `transacoes-prod/api/chaves-sha256` os hashes das chaves do Gateway; criar `transacoes-prod/api/chave-smoke`.
- [ ] Configurar no GitHub os environments `dev`, `prod` (reviewers obrigatórios), `infra-dev`, `infra-prod`, `github-pages`, com as variáveis `AWS_ROLE_ARN`, `AWS_REGION`, `NOME`, `URL_API`, `SUBNETS`, `SG_WORKER`.

## Go-live
- [ ] **DNS:** registro do domínio apontando para o `dominio_regional` do output do Terraform.
- [ ] **Certificado:** ACM validado na região do Gateway.
- [ ] **Chaves dos primeiros clientes:** geradas pelo Gateway (`clientes` no tfvars), entregues por canal seguro.
- [ ] **Alarmes testados:** inscrição no SNS confirmada; forçar um alarme (`aws cloudwatch set-alarm-state`) e conferir a notificação.
- [ ] **Rollback ensaiado:** fazer deploy de uma imagem com readiness quebrada em dev e confirmar o rollback do circuit breaker.
- [ ] **Smoke test:** o CD roda `.github/scripts/smoke-test.sh` nos 6 endpoints após cada deploy.

## Estado do código
- Workflows `ci.yml`, `cd.yml`, `deploy-ambiente.yml`, `infra.yml`, `seguranca.yml`, `docs.yml` validados com `actionlint`.
- Terraform: `validate`, `tflint` e Checkov sem falhas. O `terraform plan` real fica pendente de credenciais.
