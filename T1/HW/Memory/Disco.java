package HW.Memory;

import java.util.HashMap;
import java.util.Map;

public class Disco {
    private Map<Integer, Word[]> paginas = new HashMap<>();

    public void salvarPagina(int numPagina, Word[] pagina) {
        paginas.put(numPagina, pagina.clone());
    }

    public Word[] recuperaPagina (int numPagina) {
        return paginas.get(numPagina);
    }

    public void removePagina(int numPagina) {
        paginas.remove(numPagina);
    }

    public boolean contemPagina(int numPagina) {
        return paginas.containsKey(numPagina);
    }
}
