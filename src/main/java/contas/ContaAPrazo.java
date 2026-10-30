package contas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Conta a prazo: o capital só pode ser levantado depois do vencimento.
 * O prazo é expresso em meses.
 */
public class ContaAPrazo extends Account {
    /** Depósito inicial mínimo exigido. */
    public static final BigDecimal DEPOSITO_MINIMO = new BigDecimal("500.00");

    private final int prazoMeses;
    private final LocalDate dataCriacao;
    private final BigDecimal taxaJuroAnual; // ex.: 0.05 = 5 % ao ano
    private boolean renovacaoAutomatica;

    public ContaAPrazo(String numeroConta, String nomeTitular, Address morada, String nif,
                       LocalDate dataNascimento, BigDecimal saldo, int prazoMeses, LocalDate dataCriacao,
                       BigDecimal taxaJuroAnual, boolean renovacaoAutomatica) {
        super(numeroConta, nomeTitular, morada, nif, dataNascimento, saldo);
        if (getSaldo().compareTo(DEPOSITO_MINIMO) < 0) {
            throw new IllegalArgumentException("O depósito inicial deve ser no mínimo " + DEPOSITO_MINIMO + " €.");
        }
        if (prazoMeses <= 0) {
            throw new IllegalArgumentException("O prazo deve ser superior a 0 meses.");
        }
        if (dataCriacao == null) {
            throw new IllegalArgumentException("Data de criação é obrigatória.");
        }
        if (taxaJuroAnual == null || taxaJuroAnual.signum() < 0) {
            throw new IllegalArgumentException("A taxa de juro não pode ser negativa.");
        }
        this.prazoMeses = prazoMeses;
        this.dataCriacao = dataCriacao;
        this.taxaJuroAnual = taxaJuroAnual;
        this.renovacaoAutomatica = renovacaoAutomatica;
    }

    public int getPrazoMeses() {
        return prazoMeses;
    }

    public LocalDate getDataCriacao() {
        return dataCriacao;
    }

    public BigDecimal getTaxaJuroAnual() {
        return taxaJuroAnual;
    }

    public boolean isRenovacaoAutomatica() {
        return renovacaoAutomatica;
    }

    public void setRenovacaoAutomatica(boolean renovacaoAutomatica) {
        this.renovacaoAutomatica = renovacaoAutomatica;
    }

    public LocalDate getDataVencimento() {
        return dataCriacao.plusMonths(prazoMeses);
    }

    public boolean isVencida(LocalDate hoje) {
        return !hoje.isBefore(getDataVencimento());
    }

    /** Juro simples devido ao vencimento: capital × taxa anual × (meses / 12). */
    public BigDecimal calcularJuroAoVencimento() {
        return getSaldo()
                .multiply(taxaJuroAnual)
                .multiply(BigDecimal.valueOf(prazoMeses))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    /** Levantamento permitido apenas depois do vencimento (e com saldo suficiente). */
    @Override
    public boolean levantar(BigDecimal valor) {
        return levantar(valor, LocalDate.now());
    }

    /** Variante com a data explícita (facilita os testes). */
    public boolean levantar(BigDecimal valor, LocalDate hoje) {
        BigDecimal v = valorValido(valor);
        if (v == null || !isVencida(hoje) || getSaldo().compareTo(v) < 0) {
            return false;
        }
        debitar(v);
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + ", Prazo: " + prazoMeses + " meses, Data Criação: " + dataCriacao
                + ", Vencimento: " + getDataVencimento() + ", Taxa Juro: " + taxaJuroAnual;
    }
}
