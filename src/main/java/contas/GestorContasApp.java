package contas;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Interface gráfica (Swing) do gestor de contas.
 * A janela só trata de interação com o utilizador; todas as regras de negócio
 * estão nas classes do domínio ({@link AccountManager}, {@link Account}...).
 */
public class GestorContasApp extends JFrame {

    private static final String CONTA_A_PRAZO = "Conta a prazo";
    private static final String CONTA_A_ORDEM = "Conta à ordem";
    private static final BigDecimal DEPOSITO_INICIAL_ORDEM = new BigDecimal("250.00");

    private static final long serialVersionUID = 1L;

    private final transient AccountManager manager = new AccountManager();

    public GestorContasApp() {
        super("Gestor de Contas Bancárias");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);

        JPanel botoes = new JPanel(new GridLayout(8, 1, 5, 5));
        adicionarBotao(botoes, "Criar conta", this::criarConta);
        adicionarBotao(botoes, "Depositar", this::depositar);
        adicionarBotao(botoes, "Levantar", this::levantar);
        adicionarBotao(botoes, "Transferir", this::transferir);
        adicionarBotao(botoes, "Editar conta", this::editarConta);
        adicionarBotao(botoes, "Remover conta", this::removerConta);
        adicionarBotao(botoes, "Listar contas", () -> mostrarTexto("Lista de contas", listagemDeContas()));
        adicionarBotao(botoes, "Sair", () -> System.exit(0));

        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(botoes, BorderLayout.WEST);
    }

    private static void adicionarBotao(JPanel painel, String texto, Runnable acao) {
        JButton botao = new JButton(texto);
        botao.addActionListener(e -> acao.run());
        painel.add(botao);
    }

    // ------------------------------------------------------------------ entrada de dados

    /** Pede um texto; devolve null se o utilizador cancelar ou deixar em branco. */
    private String pedir(String mensagem) {
        String valor = JOptionPane.showInputDialog(this, mensagem);
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    /** Pede um montante (aceita vírgula ou ponto); devolve null se cancelado ou inválido. */
    private BigDecimal pedirMontante(String mensagem) {
        String texto = pedir(mensagem);
        if (texto == null) {
            return null;
        }
        try {
            return new BigDecimal(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            mostrarErro("Valor inválido.");
            return null;
        }
    }

    private boolean confirmar(String mensagem) {
        return JOptionPane.showConfirmDialog(this, mensagem, "Confirmar", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    private void mostrarErro(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarTexto(String titulo, String texto) {
        JTextArea area = new JTextArea(20, 50);
        area.setText(texto);
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    // ------------------------------------------------------------------ casos de uso

    private void criarConta() {
        String id = pedir("ID da conta:");
        String nome = id == null ? null : pedir("Nome do titular:");
        String rua = nome == null ? null : pedir("Rua:");
        String localidade = rua == null ? null : pedir("Localidade:");
        String codigoPostal = localidade == null ? null : pedir("Código postal (XXXX-XXX):");
        String nif = codigoPostal == null ? null : pedir("NIF:");
        String nascimento = nif == null ? null : pedir("Data de nascimento (AAAA-MM-DD):");
        if (nascimento == null) {
            return; // o utilizador cancelou algum passo
        }

        String[] tipos = {CONTA_A_PRAZO, CONTA_A_ORDEM};
        int escolha = JOptionPane.showOptionDialog(this, "Tipo de conta:", "Selecionar tipo de conta",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, tipos, tipos[0]);
        if (escolha == JOptionPane.CLOSED_OPTION) {
            return;
        }

        try {
            Address morada = new Address(rua, localidade, codigoPostal);
            LocalDate dataNascimento = LocalDate.parse(nascimento);
            Account conta = tipos[escolha].equals(CONTA_A_PRAZO)
                    ? criarContaAPrazo(id, nome, morada, nif, dataNascimento)
                    : criarContaAOrdem(id, nome, morada, nif, dataNascimento);
            if (conta == null) {
                return; // cancelado durante os passos específicos do tipo de conta
            }
            if (manager.adicionarConta(conta)) {
                mostrarTexto("Criar conta", "Conta criada com sucesso!\n" + conta);
            } else {
                mostrarErro("Já existe uma conta com o número " + id + ".");
            }
        } catch (DateTimeParseException e) {
            mostrarErro("Data de nascimento inválida. Usa o formato AAAA-MM-DD.");
        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage()); // mensagens de validação do domínio
        }
    }

    private Account criarContaAPrazo(String id, String nome, Address morada, String nif, LocalDate nascimento) {
        String prazoTexto = pedir("Prazo (meses):");
        if (prazoTexto == null) {
            return null;
        }
        int prazoMeses;
        try {
            prazoMeses = Integer.parseInt(prazoTexto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Prazo inválido.");
        }
        BigDecimal taxa = pedirMontante("Taxa de juro anual (ex.: 0.05 para 5 %):");
        BigDecimal montante = taxa == null ? null : pedirMontante("Montante inicial (mínimo " + ContaAPrazo.DEPOSITO_MINIMO + " €):");
        if (montante == null) {
            return null;
        }
        boolean renovar = confirmar("Renovação automática?");
        return new ContaAPrazo(id, nome, morada, nif, nascimento, montante, prazoMeses, LocalDate.now(), taxa, renovar);
    }

    private Account criarContaAOrdem(String id, String nome, Address morada, String nif, LocalDate nascimento) {
        boolean cartao = confirmar("Quer cartão?");
        if (!confirmar("Quer que a conta seja Conta Salário?")) {
            return new CheckingAccount(id, nome, morada, nif, nascimento, DEPOSITO_INICIAL_ORDEM, BigDecimal.ZERO, cartao);
        }
        BigDecimal limite = pedirMontante("Limite de descoberto (ex.: 500):");
        if (limite == null) {
            return null;
        }
        return new SalaryAccount(id, nome, morada, nif, nascimento, DEPOSITO_INICIAL_ORDEM, BigDecimal.ZERO, cartao, limite);
    }

    private void depositar() {
        String id = pedir("ID da conta para depósito:");
        BigDecimal valor = id == null ? null : pedirMontante("Valor para depósito:");
        if (valor == null) {
            return;
        }
        boolean ok = manager.depositar(id, valor);
        mostrarTexto("Depósito", (ok ? "Depósito efetuado com sucesso.\n" : "Depósito recusado (conta inexistente ou valor inválido).\n")
                + descricao(id));
    }

    private void levantar() {
        String id = pedir("ID da conta para levantamento:");
        BigDecimal valor = id == null ? null : pedirMontante("Valor para levantamento:");
        if (valor == null) {
            return;
        }
        boolean ok = manager.levantar(id, valor);
        mostrarTexto("Levantamento", (ok ? "Levantamento efetuado com sucesso.\n" : "Levantamento recusado (saldo/limite insuficiente, prazo por vencer ou conta inexistente).\n")
                + descricao(id));
    }

    private void transferir() {
        String origem = pedir("ID da conta de origem:");
        String destino = origem == null ? null : pedir("ID da conta de destino:");
        BigDecimal valor = destino == null ? null : pedirMontante("Valor para transferência:");
        if (valor == null) {
            return;
        }
        boolean ok = manager.transferir(origem, destino, valor);
        mostrarTexto("Transferência", (ok ? "Transferência efetuada com sucesso.\n" : "Transferência recusada.\n")
                + descricao(origem) + descricao(destino));
    }

    private void editarConta() {
        String id = pedir("ID da conta a modificar:");
        if (id == null) {
            return;
        }
        // Campos em branco mantêm o valor atual.
        String nome = JOptionPane.showInputDialog(this, "Novo nome (vazio = manter):");
        String rua = JOptionPane.showInputDialog(this, "Nova rua (vazio = manter):");
        String localidade = JOptionPane.showInputDialog(this, "Nova localidade (vazio = manter):");
        String codigoPostal = JOptionPane.showInputDialog(this, "Novo código postal (vazio = manter):");
        try {
            boolean ok = manager.modificarConta(id, nome, rua, localidade, codigoPostal);
            mostrarTexto("Editar conta " + id, (ok ? "Alteração efetuada com sucesso.\n" : "Conta inexistente.\n") + descricao(id));
        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());
        }
    }

    private void removerConta() {
        String id = pedir("ID da conta a remover:");
        if (id != null) {
            mostrarTexto("Remover conta " + id,
                    manager.removerConta(id) ? "Conta removida com sucesso.\n" : "Conta inexistente.\n");
        }
    }

    // ------------------------------------------------------------------ apresentação

    private String descricao(String id) {
        Optional<Account> conta = manager.consultarConta(id);
        return conta.map(c -> c + "\n").orElse("");
    }

    private String listagemDeContas() {
        List<Account> contas = manager.listarContas();
        StringBuilder sb = new StringBuilder();

        sb.append("=== Contas a Prazo ===\n");
        contas.stream().filter(c -> c instanceof ContaAPrazo).forEach(c -> sb.append(c).append('\n'));

        // As contas salário são também contas à ordem; separa-se para as mostrar à parte.
        sb.append("\n=== Contas à Ordem ===\n");
        contas.stream().filter(c -> c instanceof CheckingAccount && !(c instanceof SalaryAccount))
                .forEach(c -> sb.append(c).append('\n'));

        sb.append("\n=== Contas Salário ===\n");
        contas.stream().filter(c -> c instanceof SalaryAccount).forEach(c -> sb.append(c).append('\n'));
        return sb.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GestorContasApp().setVisible(true));
    }
}
