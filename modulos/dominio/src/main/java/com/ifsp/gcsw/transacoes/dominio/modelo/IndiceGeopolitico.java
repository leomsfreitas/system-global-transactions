package com.ifsp.gcsw.transacoes.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "indice_geopolitico")
public class IndiceGeopolitico {

  @Id private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pais_id")
  private Pais pais;

  @Column(name = "indice_risco", nullable = false, precision = 5, scale = 2)
  private BigDecimal indiceRisco;

  @Column(name = "fator_ajuste", nullable = false, precision = 6, scale = 4)
  private BigDecimal fatorAjuste;

  @Column(name = "data_referencia", nullable = false)
  private LocalDate dataReferencia;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "fonte_id")
  private FonteDados fonte;

  protected IndiceGeopolitico() {}

  public Long getId() {
    return id;
  }

  public Pais getPais() {
    return pais;
  }

  public BigDecimal getIndiceRisco() {
    return indiceRisco;
  }

  public BigDecimal getFatorAjuste() {
    return fatorAjuste;
  }

  public LocalDate getDataReferencia() {
    return dataReferencia;
  }

  public FonteDados getFonte() {
    return fonte;
  }
}
