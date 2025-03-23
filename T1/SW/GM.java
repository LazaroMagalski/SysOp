package SW;

import HW.Memory.Memory;

public class GM {
	public Memory memory;
	public int tamMem;
	public int tamPag;

	public GM(Memory memory, int tamMem, int tamPag) {
		this.memory = memory;
		this.tamMem = tamMem;
		this.tamPag = tamPag;
	}

	public Boolean aloca(int nroPalavras, int[] tabelaPaginas) {
		// retorna true se consegue alocar ou falso caso negativo
		// cada posição i do vetor de saída “tabelaPaginas” informa em que frame a
		// página i deve ser hospedada
		int paginasNecessarias = (int) Math.ceil((double) nroPalavras / tamPag); // A função Math.ceil(x) retorna o
		int frames = tamMem / tamPag;																	// menor número inteiro maior ou
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

	public int traduz(int posicaoLogica, int[] particoes) {
		int tamParticao = particoes.length;//checar se é isso mesmo
		int p = posicaoLogica / tamParticao; 
		int offset = posicaoLogica % tamParticao; 
		int frameNaMemoria = particoes[p]; 
		return frameNaMemoria * tamParticao + offset;
	}
}