package contas;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Conta à ordem: o levantamento só é permitido se o saldo final não ficar abaixo do saldo mínimo.
 */
public class CheckingAccount extends Account implements CreditCardHolder {
    private BigDecimal saldoMinimo;
    private boolean temCartaoCredito;

    public CheckingAccount(String numeroConta, String nomeTitular, Address morada, String nif,
                           LocalDate dataNascimento, BigDecimal saldo, BigDecimal saldoMinimo,
                           boolean temCartaoCredito) {
        super(numeroConta, nomeTitular, morada, nif, dataNascimento, saldo);
        setSaldoMinimo(saldoMinimo);
        this.temCartaoCredito = temCartaoCredito;
    }

    public BigDecimal getSaldoMinimo() {
        return saldoMinimo;
    }

    public final void setSaldoMinimo(BigDecimal saldoMinimo) {
        this.saldoMinimo = Validacao.montanteNaoNegativo(saldoMinimo, "Saldo mínimo");
    }

    public boolean isTemCartaoCredito() {
        return temCartaoCredito;
    }

    public void setTemCartaoCredito(boolean temCartaoCredito) {
        this.temCartaoCredito = temCartaoCredito;
    }

    @Override
    public boolean hasCreditCard() {
        return temCartaoCredito;
    }

    /** Menor saldo que a conta pode ter depois de um levantamento. */
    protected BigDecimal limiteInferior() {
        return saldoMinimo;
    }

    @Override
    public boolean levantar(BigDecimal valor) {
        BigDecimal v = valorValido(valor);
        if (v == null || getSaldo().subtract(v).compareTo(limiteInferior()) < 0) {
            return false;
        }
        debitar(v);
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + ", Saldo Mínimo: " + saldoMinimo + " €, Cartão de Crédito: "
                + (temCartaoCredito ? "Sim" : "Não");
    }
}
