package SW;

import HW.Memory.Memory;

public class GM {
	public Memory memory;
	public int tamMem;
	public int tamPag;
	public int frames;

	public GM(Memory memory, int tamPag) {
		this.memory = memory;
		this.tamMem = memory.pos.length;
		this.tamPag = tamPag;
		this.frames = tamMem / tamPag;
	}

	public Boolean aloca(int nroPalavras, int[] tabelaPaginas) {
		// retorna true se consegue alocar ou falso caso negativo
		// cada posição i do vetor de saída “tabelaPaginas” informa em que frame a
		// página i deve ser hospedada
		int paginasNecessarias = (int) Math.ceil((double) nroPalavras / tamPag); // A função Math.ceil(x) retorna o
													// menor número inteiro maior ou
																				// igual a "x"
		int paginasAlocadas = 0;
		tabelaPaginas = new int[paginasNecessarias];
		for (int frame = 0; frame < frames; frame++) {
			//System.out.println(paginasAlocadas + " " + paginasNecessarias + " " + tabelaPaginas[frame]);
			if (paginasAlocadas < paginasNecessarias && memory.pos[frame].p == -1) {
				tabelaPaginas[paginasAlocadas] = frame;
				paginasAlocadas++;
				memory.pos[frame].p = 0;
			}
		}

		if (paginasAlocadas == paginasNecessarias) {
			// todas as paginas foram alocadas
			for(int i = 0; i < paginasAlocadas; i++) {
				System.out.println("tabelaPaginas["+i+"] = "+tabelaPaginas[i]);
			}
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