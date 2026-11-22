# 0017: API Gateway REST com VPC Link v2 e ALB interno

## Contexto
O documento põe o ALB em sub-rede pública, o que permite contornar o Gateway e a validação de chave (seção 5.8). Chaves de API e usage plans existem só no API Gateway **REST** API.

## Verificação (22/11/2026)
- A AWS passou a suportar, em novembro de 2025, integração privada de **REST APIs** diretamente com **Application Load Balancer** usando **VPC Link v2**, sem NLB intermediário ([guia do desenvolvedor](https://docs.aws.amazon.com/apigateway/latest/developerguide/private-integration.html), [blog AWS Compute](https://aws.amazon.com/blogs/compute/build-scalable-rest-apis-using-amazon-api-gateway-private-integration-with-application-load-balancer/)).
- VPC Link v1 (que exigia NLB) é legado.
- Restrições: sub-redes e security groups do VPC Link v2 não mudam depois de criado; link sem tráfego por 60 dias fica `INACTIVE`; Gateway, link e ALB na mesma conta.

## Decisão
- ALB **interno** (sub-redes privadas), alcançado só pelo VPC Link v2 do API Gateway REST (`aws_api_gateway_integration` com `connection_type = "VPC_LINK"` e `integration_target` = ARN do ALB).
- Security group do ALB aceita apenas o security group do VPC Link.
- Plano B (se o provider Terraform da época não suportar `integration_target`): NLB interno na frente do ALB com VPC Link v1. Não foi necessário (ver validação em `infra/`).
- Domínio customizado com certificado ACM no Gateway; WAF com regras gerenciadas associado ao estágio.

## Consequências
Nenhum caminho público chega à aplicação sem passar pelo Gateway. Monitorar o estado do VPC Link (alarme) por causa da inativação após 60 dias sem tráfego.
