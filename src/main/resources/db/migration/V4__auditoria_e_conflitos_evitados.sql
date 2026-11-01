-- RF-10 / RN-09: trilha imutável de mudanças das reservas, gravada na mesma transação da mudança.
CREATE TABLE auditoria_reserva (
    id              BIGSERIAL PRIMARY KEY,
    reserva_id      BIGINT       NOT NULL REFERENCES reserva (id),
    usuario_id      BIGINT       NOT NULL REFERENCES usuario (id),
    instante        TIMESTAMPTZ  NOT NULL,
    estado_anterior VARCHAR(20),
    estado_novo     VARCHAR(20)  NOT NULL,
    motivo          VARCHAR(255) NOT NULL
);

CREATE INDEX auditoria_reserva_reserva_idx ON auditoria_reserva (reserva_id);

-- RF-12: tentativas recusadas por sobreposição ("conflitos evitados").
-- Sem chave estrangeira de propósito: o registro é gravado numa transação própria, enquanto a
-- transação do pedido ainda trava a linha do recurso (ADR-002); uma FK disputaria essa trava.
CREATE TABLE tentativa_recusada (
    id         BIGSERIAL PRIMARY KEY,
    recurso_id BIGINT      NOT NULL,
    inicio     TIMESTAMPTZ NOT NULL,
    termino    TIMESTAMPTZ NOT NULL,
    instante   TIMESTAMPTZ NOT NULL
);

CREATE INDEX tentativa_recusada_periodo_idx ON tentativa_recusada (inicio, termino);
