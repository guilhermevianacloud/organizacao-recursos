# Changelog

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
