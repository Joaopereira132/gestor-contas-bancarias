package contas;

/**
 * Morada composta por rua, localidade e código postal (formato XXXX-XXX).
 * Todos os campos são validados, tanto na construção como nos setters.
 */
public class Address {
    private String rua;
    private String localidade;
    private String codigoPostal;

    public Address(String rua, String localidade, String codigoPostal) {
        setRua(rua);
        setLocalidade(localidade);
        setCodigoPostal(codigoPostal);
    }

    public String getRua() {
        return rua;
    }

    public final void setRua(String rua) {
        this.rua = Validacao.textoObrigatorio(rua, "Rua");
    }

    public String getLocalidade() {
        return localidade;
    }

    public final void setLocalidade(String localidade) {
        this.localidade = Validacao.textoObrigatorio(localidade, "Localidade");
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public final void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = Validacao.codigoPostal(codigoPostal);
    }

    @Override
    public String toString() {
        return rua + ", " + localidade + ", " + codigoPostal;
    }
}
