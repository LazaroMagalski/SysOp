package HW.Memory;

import java.util.HashMap;
import java.util.Map;

public class Disco {
    private Map<Integer, Word[]> paginas = new HashMap<>();

    public void salvarPagina(int numPagina, Word[] pagina) {
        Word[] copia = new Word[pagina.length];
        for (int i = 0; i < pagina.length; i++) {
            copia[i] = new Word(pagina[i]); 
        }
        paginas.put(numPagina, copia);

    }

    public Word[] recuperaPagina(int numPagina) {
        return paginas.get(numPagina);
    }

    public void removePagina(int numPagina) {
        paginas.remove(numPagina);
    }

    public boolean contemPagina(int numPagina) {
        return paginas.containsKey(numPagina);
    }
}
