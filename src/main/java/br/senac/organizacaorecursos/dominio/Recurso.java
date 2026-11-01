package br.senac.organizacaorecursos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Sala, material ou professor que pode ser reservado. */
@Entity
@Table(name = "recurso")
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRecurso tipo;

    @Column(nullable = false)
    private String nome;

    private Integer capacidade;

    private String localizacao;

    private String competencias;

    @Column(nullable = false)
    private boolean restrito;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Recurso() {
    }

    public Recurso(TipoRecurso tipo, String nome, boolean restrito) {
        this.tipo = tipo;
        this.nome = nome;
        this.restrito = restrito;
    }

    /** Atualiza os dados cadastrais (RF-02, só Administrador). */
    public void atualizar(TipoRecurso tipo, String nome, Integer capacidade, String localizacao,
                          String competencias, boolean restrito) {
        this.tipo = tipo;
        this.nome = nome;
        this.capacidade = capacidade;
        this.localizacao = localizacao;
        this.competencias = competencias;
        this.restrito = restrito;
    }

    public Long getId() {
        return id;
    }

    public TipoRecurso getTipo() {
        return tipo;
    }

    public String getNome() {
        return nome;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(Integer capacidade) {
        this.capacidade = capacidade;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public String getCompetencias() {
        return competencias;
    }

    public void setCompetencias(String competencias) {
        this.competencias = competencias;
    }

    public boolean isRestrito() {
        return restrito;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
