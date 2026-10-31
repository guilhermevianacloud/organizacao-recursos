-- Fase 3: usuários, recursos (sala, material, professor), manutenções e reservas.
-- A proteção contra sobreposição no banco (ADR-002) entra na migração V3.

CREATE TABLE usuario (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    perfil     VARCHAR(20)  NOT NULL CHECK (perfil IN ('SOLICITANTE', 'RESPONSAVEL', 'ADMINISTRADOR')),
    senha_hash VARCHAR(255) NOT NULL
);

CREATE TABLE recurso (
    id           BIGSERIAL PRIMARY KEY,
    tipo         VARCHAR(20)  NOT NULL CHECK (tipo IN ('SALA', 'MATERIAL', 'PROFESSOR')),
    nome         VARCHAR(255) NOT NULL,
    capacidade   INTEGER CHECK (capacidade IS NULL OR capacidade >= 0),
    localizacao  VARCHAR(255),
    competencias VARCHAR(255),
    restrito     BOOLEAN NOT NULL DEFAULT FALSE,
    ativo        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE manutencao (
    id         BIGSERIAL PRIMARY KEY,
    recurso_id BIGINT       NOT NULL REFERENCES recurso (id),
    inicio     TIMESTAMPTZ  NOT NULL,
    termino    TIMESTAMPTZ  NOT NULL,
    motivo     VARCHAR(255),
    CONSTRAINT manutencao_termino_apos_inicio CHECK (termino > inicio)
);

CREATE TABLE reserva (
    id             BIGSERIAL PRIMARY KEY,
    recurso_id     BIGINT       NOT NULL REFERENCES recurso (id),
    solicitante_id BIGINT       NOT NULL REFERENCES usuario (id),
    inicio         TIMESTAMPTZ  NOT NULL,
    termino        TIMESTAMPTZ  NOT NULL,
    estado         VARCHAR(20)  NOT NULL CHECK (estado IN ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA',
                                                          'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU')),
    motivo         VARCHAR(255),
    versao         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT reserva_termino_apos_inicio CHECK (termino > inicio)
);

CREATE INDEX reserva_recurso_idx ON reserva (recurso_id);
CREATE INDEX manutencao_recurso_idx ON manutencao (recurso_id);
