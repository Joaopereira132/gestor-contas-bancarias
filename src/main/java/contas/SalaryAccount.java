package contas;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Conta salário: é uma conta à ordem que permite saldo negativo até ao limite de descoberto.
 * Só precisa de redefinir o limite inferior; a lógica de levantamento é herdada.
 */
public class SalaryAccount extends CheckingAccount {
    private BigDecimal limiteDescoberto;

    public SalaryAccount(String numeroConta, String nomeTitular, Address morada, String nif,
                         LocalDate dataNascimento, BigDecimal saldo, BigDecimal saldoMinimo,
                         boolean temCartaoCredito, BigDecimal limiteDescoberto) {
        super(numeroConta, nomeTitular, morada, nif, dataNascimento, saldo, saldoMinimo, temCartaoCredito);
        setLimiteDescoberto(limiteDescoberto);
    }

    public BigDecimal getLimiteDescoberto() {
        return limiteDescoberto;
    }

    public final void setLimiteDescoberto(BigDecimal limiteDescoberto) {
        this.limiteDescoberto = Validacao.montanteNaoNegativo(limiteDescoberto, "Limite de descoberto");
    }

    @Override
    protected BigDecimal limiteInferior() {
        return limiteDescoberto.negate();
    }

    @Override
    public String toString() {
        return super.toString() + ", Limite Descoberto: " + limiteDescoberto + " €";
    }
}
