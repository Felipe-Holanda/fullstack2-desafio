# Todo List - Etapas 1, 2, 3 e 4

Este repositório implementa as quatro primeiras etapas da aplicação TODO List: Usuário (com autenticação), Pastas, Tags e Tarefas, com documentação via Swagger, testes e H2.

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
	- GET `/api/folders` -> lista minhas pastas (sou dono)
	- GET `/api/folders/participating` -> lista pastas que participo (não sou dono)
	- GET `/api/folders/all` -> lista todas as pastas (sou dono ou participo)
	- POST `/api/folders/join` -> entra em pasta pública via `key` (8 dígitos)
	- POST `/api/folders/{id}/rotate-key` -> rotaciona a `key` (apenas dono; somente se pública)
	- DELETE `/api/folders/{id}` -> remove a pasta (apenas dono)
	- DELETE `/api/folders/{id}/members/{userId}` -> remove membro (apenas dono)
	- GET `/api/folders/{id}/members` -> lista membros (dono ou qualquer membro)

- Tags (por pasta)
	- POST `/api/folders/{folderId}/tags` -> cria tag (somente dono da pasta). Nome obrigatório e único por pasta. Cor é gerada automaticamente (hex claro no formato `#RRGGBB`)
	- GET `/api/folders/{folderId}/tags` -> lista tags (dono ou qualquer membro)
	- DELETE `/api/folders/{folderId}/tags/{tagId}` -> remove tag (somente dono)

- Tarefas (por pasta)
	- POST `/api/folders/{folderId}/tasks` -> cria tarefa raiz ou subtarefa (informe `parentTaskId`). Aceita `title` (obrigatório), `description`, `dueDate`, `tagIds` (vinculadas à mesma pasta). Novas tarefas começam com `completed=false`.
	- GET `/api/folders/{folderId}/tasks` -> lista apenas tarefas raiz, cada uma já com suas subtarefas embutidas.
	- GET `/api/folders/{folderId}/tasks/{taskId}` -> busca uma tarefa por id (com subtarefas).
	- PUT `/api/folders/{folderId}/tasks/{taskId}` -> atualiza campos (`title`, `description`, `dueDate`, `completed`, `tagIds`).
	- PATCH `/api/folders/{folderId}/tasks/{taskId}/completed?completed=true|false` -> marca/ desmarca conclusão.
	- DELETE `/api/folders/{folderId}/tasks/{taskId}` -> exclui a tarefa; subtarefas são removidas em cascata.

Permissões de tarefas:
- Dono ou qualquer membro da pasta pode criar, listar, buscar, atualizar e deletar tarefas daquela pasta.
- Validações: `tagIds` devem pertencer à mesma pasta; subtarefas devem ser da mesma pasta da tarefa pai; limite de no máximo 5 subtarefas diretas por tarefa.

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

### Docker

Você pode rodar a API em container. O banco H2 continua em arquivo e é persistido via volume mapeado para `./build/data`.

- Build da imagem e subir com Compose
	- docker compose up --build

Depois de subir:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:file:./build/data/todolist-db)

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
	- Tags: criar/listar/deletar, nome único por pasta, permissões (somente dono cria/deleta, membros podem listar)
	- Tarefas: criar raiz e subtarefas, validar limite de 5 subtarefas diretas, vincular/desvincular tags, listar (apenas raízes com subtarefas), atualizar, marcar concluída, deletar em cascata, permissões (membros têm acesso)

## Observação sobre Java 21

Este projeto utiliza Java 21 e Spring Boot 3.3.x. Garanta que o Maven está usando um JDK 21 (defina `JAVA_HOME`/`PATH` ou configure um toolchain Maven). Caso contrário, verá erro “release version 21 not supported”.

Caso você tenha rodado versões anteriores com H2 em arquivo, e veja erro de formato de arquivo do H2 após atualizar dependências, apague a pasta `./build/data` para recomeçar o banco local.
