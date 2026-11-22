package com.ifsp.gcsw.transacoes.dominio.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** cache.* (secao 6.4). */
@ConfigurationProperties("cache")
public record PropriedadesCache(
    @DefaultValue("true") boolean habilitado,
    @DefaultValue("15m") Duration ttlCotacao,
    @DefaultValue("24h") Duration ttlIndice,
    @DefaultValue("24h") Duration ttlIndicadores,
    @DefaultValue("24h") Duration ttlMoedas,
    @DefaultValue("30s") Duration pausaAposFalha) {}
