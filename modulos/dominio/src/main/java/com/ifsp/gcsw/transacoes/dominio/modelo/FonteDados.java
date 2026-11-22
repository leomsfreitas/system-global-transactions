package com.ifsp.gcsw.transacoes.dominio.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fonte_dados")
public class FonteDados {

  @Id private Long id;

  @Column(nullable = false)
  private String codigo;

  @Column(nullable = false)
  private String nome;

  @Column(name = "url_base", nullable = false)
  private String urlBase;

  @Column(nullable = false)
  private String tipo;

  @Column(nullable = false)
  private boolean ativa;

  protected FonteDados() {}

  public Long getId() {
    return id;
  }

  public String getCodigo() {
    return codigo;
  }

  public String getNome() {
    return nome;
  }

  public String getUrlBase() {
    return urlBase;
  }

  public String getTipo() {
    return tipo;
  }

  public boolean isAtiva() {
    return ativa;
  }
}
