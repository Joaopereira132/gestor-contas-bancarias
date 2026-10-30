package contas;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Conta bancária (classe abstrata): guarda os dados comuns a todos os tipos de conta
 * e implementa depósitos e transferências. Cada subclasse define a sua regra de levantamento
 * (polimorfismo).
 *
 * O saldo só pode ser alterado através de {@link #creditar} e {@link #debitar}, o que
 * impede que uma subclasse (ou outro código) o modifique por engano.
 */
public abstract class Account implements AccountOperations {
    private final String numeroConta;
    private String nomeTitular;
    private Address morada;
    private final String nif;
    private final LocalDate dataNascimento;
    private BigDecimal saldo;

    protected Account(String numeroConta, String nomeTitular, Address morada, String nif,
                      LocalDate dataNascimento, BigDecimal saldoInicial) {
        this.numeroConta = Validacao.textoObrigatorio(numeroConta, "Número da conta");
        setNomeTitular(nomeTitular);
        setMorada(morada);
        this.nif = Validacao.nif(nif);
        this.dataNascimento = Validacao.dataNascimento(dataNascimento, LocalDate.now());
        this.saldo = Validacao.montanteNaoNegativo(saldoInicial, "Saldo inicial");
    }

    public String getNumeroConta() {
        return numeroConta;
    }

    public String getNomeTitular() {
        return nomeTitular;
    }

    public final void setNomeTitular(String nomeTitular) {
        this.nomeTitular = Validacao.textoObrigatorio(nomeTitular, "Nome do titular");
    }

    public Address getMorada() {
        return morada;
    }

    public final void setMorada(Address morada) {
        if (morada == null) {
            throw new IllegalArgumentException("Morada é obrigatória.");
        }
        this.morada = morada;
    }

    public String getNif() {
        return nif;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public final BigDecimal getSaldo() {
        return saldo;
    }

    /** Aumenta o saldo (uso interno; o valor já foi validado pelo chamador). */
    protected final void creditar(BigDecimal valor) {
        saldo = saldo.add(valor);
    }

    /** Diminui o saldo (uso interno; a regra de cada conta já foi verificada pelo chamador). */
    protected final void debitar(BigDecimal valor) {
        saldo = saldo.subtract(valor);
    }

    /** Valor positivo, normalizado a 2 casas decimais; {@code null} se for inválido. */
    protected static BigDecimal valorValido(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            return null;
        }
        return valor.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public boolean depositar(BigDecimal valor) {
        BigDecimal v = valorValido(valor);
        if (v == null || v.signum() == 0) {
            return false;
        }
        creditar(v);
        return true;
    }

    /** Cada tipo de conta define quando um levantamento é permitido. */
    @Override
    public abstract boolean levantar(BigDecimal valor);

    /**
     * Transfere para outra conta. Só deposita no destino se o levantamento na origem
     * tiver sido aceite, por isso o dinheiro nunca é criado nem perdido.
     */
    @Override
    public boolean transferir(Account destino, BigDecimal valor) {
        if (destino == null || destino == this) {
            return false;
        }
        if (!levantar(valor)) {
            return false;
        }
        destino.creditar(valorValido(valor));
        return true;
    }

    @Override
    public String toString() {
        return "Conta Nº: " + numeroConta + ", Titular: " + nomeTitular + ", Morada: " + morada
                + ", Saldo: " + saldo + " €";
    }
}
