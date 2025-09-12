# Todo List - Etapas 1, 2 e 3

Este repositório implementa as três primeiras etapas da aplicação TODO List: Usuário (com autenticação), Pastas e Tags, com documentação via Swagger, testes e H2.

## Decisões de arquitetura e bibliotecas

- Java 21 + Spring Boot 3.3.x
- Spring Data JPA + H2 (arquivo local em `./build/data` para dev e memória no profile `test`)
- Spring Security 6 com JWT (stateless)
- Validação com `spring-boot-starter-validation`
- Swagger/OpenAPI com `springdoc-openapi-starter-webmvc-ui`
- Lombok para reduzir boilerplate

Outras decisões importantes:
- JWT via JJWT 0.11.x (HS256). Subject=email, claim `uid` com o id do usuário. Expiração configurável (`app.security.jwt.expiration`).
- BCrypt para senhas (`BCryptPasswordEncoder`).
- Configuração de segurança retorna 401 para não autenticado e 403 quando falta permissão.

## Endpoints principais

- Autenticação/Usuário
	- POST `/api/users/register` -> cria usuário (retorna dados sem senha); cria automaticamente uma "Pasta Padrão" (privada)
	- GET `/api/users/{id}` -> busca usuário por id (requer Bearer)
	- POST `/api/auth/login` -> autentica e retorna JWT (Bearer)

- Pastas
	- POST `/api/folders` -> cria pasta (privada ou pública). Se pública, gera `key` numérica de 8 dígitos
	- GET `/api/folders` -> lista minhas pastas (do dono)
	- POST `/api/folders/join` -> entra em pasta pública via `key` (8 dígitos)
	- POST `/api/folders/{id}/rotate-key` -> rotaciona a `key` (apenas dono; somente se pública)
	- DELETE `/api/folders/{id}` -> remove a pasta (apenas dono)
	- DELETE `/api/folders/{id}/members/{userId}` -> remove membro (apenas dono)
	- GET `/api/folders/{id}/members` -> lista membros (dono ou qualquer membro)

- Tags (por pasta)
	- POST `/api/folders/{folderId}/tags` -> cria tag (somente dono da pasta). Nome obrigatório e único por pasta. Cor é gerada automaticamente (hex claro no formato `#RRGGBB`)
	- GET `/api/folders/{folderId}/tags` -> lista tags (dono ou qualquer membro)
	- DELETE `/api/folders/{folderId}/tags/{tagId}` -> remove tag (somente dono)

Swagger UI: `/swagger-ui.html`

## Configurações

- `src/main/resources/application.properties` aponta para H2 em arquivo (`./build/data/todolist-db`).
- JWT: secret Base64 e expiração configurados por propriedades (`app.security.jwt.*`).
- H2 Console em `/h2-console` (dev).

## Como rodar

Pré-requisito: Java 21. O Maven deve usar JDK 21.

Para rodar os testes e validar a aplicação (perfil test usa H2 em memória):

- Rodar testes: mvn test

Para executar a aplicação localmente (usa H2 em arquivo):

- Subir app: mvn spring-boot:run

## Autenticação (BCrypt + JWT)

- Senhas são hasheadas com BCrypt (`BCryptPasswordEncoder`).
- Login gera um JWT assinado (HS256) com subject=email e claim `uid` (id do usuário).
- Endpoints públicos: `/api/auth/**`, `/api/users/register`, Swagger (`/v3/api-docs/**`, `/swagger-ui/**`), H2 Console.
- Demais endpoints exigem header: `Authorization: Bearer <token>`.

## Testes

- Perfil `test` usa H2 em memória.
- Testes de integração:
	- Auth/Usuário: registro, login, acesso a endpoint protegido (401/200)
	- Pastas: criar privada e pública, listar, join por chave, rotacionar chave, remover membro, deletar (owner vs não owner), listar membros (permissões)

## Próximos passos

- Etapa 4 (Tarefas): CRUD de tarefas, associar múltiplas tags, checagem/conclusão, ordenação etc.

## Observação sobre Java 21

Este projeto utiliza Java 21 e Spring Boot 3.3.x. Garanta que o Maven está usando um JDK 21 (defina `JAVA_HOME`/`PATH` ou configure um toolchain Maven). Caso contrário, verá erro “release version 21 not supported”.

Caso você tenha rodado versões anteriores com H2 em arquivo, e veja erro de formato de arquivo do H2 após atualizar dependências, apague a pasta `./build/data` para recomeçar o banco local.
