package contas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

/**
 * Regras de validação reutilizadas pelas várias classes do domínio.
 * Centralizá-las evita duplicar lógica e garante mensagens de erro consistentes.
 */
public final class Validacao {

    /** Idade mínima para ser titular de uma conta. */
    public static final int IDADE_MINIMA = 18;

    private Validacao() {
        // classe utilitária: não instanciável
    }

    /** Garante que o texto não é nulo nem vazio e devolve-o sem espaços nas pontas. */
    public static String textoObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        return valor.trim();
    }

    /**
     * Normaliza um montante para 2 casas decimais (arredondamento comercial).
     * Os valores monetários usam BigDecimal porque double não representa
     * corretamente decimais como 0.1 (0.1 + 0.2 != 0.3).
     */
    public static BigDecimal montante(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    /** Montante que tem de ser maior ou igual a zero. */
    public static BigDecimal montanteNaoNegativo(BigDecimal valor, String campo) {
        BigDecimal normalizado = montante(valor, campo);
        if (normalizado.signum() < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo.");
        }
        return normalizado;
    }

    /**
     * Valida um NIF português: 9 dígitos, primeiro dígito válido e dígito de controlo
     * (módulo 11) correto.
     */
    public static String nif(String valor) {
        String nif = textoObrigatorio(valor, "NIF");
        // Primeiro dígito: 1, 2, 3, 5, 6, 7, 8, 9 ou o prefixo 45 (não residentes).
        boolean inicioValido = !nif.isEmpty() && nif.charAt(0) != '0' && (nif.charAt(0) != '4' || nif.startsWith("45"));
        if (!nif.matches("\\d{9}") || !inicioValido) {
            throw new IllegalArgumentException("NIF inválido: deve ter 9 dígitos.");
        }
        int soma = 0;
        for (int i = 0; i < 8; i++) {
            soma += Character.getNumericValue(nif.charAt(i)) * (9 - i);
        }
        int controlo = 11 - (soma % 11);
        if (controlo >= 10) {
            controlo = 0;
        }
        if (controlo != Character.getNumericValue(nif.charAt(8))) {
            throw new IllegalArgumentException("NIF inválido: dígito de controlo incorreto.");
        }
        return nif;
    }

    /** Data de nascimento no passado e correspondente a um titular maior de idade. */
    public static LocalDate dataNascimento(LocalDate data, LocalDate hoje) {
        if (data == null) {
            throw new IllegalArgumentException("Data de nascimento é obrigatória.");
        }
        if (data.isAfter(hoje)) {
            throw new IllegalArgumentException("Data de nascimento não pode estar no futuro.");
        }
        if (Period.between(data, hoje).getYears() < IDADE_MINIMA) {
            throw new IllegalArgumentException("O titular deve ter pelo menos " + IDADE_MINIMA + " anos.");
        }
        return data;
    }

    /** Código postal português no formato XXXX-XXX. */
    public static String codigoPostal(String valor) {
        String cp = textoObrigatorio(valor, "Código postal");
        if (!cp.matches("\\d{4}-\\d{3}")) {
            throw new IllegalArgumentException("Código postal inválido. Formato esperado: XXXX-XXX.");
        }
        return cp;
    }
}
