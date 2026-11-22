package com.ifsp.gcsw.transacoes.worker.execucao;

import com.ifsp.gcsw.transacoes.worker.job.Job;
import com.ifsp.gcsw.transacoes.worker.job.ResultadoJob;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Executa um job registrando metricas (lidos, gravados, ignorados, descartados, falhas, duracao) e
 * o instante do ultimo sucesso, usado pelo alarme de dado desatualizado.
 */
@Component
public class ExecutorJobs {

  private static final Logger LOG = LoggerFactory.getLogger(ExecutorJobs.class);

  private final Map<String, Job> jobs;
  private final MeterRegistry metricas;
  private final Clock relogio;
  private final Map<String, AtomicLong> ultimoSucesso = new ConcurrentHashMap<>();

  public ExecutorJobs(List<Job> jobs, MeterRegistry metricas, Clock relogio) {
    this.jobs = jobs.stream().collect(Collectors.toMap(Job::nome, Function.identity()));
    this.metricas = metricas;
    this.relogio = relogio;
  }

  public Optional<Job> buscar(String nome) {
    return Optional.ofNullable(jobs.get(nome));
  }

  public java.util.Set<String> nomes() {
    return new java.util.TreeSet<>(jobs.keySet());
  }

  public ResultadoJob executar(String nome) {
    Job job =
        buscar(nome)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Job desconhecido: " + nome + ". Disponiveis: " + nomes()));
    MDC.put("job", nome);
    Timer.Sample amostra = Timer.start(metricas);
    ResultadoJob resultado;
    try {
      resultado = job.executar();
    } catch (RuntimeException e) {
      LOG.error("Job {} falhou: {}", nome, e.getMessage(), e);
      resultado = ResultadoJob.falha();
    }
    long nanos =
        amostra.stop(Timer.builder("worker.job.duracao").tag("job", nome).register(metricas));
    registrar(nome, resultado);
    LOG.info(
        "Job {} concluido: lidos={} gravados={} ignorados={} descartados={} falhas={} duracaoMs={}",
        nome,
        resultado.lidos(),
        resultado.gravados(),
        resultado.ignorados(),
        resultado.descartados(),
        resultado.falhas(),
        nanos / 1_000_000);
    MDC.remove("job");
    return resultado;
  }

  private void registrar(String nome, ResultadoJob r) {
    contador(nome, "lidos").increment(r.lidos());
    contador(nome, "gravados").increment(r.gravados());
    contador(nome, "ignorados").increment(r.ignorados());
    contador(nome, "descartados").increment(r.descartados());
    Counter.builder("worker.job.falhas").tag("job", nome).register(metricas).increment(r.falhas());
    if (r.falhas() == 0) {
      AtomicLong instante =
          ultimoSucesso.computeIfAbsent(
              nome,
              n -> {
                AtomicLong valor = new AtomicLong();
                Gauge.builder("worker.job.ultimo.sucesso", valor, AtomicLong::get)
                    .tag("job", n)
                    .description("Epoch em segundos do ultimo sucesso")
                    .register(metricas);
                return valor;
              });
      instante.set(relogio.instant().getEpochSecond());
    }
  }

  private Counter contador(String job, String tipo) {
    return Counter.builder("worker.job.registros")
        .tag("job", job)
        .tag("tipo", tipo)
        .register(metricas);
  }
}
