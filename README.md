# JTech Tasklist — Fullstack ToDo (Frontend + Backend)

Aplicação completa de lista de tarefas com autenticação JWT, pastas compartilháveis, tarefas, microtarefas (subtasks) e tags. O projeto está dividido em dois módulos:

- `jtech-tasklist-backend`: API REST em Spring Boot 3 (Java 21) com H2, JPA e segurança JWT
- `jtech-tasklist-frontend`: SPA em Vue 3 + Vite + TypeScript consumindo a API


## Funcionalidades

- Cadastro e autenticação
	- Registro de usuário e login na API com emissão de JWT
	- Proteção de rotas no backend; frontend armazena o token e injeta Authorization Bearer

- Pastas (Folders) com compartilhamento
	- Criação e listagem de pastas do usuário
	- Pastas públicas com chave de acesso (join key): qualquer usuário pode entrar informando a chave
	- Gerenciamento de membros (owner pode listar/remover membros)
	- Compartilhamento efetivo ocorre no nível da pasta: todos os membros veem e manipulam as tarefas da pasta

- Tarefas (Tasks)
	- CRUD completo de tarefas dentro de cada pasta
	- Marcar como concluída/pendente
	- As permissões respeitam a propriedade/participação na pasta

- Microtarefas (Subtasks)
	- Cada tarefa pode ter subtarefas (microtasks)
	- Limite de 5 subtarefas por tarefa (regra validada pela API)

- Tags
	- Criação e remoção de tags por pasta
	- Associação de tags às tarefas da pasta

- Observabilidade e ferramentas
	- Swagger/OpenAPI disponível em `/swagger-ui.html`
	- Console do H2 disponível em `/h2-console` (apenas em desenvolvimento)


## Stack Tecnológica

### Backend (jtech-tasklist-backend)
- Linguagem/Runtime: Java 21 (Temurin)
- Framework: Spring Boot 3.3
- Segurança: Spring Security + JWT (jjwt)
- Persistência: Spring Data JPA + Banco H2 (file-based)
- Documentação: springdoc-openapi (Swagger UI)
- Build: Maven (mvn wrapper incluso)
- Utilidades: Lombok
- Testes: Spring Boot Starter Test + Spring Security Test

### Frontend (jtech-tasklist-frontend)
- Framework: Vue 3 (Composition API) + TypeScript
- Build/Dev Server: Vite
- Estado: Pinia
- Roteamento: Vue Router 4
- Estilo: Tailwind CSS v4
- Testes: Vitest + @vue/test-utils
- Lint/Format: ESLint + Prettier


## Como executar localmente (sem Docker)

Pré-requisitos:
- Java 21 + Maven Wrapper (ou Maven 3.9+)
- Node.js 20 LTS (ou >=22.12) e npm

1) Backend
- Em `jtech-tasklist-backend`:

```bash
./mvnw spring-boot:run
```

Isso sobe a API em http://localhost:8080 com H2 file em `./build/data`. Endpoints úteis:
- Swagger: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:file:./build/data/todolist-db`)

2) Frontend
- Em `jtech-tasklist-frontend`:

```bash
npm install
npm run dev
```

O app abre em http://localhost:5173. A base da API está configurada no frontend como `http://localhost:8080` (arquivo `src/lib/api/client.ts`).

3) Testes
- Backend:

```bash
./mvnw test
```

- Frontend:

```bash
npm run test:unit
```


## Executando com Docker (API + Front juntas)

Este repositório inclui:
- `jtech-tasklist-backend/Dockerfile` (já existente) para a API
- `jtech-tasklist-frontend/Dockerfile` para build/serve da SPA
- `docker-compose.yml` na raiz orquestrando os dois serviços
- Script auxiliar `scripts/docker-run.sh`

Subir tudo (a partir da raiz do repo):

```bash
./scripts/docker-run.sh up
```

Após a subida:
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

Outros comandos úteis do script:

```bash
./scripts/docker-run.sh down   # derruba os serviços
./scripts/docker-run.sh logs   # acompanha os logs dos serviços
./scripts/docker-run.sh ps     # lista containers
./scripts/docker-run.sh rebuild  # força rebuild de imagens
```

Notas:
- O banco H2 é persistido em um volume Docker nomeado (`h2data`), mapeado para `/app/build/data` no container da API.
- O frontend chama a API via `http://localhost:8080`; como o navegador executa no host, isso funciona normalmente quando ambos os serviços estão publicados nas portas do host.


## Estrutura de pastas (resumo)

```
├─ jtech-tasklist-backend/        # API Spring Boot
│  ├─ src/main/java/...           # código da API
│  ├─ src/main/resources/
│  │  └─ application.properties   # H2 file, Swagger, JWT
│  └─ Dockerfile                  # build multi-stage Java 21
├─ jtech-tasklist-frontend/       # SPA Vue 3 + TS
│  ├─ src/                        # componentes, serviços, stores
│  ├─ Dockerfile                  # build e serve via Nginx
│  └─ nginx.conf                  # SPA fallback (history mode)
└─ docker-compose.yml             # orquestra front+back
```


## Decisões/Observações técnicas

- Compartilhamento é feito no nível da pasta: entradas via chave pública ou gestão de membros; tarefas e tags são sempre vinculadas à pasta.
- Subtarefas são representadas como `Task` com `parentTask`, limitadas a 5 por tarefa.
- CORS liberado com `allowedOriginPatterns=*` para facilitar desenvolvimento (não usar em prod sem restrições específicas).
- H2 em arquivo facilita testes locais; em produção, substitua por banco relacional gerenciado.


## Roadmap (sugestões)

- Configurar variável de ambiente para `BASE_URL` do frontend via injeção no build.
- Adicionar papéis (roles) com granularidade de permissões por pasta/tarefa.
- Internacionalização (i18n) no frontend.

