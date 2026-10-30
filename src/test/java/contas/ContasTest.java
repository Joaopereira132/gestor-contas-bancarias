package contas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** Testes das regras de negócio do gestor de contas (dinheiro, levantamentos, transferências, validações). */
class ContasTest {

    private static final String NIF_VALIDO = "123456789";
    private static final LocalDate NASCIMENTO = LocalDate.of(1990, 1, 1);

    private static BigDecimal eur(String valor) {
        return new BigDecimal(valor);
    }

    private static Address morada() {
        return new Address("Rua das Flores", "Vila Nova de Gaia", "4400-103");
    }

    private static CheckingAccount contaOrdem(String numero, String saldo, String saldoMinimo) {
        return new CheckingAccount(numero, "Ana Silva", morada(), NIF_VALIDO, NASCIMENTO, eur(saldo), eur(saldoMinimo), false);
    }

    // ---------------------------------------------------------------- validações

    @Test
    void nifValidoEInvalido() {
        assertEquals(NIF_VALIDO, Validacao.nif(NIF_VALIDO));
        assertThrows(IllegalArgumentException.class, () -> Validacao.nif("123456788")); // controlo errado
        assertThrows(IllegalArgumentException.class, () -> Validacao.nif("12345"));      // tamanho errado
        assertThrows(IllegalArgumentException.class, () -> Validacao.nif("12345678a"));  // não numérico
    }

    @Test
    void codigoPostalTemDeSerXxxxHifenXxx() {
        assertEquals("4400-103", Validacao.codigoPostal("4400-103"));
        assertThrows(IllegalArgumentException.class, () -> Validacao.codigoPostal("4400103"));
        assertThrows(IllegalArgumentException.class, () -> new Address("Rua", "Gaia", "abcd-123"));
    }

    @Test
    void titularTemDeSerMaiorDeIdade() {
        LocalDate menor = LocalDate.now().minusYears(17);
        assertThrows(IllegalArgumentException.class,
                () -> new CheckingAccount("1", "Ana", morada(), NIF_VALIDO, menor, eur("10"), eur("0"), false));
    }

    @Test
    void naoPermiteSaldoInicialNegativo() {
        assertThrows(IllegalArgumentException.class, () -> contaOrdem("1", "-1", "0"));
    }

    // ---------------------------------------------------------------- dinheiro

    @Test
    void somasDecimaisSaoExatas() {
        CheckingAccount conta = contaOrdem("1", "0", "0");
        conta.depositar(eur("0.1"));
        conta.depositar(eur("0.2"));
        assertEquals(eur("0.30"), conta.getSaldo()); // com double seria 0.30000000000000004
    }

    @Test
    void depositoInvalidoERecusado() {
        CheckingAccount conta = contaOrdem("1", "100", "0");
        assertFalse(conta.depositar(eur("0")));
        assertFalse(conta.depositar(eur("-5")));
        assertFalse(conta.depositar(null));
        assertEquals(eur("100.00"), conta.getSaldo());
    }

    // ---------------------------------------------------------------- levantamentos (polimorfismo)

    @Test
    void contaOrdemRespeitaSaldoMinimo() {
        CheckingAccount conta = contaOrdem("1", "100", "20");
        assertTrue(conta.levantar(eur("80")));    // fica com 20 = mínimo
        assertFalse(conta.levantar(eur("0.01"))); // ficaria abaixo do mínimo
        assertEquals(eur("20.00"), conta.getSaldo());
    }

    @Test
    void contaSalarioPermiteDescobertoAteAoLimite() {
        SalaryAccount conta = new SalaryAccount("2", "Rui Costa", morada(), NIF_VALIDO, NASCIMENTO,
                eur("100"), eur("0"), false, eur("500"));
        assertTrue(conta.levantar(eur("600")));    // saldo -500 = limite
        assertEquals(eur("-500.00"), conta.getSaldo());
        assertFalse(conta.levantar(eur("0.01")));  // ultrapassa o limite
    }

    @Test
    void contaAPrazoSoPermiteLevantarDepoisDoVencimento() {
        LocalDate criacao = LocalDate.of(2026, 1, 15);
        ContaAPrazo conta = new ContaAPrazo("3", "Eva Pinto", morada(), NIF_VALIDO, NASCIMENTO,
                eur("1000"), 6, criacao, eur("0.05"), false);

        assertEquals(LocalDate.of(2026, 7, 15), conta.getDataVencimento());
        assertFalse(conta.levantar(eur("100"), LocalDate.of(2026, 7, 14))); // antes do vencimento
        assertTrue(conta.levantar(eur("100"), LocalDate.of(2026, 7, 15)));  // no dia do vencimento
        assertEquals(eur("900.00"), conta.getSaldo());
    }

    @Test
    void contaAPrazoExigeDepositoMinimoEJuroCorreto() {
        assertThrows(IllegalArgumentException.class, () -> new ContaAPrazo("3", "Eva", morada(), NIF_VALIDO,
                NASCIMENTO, eur("499.99"), 6, LocalDate.now(), eur("0.05"), false));

        ContaAPrazo conta = new ContaAPrazo("4", "Eva", morada(), NIF_VALIDO, NASCIMENTO,
                eur("1000"), 6, LocalDate.now(), eur("0.05"), false);
        assertEquals(eur("25.00"), conta.calcularJuroAoVencimento()); // 1000 × 5 % × 6/12
    }

    // ---------------------------------------------------------------- gestor e transferências

    @Test
    void naoPermiteNumerosDeContaDuplicados() {
        AccountManager manager = new AccountManager();
        assertTrue(manager.adicionarConta(contaOrdem("1", "10", "0")));
        assertFalse(manager.adicionarConta(contaOrdem("1", "99", "0")));
        assertEquals(1, manager.listarContas().size());
    }

    @Test
    void transferenciaMoveDinheiroSemCriarNemPerder() {
        AccountManager manager = new AccountManager();
        manager.adicionarConta(contaOrdem("A", "100", "0"));
        manager.adicionarConta(contaOrdem("B", "50", "0"));

        assertTrue(manager.transferir("A", "B", eur("30")));

        assertEquals(eur("70.00"), manager.consultarConta("A").get().getSaldo());
        assertEquals(eur("80.00"), manager.consultarConta("B").get().getSaldo());
    }

    @Test
    void transferenciaRecusadaNaoAlteraSaldos() {
        AccountManager manager = new AccountManager();
        manager.adicionarConta(contaOrdem("A", "100", "0"));
        manager.adicionarConta(contaOrdem("B", "50", "0"));

        assertFalse(manager.transferir("A", "B", eur("100.01"))); // saldo insuficiente
        assertFalse(manager.transferir("A", "A", eur("10")));     // para a própria conta
        assertFalse(manager.transferir("A", "X", eur("10")));     // destino inexistente

        assertEquals(eur("100.00"), manager.consultarConta("A").get().getSaldo());
        assertEquals(eur("50.00"), manager.consultarConta("B").get().getSaldo());
    }

    @Test
    void edicaoComCodigoPostalInvalidoNaoAlteraNada() {
        AccountManager manager = new AccountManager();
        manager.adicionarConta(contaOrdem("A", "100", "0"));

        assertThrows(IllegalArgumentException.class,
                () -> manager.modificarConta("A", "Novo Nome", "Rua Nova", "Porto", "invalido"));

        Account conta = manager.consultarConta("A").get();
        assertEquals("Ana Silva", conta.getNomeTitular());
        assertEquals("Rua das Flores", conta.getMorada().getRua());
    }

    @Test
    void edicaoMantemCamposEmBranco() {
        AccountManager manager = new AccountManager();
        manager.adicionarConta(contaOrdem("A", "100", "0"));

        assertTrue(manager.modificarConta("A", "", "Rua Nova", "", ""));

        Account conta = manager.consultarConta("A").get();
        assertEquals("Ana Silva", conta.getNomeTitular());
        assertEquals("Rua Nova", conta.getMorada().getRua());
        assertEquals("4400-103", conta.getMorada().getCodigoPostal());
    }
}
