package com.ifsp.gcsw.transacoes.dominio.repositorio;

import com.ifsp.gcsw.transacoes.dominio.modelo.Conversao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversaoRepositorio extends JpaRepository<Conversao, Long> {}
