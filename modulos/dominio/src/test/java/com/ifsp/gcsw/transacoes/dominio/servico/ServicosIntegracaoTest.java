package com.ifsp.gcsw.transacoes.dominio.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ifsp.gcsw.transacoes.dominio.ContainersTeste;
import com.ifsp.gcsw.transacoes.dominio.cache.ChavesCache;
import com.ifsp.gcsw.transacoes.dominio.excecao.DadoIndisponivelException;
import com.ifsp.gcsw.transacoes.dominio.excecao.RecursoNaoEncontradoException;
import com.ifsp.gcsw.transacoes.dominio.excecao.RequisicaoInvalidaException;
import com.ifsp.gcsw.transacoes.dominio.modelo.Conversao;
import com.ifsp.gcsw.transacoes.dominio.modelo.TipoIndicador;
import com.ifsp.gcsw.transacoes.dominio.repositorio.ConversaoRepositorio;
import com.ifsp.gcsw.transacoes.dominio.repositorio.GravacaoDados;
import com.ifsp.gcsw.transacoes.dominio.taxa.MetodoTaxa;
import com.ifsp.gcsw.transacoes.dominio.taxa.TaxaResolvida;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/** Servicos de ponta a ponta com PostgreSQL e Redis reais. */
@SpringBootTest
@Import(ContainersTeste.class)
class ServicosIntegracaoTest {

  private static final OffsetDateTime AGORA =
      OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);

  @Autowired GravacaoDados gravacao;
  @Autowired ServicoConversao servicoConversao;
  @Autowired ServicoCotacao servicoCotacao;
  @Autowired ServicoMoeda servicoMoeda;
  @Autowired ServicoIndicador servicoIndicador;
  @Autowired ServicoIndiceGeopolitico servicoIndice;
  @Autowired ConversaoRepositorio conversoes;
  @Autowired StringRedisTemplate redis;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void preparar() {
    redis.getRequiredConnectionFactory().getConnection().serverCommands().flushAll();
    gravacao.inserirCotacao("USD", "BRL", new BigDecimal("5.3000"), AGORA.minusDays(1), "BCB");
    gravacao.inserirCotacao("USD", "BRL", new BigDecimal("5.4210"), AGORA.minusHours(1), "BCB");
    gravacao.inserirIndice(
        "BR", new BigDecimal("30.00"), new BigDecimal("0.9850"), LocalDate.now(), "BANCO_MUNDIAL");
    gravacao.inserirIndice(
        "US", new BigDecimal("35.98"), new BigDecimal("0.9820"), LocalDate.now(), "BANCO_MUNDIAL");
  }

  @Test
  void exemploCanonico_ponta_a_ponta_gravaAuditoria() {
    long antes = conversoes.count();

    ConversaoRealizada r =
        servicoConversao.converter("USD", "BRL", new BigDecimal("100.00"), true, "abcd1234");

    assertThat(r.taxaCambio()).isEqualByComparingTo("5.4210");
    assertThat(r.fatorGeopolitico()).isEqualTo(new BigDecimal("0.9850"));
    assertThat(r.valorConvertido()).isEqualTo(new BigDecimal("533.97"));
    assertThat(r.fonte()).isEqualTo("BCB");
    assertThat(conversoes.count()).isEqualTo(antes + 1);
    Conversao gravada = conversoes.findAll().stream().reduce((a, b) -> b).orElseThrow();
    assertThat(gravada.getValorConvertido()).isEqualByComparingTo("533.97");
    assertThat(gravada.getApiKeyHashPrefixo()).isEqualTo("abcd1234");
    assertThat(gravada.getFonteId()).isNotNull();
  }

  @Test
  void conversaoSemAjuste_fatorUm() {
    ConversaoRealizada r =
        servicoConversao.converter("USD", "BRL", new BigDecimal("100.00"), false, null);
    assertThat(r.fatorGeopolitico()).isEqualTo(new BigDecimal("1.0000"));
    assertThat(r.valorConvertido()).isEqualTo(new BigDecimal("542.10"));
  }

  @Test
  void conversaoInversaParaPaisDeOrigemDiferente() {
    ConversaoRealizada r =
        servicoConversao.converter("BRL", "USD", new BigDecimal("542.10"), true, null);
    // 1/5.4210 = 0.184468; 542.10 x 0.184468 x 0.9820 = 98.199...
    assertThat(r.taxaCambio()).isEqualTo(new BigDecimal("0.184468"));
    assertThat(r.valorConvertido()).isEqualTo(new BigDecimal("98.20"));
  }

  @Test
  void paisSemIndice_respondeIndisponivel() {
    gravacao.inserirCotacao("USD", "AUD", new BigDecimal("1.5"), AGORA, "BCB");
    assertThatThrownBy(() -> servicoConversao.converter("USD", "AUD", BigDecimal.TEN, true, null))
        .isInstanceOf(DadoIndisponivelException.class)
        .hasMessageContaining("AU");
  }

  @Test
  void moedaDesconhecida_naoEncontrada() {
    assertThatThrownBy(() -> servicoConversao.converter("ARS", "BRL", BigDecimal.TEN, true, null))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessageContaining("ARS");
  }

  @Test
  void gravacaoEIdempotente_historicoNuncaSobrescrito() {
    OffsetDateTime instante = AGORA.minusHours(5);
    assertThat(gravacao.inserirCotacao("GBP", "BRL", new BigDecimal("7.10"), instante, "BCB"))
        .isEqualTo(1);
    assertThat(gravacao.inserirCotacao("GBP", "BRL", new BigDecimal("9.99"), instante, "BCB"))
        .isZero();
    BigDecimal valor =
        jdbc.queryForObject(
            "SELECT c.valor FROM cotacao c JOIN moeda b ON b.id = c.moeda_base_id WHERE b.codigo_iso = 'GBP' AND c.data_hora = ?",
            BigDecimal.class,
            java.sql.Timestamp.from(instante.toInstant()));
    assertThat(valor).isEqualByComparingTo("7.10");

    LocalDate periodo = LocalDate.parse("2025-01-01");
    assertThat(
            gravacao.inserirIndicador(
                "GB", TipoIndicador.INFLACAO, new BigDecimal("3.88"), periodo, "BANCO_MUNDIAL"))
        .isEqualTo(1);
    assertThat(
            gravacao.inserirIndicador(
                "GB", TipoIndicador.INFLACAO, new BigDecimal("3.88"), periodo, "BANCO_MUNDIAL"))
        .isZero();
    assertThat(
            gravacao.inserirIndice(
                "GB", new BigDecimal("29.63"), new BigDecimal("0.9852"), periodo, "BANCO_MUNDIAL"))
        .isEqualTo(1);
    assertThat(
            gravacao.inserirIndice(
                "GB", new BigDecimal("29.63"), new BigDecimal("0.9852"), periodo, "BANCO_MUNDIAL"))
        .isZero();
  }

  @Test
  void cotacaoAtual_usaCacheEInvalidacao() {
    // Par exclusivo deste teste, para nao interferir nos outros (o banco e compartilhado).
    gravacao.inserirCotacao("AUD", "BRL", new BigDecimal("3.5000"), AGORA.minusHours(3), "BCB");
    TaxaResolvida primeira = servicoCotacao.atual("AUD", "BRL");
    assertThat(redis.hasKey(ChavesCache.cotacaoAtual("AUD", "BRL"))).isTrue();

    gravacao.inserirCotacao("AUD", "BRL", new BigDecimal("3.6000"), AGORA.minusMinutes(1), "BCB");
    assertThat(servicoCotacao.atual("AUD", "BRL").valor()).isEqualTo(primeira.valor());

    redis.delete(ChavesCache.cotacaoAtual("AUD", "BRL"));
    assertThat(servicoCotacao.atual("AUD", "BRL").valor()).isEqualByComparingTo("3.6");
  }

  @Test
  void cotacaoCruzadaVindaDoBanco() {
    gravacao.inserirCotacao("EUR", "CNY", new BigDecimal("7.8000"), AGORA.minusHours(2), "BCE");
    gravacao.inserirCotacao("EUR", "MXN", new BigDecimal("19.5000"), AGORA.minusHours(2), "BCE");

    TaxaResolvida t = servicoCotacao.atual("CNY", "MXN");

    assertThat(t.metodo()).isEqualTo(MetodoTaxa.CRUZADA);
    assertThat(t.valor()).isEqualTo(new BigDecimal("2.500000"));
    assertThat(t.fonte()).isEqualTo("Cruzada: BCE");
  }

  @Test
  void historico_validacoesERetorno() {
    LocalDate hoje = AGORA.toLocalDate();
    List<TaxaResolvida> h = servicoCotacao.historico("USD", "BRL", hoje.minusDays(3), hoje);
    assertThat(h).isNotEmpty();
    assertThat(h.getLast().valor()).isNotNull();

    assertThatThrownBy(() -> servicoCotacao.historico("USD", "BRL", hoje, hoje.minusDays(1)))
        .isInstanceOf(RequisicaoInvalidaException.class);
    assertThatThrownBy(() -> servicoCotacao.historico("USD", "BRL", hoje.minusDays(366), hoje))
        .isInstanceOf(RequisicaoInvalidaException.class)
        .hasMessageContaining("366");
    assertThatThrownBy(() -> servicoCotacao.historico("USD", "BRL", null, hoje))
        .isInstanceOf(RequisicaoInvalidaException.class);
    assertThatThrownBy(() ->
            servicoCotacao.historico(
                "USD", "BRL", LocalDate.parse("2001-01-01"), LocalDate.parse("2001-01-31")))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void moedas_listaComPaisECache() {
    List<MoedaCadastrada> moedas = servicoMoeda.listar();
    assertThat(moedas).extracting(MoedaCadastrada::codigo).contains("USD", "EUR", "JPY");
    assertThat(moedas)
        .filteredOn(m -> m.codigo().equals("EUR"))
        .singleElement()
        .extracting(MoedaCadastrada::pais)
        .isEqualTo("EU");
    assertThat(redis.hasKey(ChavesCache.MOEDAS)).isTrue();
  }

  @Test
  void indicadores_maisRecentePorTipo_semEstabilidadePolitica() {
    gravacao.inserirIndicador(
        "CA",
        TipoIndicador.INFLACAO,
        new BigDecimal("3.00"),
        LocalDate.parse("2024-01-01"),
        "BANCO_MUNDIAL");
    gravacao.inserirIndicador(
        "CA",
        TipoIndicador.INFLACAO,
        new BigDecimal("2.07"),
        LocalDate.parse("2025-01-01"),
        "BANCO_MUNDIAL");
    gravacao.inserirIndicador(
        "CA",
        TipoIndicador.PIB,
        new BigDecimal("2200000000000"),
        LocalDate.parse("2025-01-01"),
        "BANCO_MUNDIAL");
    gravacao.inserirIndicador(
        "CA",
        TipoIndicador.ESTABILIDADE_POLITICA,
        new BigDecimal("0.81"),
        LocalDate.parse("2025-01-01"),
        "BANCO_MUNDIAL");

    List<IndicadorResumo> lista = servicoIndicador.listar("CA");

    assertThat(lista)
        .extracting(IndicadorResumo::tipo)
        .containsExactlyInAnyOrder("INFLACAO", "PIB");
    assertThat(lista)
        .filteredOn(i -> i.tipo().equals("INFLACAO"))
        .singleElement()
        .extracting(IndicadorResumo::valor)
        .satisfies(v -> assertThat(v).isEqualByComparingTo("2.07"));
  }

  @Test
  void indicadores_paisInexistenteOuSemDados() {
    assertThatThrownBy(() -> servicoIndicador.listar("ZZ"))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessageContaining("Pais nao encontrado");
    assertThatThrownBy(() -> servicoIndicador.listar("NL"))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessageContaining("Nenhum indicador");
  }

  @Test
  void indice_maisRecente_e404() {
    IndiceAtual br = servicoIndice.atual("BR");
    assertThat(br.fatorAjuste()).isEqualByComparingTo("0.9850");
    assertThat(br.indiceRisco()).isEqualByComparingTo("30.00");
    assertThatThrownBy(() -> servicoIndice.atual("ZZ")).hasMessageContaining("Pais nao encontrado");
    assertThatThrownBy(() -> servicoIndice.atual("FR")).hasMessageContaining("Nenhum indice");
  }
}
