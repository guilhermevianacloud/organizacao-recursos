package br.senac.organizacaorecursos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Período em que o recurso fica bloqueado para reservas (RN-04). */
@Entity
@Table(name = "manutencao")
public class Manutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "recurso_id")
    private Recurso recurso;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant termino;

    private String motivo;

    protected Manutencao() {
    }

    public Manutencao(Recurso recurso, Intervalo intervalo, String motivo) {
        this.recurso = recurso;
        this.inicio = intervalo.inicio();
        this.termino = intervalo.termino();
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public Intervalo intervalo() {
        return new Intervalo(inicio, termino);
    }

    public String getMotivo() {
        return motivo;
    }
}
