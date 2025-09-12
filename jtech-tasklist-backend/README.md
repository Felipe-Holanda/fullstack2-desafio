# Todo List - Etapa 1 (Parte A)

Implementação inicial do domínio de Usuário (entidades, repositório, serviço e controladores) e documentação Swagger. A autenticação JWT e senha BCrypt serão adicionadas na próxima sub-etapa.

## Decisões de arquitetura e bibliotecas

- Spring Boot
- Spring Data JPA + H2 (arquivo local em `./build/data`)
- Spring Security (temporariamente liberado, JWT virá na próxima etapa)
- Validação com `spring-boot-starter-validation`
- Swagger/OpenAPI com `springdoc-openapi-ui`
- Lombok para reduzir boilerplate

## Endpoints (até agora)

- POST `/api/users/register` -> cria usuário (retorna dados sem senha)
- GET `/api/users/{id}` -> busca usuário por id
- POST `/api/auth/login` -> autentica e retorna JWT (Bearer)

Swagger UI: `/swagger-ui.html`

## Configurações

- `src/main/resources/application.properties` já aponta para H2 em arquivo (`./build/data/todolist-db`).
- Chaves de JWT já existem como placeholder e serão usadas na próxima fase.
- H2 Console em `/h2-console` (dev).

## Como rodar

Pré-requisito: Java 21 (conforme `pom.xml` atualizado). Recomenda-se JDK Temurin 21.

1) Buildar e rodar testes
2) Executar a aplicação localmente

## Autenticação (BCrypt + JWT)

- Senhas são hasheadas com BCrypt (`BCryptPasswordEncoder`).
- Login gera um JWT assinado (HS256) com subject=email e claim `uid` (id do usuário).
- Endpoints públicos: `/api/auth/**`, `/api/users/register`, Swagger (`/v3/api-docs/**`, `/swagger-ui/**`), H2 Console.
- Demais endpoints exigem header: `Authorization: Bearer <token>`.

## Testes

- Perfil `test` usa H2 em memória. Incluímos teste de integração para registro de usuário (feliz e duplicado).

## Próximos passos

- Prosseguir para Etapa 2 (Pastas) assim que aprovado.

## Observação sobre Java 21

Este projeto agora utiliza Java 21 e Spring Boot 3.3.x. Para buildar localmente, garanta que o Maven está usando um JDK 21 (defina `JAVA_HOME`/`PATH` ou configure um toolchain Maven). Caso contrário, verá erro “release version 21 not supported”.
