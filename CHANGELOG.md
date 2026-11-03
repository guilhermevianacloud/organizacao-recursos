# Changelog

## v1.0.0-rc3 — correções da interface e da revisão independente dos documentos

### Defeitos
- DEF-005 (relatado pelo Guilherme): data ou hora vazia derrubava a agenda e o relatório; agora a agenda mantém o dia e as outras telas mostram "Informe uma data e um horário válidos.".
- DEF-006 (achado pelos testes): resposta atrasada da agenda apagava a tela aberta depois.

### Interface
- Identidade visual nova, só no CSS (PRs #20, #21 e #22): degradê azul → ciano como assinatura, fonte Geist, cantos arredondados e sombras suaves, estados como pílulas com ponto. Agenda com o nome do recurso fixo ao rolar e o rótulo "23h" sem corte.

### Qualidade
- Testes novos: OpenAPI pública (RF-14), token vencido (ADR-003), transições REJEITADA e NAO_COMPARECEU no BDD, 4 testes de interface dos DEF-005/006. Total: 98 de unidade + 187 de integração.
- Mutação M11 (RN-06): 11 de 11 detectadas.
- CI: relatórios guardados por 90 dias (eram 7) e `SONAR_TOKEN` só no passo da análise.
- Ciclo ATAM 01, arquitetura como construída e rastreio dos defeitos com issues no GitHub (PR #15).
- Documentos corrigidos a partir da revisão independente: números (cobertura 583/595, vazão 270,2/s), afirmações acima da evidência, textos desatualizados, campos que faltavam nos ADRs e nos defeitos.

## v1.0.0-rc2 — versão candidata para a entrega, com análise estática

### Qualidade
- SonarCloud integrado ao CI (`SONAR_TOKEN`), lendo a cobertura do JaCoCo. Quality Gate aprovado.
- DEF-004: 12 apontamentos da primeira análise; 11 corrigidos (`SecureRandom` reaproveitado, promessas tratadas na
  interface, regex dos testes trocada por `JsonPath`) e 1 justificado (CSRF desligado em API stateless, ADR-007, proposta).

### Limitações conhecidas
- Revisão do apontamento S4502 no SonarCloud depende do dono do repositório.
- Desempenho medido só em máquina local (`docs/performance/resultado-01.md`); DEF-003 aberto.

## v1.0.0-rc1 — versão candidata para a entrega (Qualidade de Software 2026.2, semana 46)

### Funcionalidades (14 requisitos obrigatórios)
- Login com JWT e três perfis (Solicitante, Responsável, Administrador), com autorização por perfil e por posse.
- Cadastro e consulta de salas, materiais e professores; pesquisa por tipo, capacidade, localização, competência e disponibilidade.
- Criação, alteração e cancelamento de reservas; detecção de sobreposição, inclusive na agenda do professor.
- Proteção contra dupla reserva em pedidos simultâneos: trava por recurso + exclusion constraint no PostgreSQL (ADR-002).
- Aprovação obrigatória de recurso restrito; bloqueio de recurso em manutenção.
- Retirada e devolução de materiais, não comparecimento.
- Histórico auditável e imutável de toda mudança de estado.
- Notificação por provedor HTTP depois do commit (simulada sem provedor configurado).
- Relatório de utilização: horas por recurso, carga por professor e conflitos evitados.
- Interface web responsiva (agenda em linha do tempo, nova reserva, aprovações, relatório, cadastros) e documentação OpenAPI.

### Qualidade
- 273 testes automatizados no CI: 98 de unidade e 175 de integração, API, BDD (13 cenários) e ponta a ponta (Playwright).
- Cobertura JaCoCo: 98,0% de linhas e 95,0% de branches, com gate de 80/70 no `verify`.
- Matriz de autorização com 88 células testadas a partir do próprio documento.
- Mutação manual: 10 quebras de regras críticas, 10 detectadas.
- Teste de carga com JMeter: 18.899 pedidos, 0 erro real, integridade mantida.

### Defeitos
- Corrigidos: DEF-001 (dupla reserva sob concorrência, Crítica) e DEF-002 (deadlock na disputa pelo mesmo recurso, Alta).
- Aberto: DEF-003 (p95 acima da meta proposta de 300 ms sob carga, Média).

### Limitações conhecidas
- Análise estática no SonarCloud depende da configuração do token pelo dono do repositório.
- Desempenho medido só em máquina local (`docs/performance/resultado-01.md`).
