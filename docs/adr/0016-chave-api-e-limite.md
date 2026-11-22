# 0016: Chave de API e limite de requisições na aplicação

## Decisão
- Em produção o API Gateway valida a chave e limita (usage plans). A aplicação repete a validação (defesa em profundidade): `FiltroChaveApi` compara o SHA-256 do `X-API-Key` com a lista `seguranca.chaves-api-sha256`, vinda do Secrets Manager (injetada pelo ECS como variável) ou do `.env` local. Ausente ou inválida → 401 no formato `Erro`.
- Só rotas `/api/**` exigem chave; `/actuator/health/**`, `/swagger-ui` e `/api-spec` são públicas (o Gateway só expõe `/api/**`).
- Logs e auditoria usam apenas o prefixo de 8 caracteres do hash.
- Limite local com Bucket4j por chave (perfil `local`): 50 req/s com rajada de 100; excedido → 429 com `Retry-After` em segundos (arredondado para cima).

## Consequências
Lista vazia → todas as requisições recebem 401 (fail-closed), com aviso no log ao subir.
