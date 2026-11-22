# 0002: Política de versões de dependências e imagens

## Contexto
Ataques de cadeia de suprimentos costumam explorar versões recém-publicadas. A especificação exige varredura de dependências e imagem, mas não define critério de escolha de versão.

## Decisão
- Toda dependência Maven, imagem Docker e ferramenta de CI usa **versão exata**, publicada há **pelo menos 14 dias** na data da escolha (verificado pelo `Last-Modified` do Maven Central e pelo `tag_last_pushed` do Docker Hub).
- Exemplos da aplicação desta regra em 22/11/2026: Spring Boot 4.1.2 em vez de 4.1.3; Bucket4j 8.22.0 em vez de 8.23.0 (publicada no próprio dia); Spotless 3.11.0 em vez de 3.12.0; `postgres:17.10-alpine` em vez de 17.11; `redis:7.4.11-alpine`; `eclipse-temurin:21.0.12_8`.
- Correção de vulnerabilidade alta/crítica pode furar a janela, com registro no relatório de segurança.

## Alternativas
- Sempre a última versão: maior exposição a pacote comprometido.
- Tags flutuantes (`17-alpine`): builds não reprodutíveis.

## Consequências
- Atualizações exigem conferir a data; o Dependabot/Renovate (pós-projeto) deve usar `minimumReleaseAge: 14 days`.
