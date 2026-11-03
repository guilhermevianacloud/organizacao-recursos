# language: pt
Funcionalidade: Aprovação de recurso restrito, bloqueio por manutenção e transições da reserva
  Regras RN-04, RN-05, RN-06 e RN-07 (docs/testes/bdd-aprovacao-bloqueio.md).
  Datas em 2032-01-15, UTC.

  Cenário: Recurso restrito aguarda a aprovação de um Responsável
    Dado um recurso restrito
    Quando o Solicitante pede o recurso das 08:00:00 às 10:00:00
    Então a resposta é 201
    E a reserva fica no estado "SOLICITADA"

  Cenário: Responsável aprova a reserva de recurso restrito
    Dado uma reserva de recurso restrito aguardando aprovação
    Quando o Responsável aprova a reserva
    Então a resposta é 200
    E a reserva fica no estado "APROVADA"

  Cenário: Solicitante não aprova a própria reserva
    Dado uma reserva de recurso restrito aguardando aprovação
    Quando o próprio Solicitante tenta aprovar a reserva
    Então a resposta é 403
    E a reserva fica no estado "SOLICITADA"

  Esquema do Cenário: Recurso em manutenção das 13:00 às 17:00 não pode ser reservado
    Dado uma sala em manutenção das 13:00:00 às 17:00:00
    Quando o Solicitante pede a sala das <inicio> às <fim>
    Então a resposta é <status>

    Exemplos:
      | inicio   | fim      | status |
      | 11:00:00 | 13:00:00 | 201    |
      | 11:00:00 | 13:00:01 | 409    |
      | 17:00:00 | 18:00:00 | 201    |
      | 16:59:59 | 18:00:00 | 409    |
      | 14:00:00 | 15:00:00 | 409    |

  Esquema do Cenário: A reserva só muda de estado pelas transições permitidas
    Dado uma reserva no estado "<estado>"
    Quando o Responsável registra "<acao>"
    Então a resposta é <status>

    Exemplos:
      | estado         | acao               | status |
      | APROVADA       | retirada           | 200    |
      | EM_USO         | devolucao          | 200    |
      | SOLICITADA     | retirada           | 409    |
      | CANCELADA      | aprovacao          | 409    |
      | CONCLUIDA      | retirada           | 409    |
      | SOLICITADA     | rejeicao           | 200    |
      | APROVADA       | nao-comparecimento | 200    |
      | EM_USO         | nao-comparecimento | 409    |
      | REJEITADA      | aprovacao          | 409    |
      | NAO_COMPARECEU | retirada           | 409    |
