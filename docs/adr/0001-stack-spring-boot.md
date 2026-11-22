# 0001: Stack: Java 21, Spring Boot 4.1 e Maven multi-módulo

## Contexto
A seção 4 fixa Java 21, Spring Boot "na versão estável mais recente compatível", Maven multi-módulo com wrapper, sem milestone/RC. Em 22/11/2026 as linhas GA eram 4.1.x e 4.2.0 (esta publicada em 19/11/2026).

## Decisão
- Spring Boot **4.1.2** (publicado em 22/10/2026). A 4.2.0 e a 4.1.3 tinham 3 dias de publicação e foram evitadas pela política do ADR 0002.
- Spring Framework 7, Jackson 3 (`tools.jackson`), Hibernate 7, Flyway 12, Testcontainers 2.0.
- Módulos: `dominio`, `conectores`, `api`, `worker`. A API e o worker têm classe principal no pacote raiz `com.ifsp.gcsw.transacoes` para que o Spring encontre entidades e repositórios do domínio sem configuração extra.
- Bibliotecas compatíveis com Boot 4: springdoc 3.1.1, Resilience4j 2.4.0 (core + reactor + micrometer, configurado programaticamente), Bucket4j 8.22.0, WireMock 3.13.2, openapi-request-validator 3.0.0 (o artefato `swagger-request-validator-core` 3.0.0 é só uma relocação para ele).
- Virtual threads ligadas na API (`spring.threads.virtual.enabled`).

## Alternativas
- Boot 3.5: mais maduro, mas encerra suporte OSS antes do fim do projeto.
- Boot 4.2.0: mais novo, publicado há 3 dias; risco de regressão sem ganho funcional.

## Consequências
- Starters do Boot 4 são modulares (`spring-boot-starter-webmvc`, `-webclient`, `-flyway`, `-*-test`); a configuração do Jackson usa `JsonMapperBuilderCustomizer`.
- Atualizar para 4.2.x é uma troca de versão no `pom.xml` pai depois da janela de 14 dias.
