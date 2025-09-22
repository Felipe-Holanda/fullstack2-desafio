
Preciso criar uma aplicação TODO List;

Os pontos cruciais dessa aplicação:

- Precisa ser toda documentada no Swagger.
- Precisa ter um README.md informando toda e qualquer decisão tomada, seja decisões de arquiteturas e bibliotecas.

## Características da aplicação que imagino serem cruciais:

- Usuário:
	> é um dos pilares da aplicação. O usuário quem terá todos os elementos.

	1. Usuário deve autenticar-se com token JWT.
	2. Sua senha deve ser criptografada com BCrypt.
	3. Deve ter como propriedades: Nome, email e senha.

- Pastas:
	> Para criar tarefas nesta aplicação, é necessário criar uma pasta.
	1. Cada usuário terá uma pasta padrão.
	2. O usuário poderá definir se uma pasta é pública ou privada.
	3. Se a pasta for pública, qualquer usuário poderá ter acesso se inserir um código de 8 dígitos considerado como "chave da pasta".
	4. Somente quem criou a pasta pode deletar ela.
	5. Quem criou a pasta pode remover outra pessoa.
	6. É possível alterar a chave da pasta.

- Tags
	> São vinculadas as pastas, e são importantes para determinar elementos de maneira visual.
	1. Cada tag tem um nome e uma cor, e devem ser únicas.
	2. As tags são atreladas as pastas.

- Tarefas
	> É o foco central da aplicação.
	1. Cada tarefa poderá ter subtarefas (Limitado a 5)
	2. Cada tarefa pode ter várias Tags.
	3. Ao cadastrar uma tarefa, teremos que possibilitar a atribuição destas.

