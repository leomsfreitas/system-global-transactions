package com.ifsp.gcsw.transacoes.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "indicador_economico")
public class IndicadorEconomico {

  @Id private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pais_id")
  private Pais pais;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TipoIndicador tipo;

  @Column(nullable = false)
  private BigDecimal valor;

  @Column(nullable = false)
  private LocalDate periodo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "fonte_id")
  private FonteDados fonte;

  protected IndicadorEconomico() {}

  public Long getId() {
    return id;
  }

  public Pais getPais() {
    return pais;
  }

  public TipoIndicador getTipo() {
    return tipo;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public LocalDate getPeriodo() {
    return periodo;
  }

  public FonteDados getFonte() {
    return fonte;
  }
}
