package contas;

import java.math.BigDecimal;

/**
 * Operações básicas de uma conta bancária.
 * Todas devolvem {@code true} se a operação foi realizada e {@code false} se foi recusada.
 */
public interface AccountOperations {
    boolean depositar(BigDecimal valor);

    boolean levantar(BigDecimal valor);

    boolean transferir(Account destino, BigDecimal valor);
}
