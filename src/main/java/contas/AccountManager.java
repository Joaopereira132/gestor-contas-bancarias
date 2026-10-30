package contas;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gere a coleção de contas em memória: criar, consultar, alterar, remover e movimentar.
 * As contas são guardadas num mapa (número da conta → conta), o que dá pesquisa direta
 * e impede números de conta duplicados.
 */
public class AccountManager {
    private final Map<String, Account> contas = new LinkedHashMap<>();

    /** @return false se já existir uma conta com o mesmo número */
    public boolean adicionarConta(Account conta) {
        return contas.putIfAbsent(conta.getNumeroConta(), conta) == null;
    }

    public boolean removerConta(String numeroConta) {
        return contas.remove(numeroConta) != null;
    }

    public Optional<Account> consultarConta(String numeroConta) {
        return Optional.ofNullable(contas.get(numeroConta));
    }

    /**
     * Atualiza o nome e a morada. Campos em branco mantêm o valor atual.
     * @throws IllegalArgumentException se o código postal for inválido
     */
    public boolean modificarConta(String numeroConta, String novoNome, String rua, String localidade, String codigoPostal) {
        Optional<Account> conta = consultarConta(numeroConta);
        if (conta.isEmpty()) {
            return false;
        }
        Account c = conta.get();
        // Valida primeiro e só depois altera, para não deixar a conta a meio de uma atualização.
        Address atual = c.getMorada();
        Address nova = new Address(
                emBrancoOu(rua, atual.getRua()),
                emBrancoOu(localidade, atual.getLocalidade()),
                emBrancoOu(codigoPostal, atual.getCodigoPostal()));
        c.setNomeTitular(emBrancoOu(novoNome, c.getNomeTitular()));
        c.setMorada(nova);
        return true;
    }

    private static String emBrancoOu(String novo, String atual) {
        return (novo == null || novo.isBlank()) ? atual : novo;
    }

    public boolean depositar(String numeroConta, BigDecimal valor) {
        return consultarConta(numeroConta).map(c -> c.depositar(valor)).orElse(false);
    }

    public boolean levantar(String numeroConta, BigDecimal valor) {
        return consultarConta(numeroConta).map(c -> c.levantar(valor)).orElse(false);
    }

    public boolean transferir(String origem, String destino, BigDecimal valor) {
        Optional<Account> o = consultarConta(origem);
        Optional<Account> d = consultarConta(destino);
        return o.isPresent() && d.isPresent() && o.get().transferir(d.get(), valor);
    }

    /** Cópia da lista de contas (a lista interna não é exposta). */
    public List<Account> listarContas() {
        return new ArrayList<>(contas.values());
    }
}
