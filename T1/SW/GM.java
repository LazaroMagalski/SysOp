package SW;

import HW.CPU.Opcode;
import HW.Memory.Memory;
import HW.Memory.Word;
// -------------------------------------------------------------------------------------------------------
// --------------------- M E M O R I A - definicoes de palavra de memoria,
// memória ----------------------
public class GM {
	public Memory memory;
	public int tamMem;
	public static int tamPag;
	public int frames;

	public GM(Memory memory, int _tamPag) {
		this.memory = memory;
		this.tamMem = memory.pos.length;
		tamPag = _tamPag;
		this.frames = tamMem / tamPag;
	}

	public int[] aloca(int nroPalavras) {
		// retorna true se consegue alocar ou falso caso negativo
		// cada posição i do vetor de saída “tabelaPaginas” informa em que frame a
		// página i deve ser hospedada
		int paginasNecessarias = (int) Math.ceil((double) nroPalavras / tamPag); // A função Math.ceil(x) retorna o
													// menor número inteiro maior ou
																				// igual a "x"
		int paginasAlocadas = 0;
		int[] tabelaPaginas = new int[paginasNecessarias];
		for (int frame = 0; frame < frames; frame++) {
			//System.out.println(paginasAlocadas + " " + paginasNecessarias + " " + tabelaPaginas[frame]);
			
			int positionFrame = frame * tamPag;
			
			if (paginasAlocadas < paginasNecessarias && memory.pos[positionFrame].p == -1) {
				tabelaPaginas[paginasAlocadas] = frame;
				
				for (int index = 0; index < tamPag; index++) {
					memory.pos[positionFrame + index].p = 0;
					memory.pos[positionFrame + index].opc = Opcode.DATA;
				}

				paginasAlocadas++;
			}
		}

		if (paginasAlocadas == paginasNecessarias) {
			// todas as paginas foram alocadas
			return tabelaPaginas;
		} else {
			// libera os espaços que já foram alocados
			// for (int i = 0; i < paginasAlocadas; i++) {
			// 	tabelaPaginas[i] = -1;
			// }
			return null;
		}
	}

	public void desaloca(int[] tabelaPaginas) {
		// libera os frames alocados

		for (int i = 0; i < tabelaPaginas.length; i++) {
			for(int j =0; j < tamPag; j++){
				memory.pos[i*tamPag+j].p = -1;
			}
		}
		return;
	}

	public void carregarPrograma (Word[] programa, int[] tabelaPaginas) {
		// Carrega o programa na memória
		for (int i = 0; i < programa.length;i++) {

			int posicaoTransladada = tradutor(i, tabelaPaginas);
			memory.pos[posicaoTransladada].opc = programa[i].opc;
			memory.pos[posicaoTransladada].p = programa[i].p;
			memory.pos[posicaoTransladada].ra = programa[i].ra;
			memory.pos[posicaoTransladada].rb = programa[i].rb;

		}
	}

	public static int tradutor(int enderecoLogico, int[] tabelaPaginas){
		int numPagina = enderecoLogico / tamPag; // Pega o número da página virtual
		int offset = enderecoLogico % tamPag; // Descobre o deslocamento dentro da página

		if (enderecoLogico < 0 || tabelaPaginas == null) {	// Verifica a validade dos argumentos fornecidos
			System.out.println("Endereço ou tabela inválidos");
		}

		if (numPagina >= tabelaPaginas.length || tabelaPaginas[numPagina] == -1) {
			System.out.println("Página "+ 1 + " não existe/não foi alocada na memória RAM");
			return -1;
		}

		int numFrame = tabelaPaginas[numPagina]; // Salva o frame físico correspondente
		int enderecoFisico = (numFrame * tamPag) + offset; // Descobre o endereco físico

		return enderecoFisico;
	}

}