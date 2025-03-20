// -------------------------------------------------------------------------------------------------------
// --------------------- M E M O R I A - definicoes de palavra de memoria,
// memória ----------------------
public class GM {
	public int tamMem;
	public int tamPag;
	public int frames = tamMem / tamPag;

	public Boolean aloca(int nroPalavras, int[] tabelaPaginas) {
		// retorna true se consegue alocar ou falso caso negativo
		// cada posição i do vetor de saída “tabelaPaginas” informa em que frame a
		// página i deve ser hospedada

		int paginasNecessarias = (int) Math.ceil((double) nroPalavras / tamPag); // A função Math.ceil(x) retorna o
																					// menor número inteiro maior ou
																					// igual a "x"
		int paginasAlocadas = 0;

		for (int frame = 0; frame < frames; frame++) {
			if (paginasAlocadas < paginasNecessarias && tabelaPaginas[frame] == -1) {
				tabelaPaginas[paginasAlocadas] = frame;
				paginasAlocadas++;
			}
		}

		if (paginasAlocadas == paginasNecessarias) {
			// todas as paginas foram alocadas
			return true;
		} else {
			// libera os espaços que já foram alocados
			for (int i = 0; i < paginasAlocadas; i++) {
				tabelaPaginas[i] = -1;
			}
			return false;
		}
	}

	public void desaloca(int[] tabelaPaginas) {
		// libera os frames alocados

		for (int i = 0; i < tabelaPaginas.length; i++) {
			tabelaPaginas[i] = -1;
		}
		return;
	}
}