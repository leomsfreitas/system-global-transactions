# Atalhos do projeto. Ferramentas externas rodam em Docker (nada instalado no host alem de Java 21 e Docker).
SHELL := /bin/bash
PWD_DOCKER := -v "$(CURDIR)":/w -w /w
TF := docker run --rm $(PWD_DOCKER) -e TF_IN_AUTOMATION=1 hashicorp/terraform:1.17.1
AMBIENTE ?= dev
URL ?= http://host.docker.internal:8080
CHAVE ?= chave-local-de-exemplo

.PHONY: help up down logs ps test verify lint lint-spec lint-java lint-infra lint-actions fmt \
        load-test stress-test soak-test seguranca gitleaks trivy checkov zap plan seed

help: ## Lista os comandos
	@grep -E '^[a-z-]+:.*##' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  %-14s %s\n", $$1, $$2}'

.env:
	cp .env.example .env

up: .env ## Sobe postgres, redis, wiremock, migracao, seed, api e worker
	docker compose up -d --build --wait api
	docker compose up -d worker
	@echo "API em http://localhost:8080 (Swagger UI: /swagger-ui/index.html). Chave local: $(CHAVE)"

down: ## Derruba o ambiente e apaga os volumes
	docker compose down -v --remove-orphans

logs: ## Acompanha os logs da api e do worker
	docker compose logs -f api worker

ps:
	docker compose ps -a

seed: ## Recarrega o seed de exemplo (idempotente)
	docker compose run --rm seed

test: ## Testes unitarios e de integracao (Testcontainers)
	./mvnw -B test

verify: ## Build completo: testes, cobertura (JaCoCo) e Spotless
	./mvnw -B verify

fmt: ## Formata codigo Java e Terraform
	./mvnw -q spotless:apply
	$(TF) fmt -recursive infra

lint: lint-spec lint-java lint-infra lint-actions ## Todos os linters

lint-spec: ## Spectral na spec OpenAPI
	docker run --rm --platform linux/amd64 $(PWD_DOCKER) stoplight/spectral:6.15.0 lint api-spec/openapi.yaml --ruleset .spectral.yaml --fail-severity=warn

lint-java:
	./mvnw -q -B spotless:check

lint-infra: ## terraform fmt/validate e tflint
	$(TF) fmt -check -recursive infra
	for d in infra/ambientes/dev infra/ambientes/prod; do \
	  docker run --rm $(PWD_DOCKER) --entrypoint sh hashicorp/terraform:1.17.1 -c "cd $$d && terraform init -backend=false -input=false >/dev/null && terraform validate" || exit 1; \
	done
	docker run --rm $(PWD_DOCKER) -e TFLINT_PLUGIN_DIR=/w/.tflint.d --entrypoint sh ghcr.io/terraform-linters/tflint:v0.59.1 -c "tflint --init --config=/w/infra/.tflint.hcl >/dev/null && tflint --chdir=infra --recursive --config=/w/infra/.tflint.hcl"

lint-actions: ## actionlint nos workflows
	docker run --rm $(PWD_DOCKER) rhysd/actionlint:1.7.7 -color

plan: ## terraform plan (exige credenciais AWS; AMBIENTE=dev|prod)
	docker run --rm $(PWD_DOCKER) -e AWS_PROFILE -e AWS_REGION -v $$HOME/.aws:/root/.aws:ro --entrypoint sh hashicorp/terraform:1.17.1 \
	  -c "cd infra/ambientes/$(AMBIENTE) && terraform init -input=false && terraform plan -input=false -out=plano.tfplan"

load-test: ## Carga: 500 req/s por 10 min (metas da secao 5.12)
	docker run --rm -i $(PWD_DOCKER) -e URL=$(URL) -e CHAVE=$(CHAVE) grafana/k6:1.3.0 run --summary-export=testes-carga/resultados/carga.json testes-carga/carga.js

stress-test: ## Estresse ate a quebra
	docker run --rm -i $(PWD_DOCKER) -e URL=$(URL) -e CHAVE=$(CHAVE) grafana/k6:1.3.0 run --summary-export=testes-carga/resultados/estresse.json testes-carga/estresse.js

soak-test: ## Soak de 30 min
	docker run --rm -i $(PWD_DOCKER) -e URL=$(URL) -e CHAVE=$(CHAVE) grafana/k6:1.3.0 run --summary-export=testes-carga/resultados/soak.json testes-carga/soak.js

seguranca: gitleaks trivy checkov ## Varreduras de seguranca (segredos, dependencias, imagem, IaC)

gitleaks:
	docker run --rm $(PWD_DOCKER) zricethezav/gitleaks:v8.28.0 git --redact -v .

trivy:
	docker run --rm $(PWD_DOCKER) aquasec/trivy:0.76.2 fs --scanners vuln,misconfig,secret --severity HIGH,CRITICAL --exit-code 1 .
	docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy:0.76.2 image --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 transacoes-api:local

checkov:
	docker run --rm $(PWD_DOCKER) bridgecrew/checkov:3.4.3 -d infra --quiet --compact --config-file infra/.checkov.yaml

zap: ## DAST com OWASP ZAP contra a API local (usa a spec)
	mkdir -p docs/04-testes/zap && chmod 777 docs/04-testes/zap
	docker run --rm -v "$(CURDIR)/api-spec":/zap/spec:ro -v "$(CURDIR)/docs/04-testes/zap":/zap/wrk:rw ghcr.io/zaproxy/zaproxy:2.16.1 \
	  zap-api-scan.py -t /zap/spec/openapi.yaml -f openapi -O $(URL) -r zap-relatorio.html -J zap-relatorio.json \
	  -z "-config replacer.full_list(0).description=chave -config replacer.full_list(0).enabled=true -config replacer.full_list(0).matchtype=REQ_HEADER -config replacer.full_list(0).matchstr=X-API-Key -config replacer.full_list(0).replacement=$(CHAVE)" || true
