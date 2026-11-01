# ADR-006 — Interface web estática servida pelo Spring

Status: aceita (2026-11-01; decidida pelo Claude dentro das recomendações aceitas por Guilherme Viana Lopes) · decisões D-02 e D-03

## Contexto e forças
- RF-13: interface responsiva e mensagens de erro compreensíveis. A especificação não define tecnologia de interface.
- Limitador de nota: "aplicação que não executa" — o professor clona e segue só o README (Aula 14).
- A API já usa JWT (ADR-003); testes, JMeter e interface devem autenticar do mesmo jeito.
- O grupo pediu um visual cuidado.

## Alternativas
- (a) Thymeleaf + CSS — páginas geradas no servidor; exigiria um segundo modelo de login (sessão com cookie e CSRF) ao lado do JWT.
- (b) React compilado pelo Maven — mais peças (Node no build, mais tempo de CI, mais código para estudar).
- (c) React separado — duas aplicações para subir; aumenta o risco do limitador.
- (d) **Páginas HTML + CSS próprio + JavaScript sem framework**, servidas pelo próprio Spring (`src/main/resources/static`), consumindo a API com o mesmo JWT.

## Decisão
(d). `index.html` (login com atalhos para os usuários de demonstração) e `app.html` (agenda em linha do tempo, nova reserva com busca de livres, minhas reservas, aprovações, detalhe com histórico e ações do Responsável, relatório, recursos e manutenção). O menu mostra só as seções do perfil, espelhando a matriz de autorização; a API continua sendo quem decide (403 se alguém tentar por fora).

## Consequências
- \+ Um comando sobe tudo (`./mvnw spring-boot:run`); nenhum build de front-end.
- \+ Um só modelo de autenticação para tudo.
- \+ Mensagens de erro vêm do ProblemDetail da API, em português, com o código.
- − Sem componentes reutilizáveis de framework; o JavaScript cresce à mão (um arquivo, ~500 linhas).
- − Token guardado no `sessionStorage` da aba: some ao fechar a aba (aceito; reduz o tempo de exposição).
- − Fonte IBM Plex carregada do Google Fonts: sem internet, o navegador usa a fonte do sistema.

## Riscos
- Injeção de HTML com dados da API: todo texto passa por uma função de escape antes de entrar na página.

## Evidência
- Teste de fumaça (2026-11-01): páginas, CSS, JS e Swagger com 200; login de demonstração e API respondendo.
- Teste end-to-end com navegador real na Fase 6 (D-03: Playwright).

## Condição de revisão
Se a interface crescer além do que um arquivo de JavaScript sustenta com clareza, migrar para um framework em novo ADR.
