-- ADR-002 / DEF-001: o banco recusa duas reservas ativas do mesmo recurso com intervalos que se cruzam.
-- btree_gist permite combinar igualdade (recurso_id =) com sobreposição de intervalo (&&) no mesmo índice GiST.
-- '[)' = intervalo semiaberto (ADR-004): reservas encostadas não conflitam.
-- Só estados que ocupam o recurso entram: canceladas, rejeitadas e encerradas não bloqueiam (risco R-01).

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reserva
    ADD CONSTRAINT reserva_sem_sobreposicao
    EXCLUDE USING gist (
        recurso_id WITH =,
        tstzrange(inicio, termino, '[)') WITH &&
    )
    WHERE (estado IN ('SOLICITADA', 'APROVADA', 'EM_USO'));
