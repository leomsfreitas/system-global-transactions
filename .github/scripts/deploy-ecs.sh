#!/usr/bin/env bash
# Deploy no ECS: registra novas revisoes das definicoes de tarefa com a imagem do SHA, roda a migracao
# como tarefa unica (falha = aborta) e atualiza o servico da API (rolling + circuit breaker com rollback).
# Variaveis: NOME (ex.: transacoes-prod), IMAGEM_API, IMAGEM_WORKER, SUBNETS (csv), SG_WORKER
set -euo pipefail

nova_revisao() { # familia container imagem -> arn da nova revisao
  local familia=$1 container=$2 imagem=$3
  aws ecs describe-task-definition --task-definition "$familia" --query taskDefinition --output json \
    | jq --arg c "$container" --arg i "$imagem" '
        .containerDefinitions |= map(if .name == $c then .image = $i else . end)
        | del(.taskDefinitionArn, .revision, .status, .requiresAttributes, .compatibilities, .registeredAt, .registeredBy)' \
    > /tmp/td.json
  aws ecs register-task-definition --cli-input-json file:///tmp/td.json --query taskDefinition.taskDefinitionArn --output text
}

CLUSTER="$NOME"
TD_MIGRACAO=$(nova_revisao "$NOME-migracao" migracao "$IMAGEM_WORKER")
nova_revisao "$NOME-worker" worker "$IMAGEM_WORKER" >/dev/null   # o EventBridge usa a familia (ultima revisao)
TD_API=$(nova_revisao "$NOME-api" api "$IMAGEM_API")

echo "Migracao: $TD_MIGRACAO"
TAREFA=$(aws ecs run-task --cluster "$CLUSTER" --task-definition "$TD_MIGRACAO" --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNETS],securityGroups=[$SG_WORKER],assignPublicIp=DISABLED}" \
  --query 'tasks[0].taskArn' --output text)
aws ecs wait tasks-stopped --cluster "$CLUSTER" --tasks "$TAREFA"
CODIGO=$(aws ecs describe-tasks --cluster "$CLUSTER" --tasks "$TAREFA" --query 'tasks[0].containers[0].exitCode' --output text)
if [ "$CODIGO" != "0" ]; then
  echo "::error::Migracao falhou (codigo $CODIGO). Deploy abortado; a API continua na versao anterior."
  exit 1
fi

echo "API: $TD_API"
aws ecs update-service --cluster "$CLUSTER" --service api --task-definition "$TD_API" >/dev/null
# Se as tarefas novas nao ficarem saudaveis, o circuit breaker faz rollback e o wait falha.
aws ecs wait services-stable --cluster "$CLUSTER" --services api
ATUAL=$(aws ecs describe-services --cluster "$CLUSTER" --services api --query 'services[0].taskDefinition' --output text)
if [ "$ATUAL" != "$TD_API" ]; then
  echo "::error::Rollback automatico acionado pelo circuit breaker (servico em $ATUAL)."
  exit 1
fi
echo "Deploy concluido: $TD_API"
