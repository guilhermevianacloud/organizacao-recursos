# Matriz de autorização

Aula 12. Uma linha por operação da API; em cada célula, o status esperado para o perfil. O teste
`MatrizDeAutorizacaoIT` **lê esta tabela** e executa cada célula com um token real do perfil:
mudar a matriz muda o teste. Regras de posse (RN-10): o Solicitante só altera e cancela as próprias
reservas; só o Responsável aprova recurso restrito (RN-05).

Nomes entre chaves na rota são preparados pelo teste: `{recurso}` recurso existente; `{reservaPropria}`
reserva do Solicitante que faz o pedido; `{reservaDeOutro}` reserva de outro Solicitante; `{reservaRestrita}`
reserva de recurso restrito aguardando aprovação; `{reservaAprovada}` reserva aprovada; `{reservaEmUso}`
reserva em uso.

| ID | Operação | Método | Rota | Anônimo | Solicitante | Responsável | Administrador |
|---|---|---|---|---|---|---|---|
| AUT-01 | Entrar (login) | POST | /api/auth/login | 200 | 200 | 200 | 200 |
| REC-01 | Pesquisar recursos | GET | /api/recursos | 401 | 200 | 200 | 200 |
| REC-02 | Consultar um recurso | GET | /api/recursos/{recurso} | 401 | 200 | 200 | 200 |
| REC-03 | Cadastrar recurso | POST | /api/recursos | 401 | 403 | 403 | 201 |
| REC-04 | Alterar recurso | PUT | /api/recursos/{recurso} | 401 | 403 | 403 | 200 |
| MAN-01 | Listar manutenções | GET | /api/manutencoes | 401 | 200 | 200 | 200 |
| MAN-02 | Bloquear recurso para manutenção | POST | /api/manutencoes | 401 | 403 | 403 | 201 |
| RES-01 | Agenda do dia | GET | /api/reservas?data=2026-11-09 | 401 | 200 | 200 | 200 |
| RES-02 | Minhas reservas | GET | /api/reservas/minhas | 401 | 200 | 403 | 403 |
| RES-03 | Solicitar reserva | POST | /api/reservas | 401 | 201 | 403 | 403 |
| RES-04 | Alterar a própria reserva | PUT | /api/reservas/{reservaPropria} | 401 | 200 | 403 | 403 |
| RES-05 | Cancelar a própria reserva | POST | /api/reservas/{reservaPropria}/cancelamento | 401 | 200 | 403 | 403 |
| RES-06 | Cancelar reserva de outro (IDOR) | POST | /api/reservas/{reservaDeOutro}/cancelamento | 401 | 403 | 403 | 403 |
| RES-07 | Alterar reserva de outro (IDOR) | PUT | /api/reservas/{reservaDeOutro} | 401 | 403 | 403 | 403 |
| RES-08 | Aprovar reserva de restrito | POST | /api/reservas/{reservaRestrita}/aprovacao | 401 | 403 | 200 | 403 |
| RES-09 | Rejeitar reserva de restrito | POST | /api/reservas/{reservaRestrita}/rejeicao | 401 | 403 | 200 | 403 |
| RES-10 | Registrar retirada | POST | /api/reservas/{reservaAprovada}/retirada | 401 | 403 | 200 | 403 |
| RES-11 | Registrar devolução | POST | /api/reservas/{reservaEmUso}/devolucao | 401 | 403 | 200 | 403 |
| RES-12 | Registrar não comparecimento | POST | /api/reservas/{reservaAprovada}/nao-comparecimento | 401 | 403 | 200 | 403 |
| RES-13 | Histórico (auditoria) de uma reserva | GET | /api/reservas/{reservaPropria}/historico | 401 | 403 | 200 | 200 |
| RES-14 | Reservas aguardando aprovação | GET | /api/reservas/pendentes | 401 | 403 | 200 | 403 |
| REL-01 | Relatório de utilização | GET | /api/relatorios/utilizacao?inicio=2026-11-09T00:00:00Z&fim=2026-11-10T00:00:00Z | 401 | 403 | 200 | 200 |

## Rotas sem regra de segurança
Nenhuma rota da API fica fora da matriz: a configuração nega (`denyAll`) qualquer rota que não seja da API,
da interface, da documentação ou do login.

## Divergências encontradas
Nenhuma até 2026-10-31: as 76 células passaram na primeira execução da implementação (ver `docs/testes/evidencia-tdd.md`).
