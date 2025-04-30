package SW;

import HW.Memory.Memory;
import HW.Memory.Word;
import java.util.Stack;
// -------------------------------------------------------------------------------------------------------
// --------------------- M E M O R I A - definicoes de palavra de memoria,
// memória ----------------------
public class GM {
	public Memory memory;
	public int tamMem;
	public static int tamPag;
	public int frames;
	private Stack<Integer> freeFrames;

	public GM(Memory memory, int _tamPag) {
		this.memory = memory;
		this.tamMem = memory.pos.length;
		tamPag = _tamPag;
		this.frames = tamMem / tamPag;
		freeFrames = new Stack<>();
		for (int i = frames-1; i >= 0; i--) {
			freeFrames.push(i);
		}

		// Frames já começam alocados
		for (int i = 0; i < 30; i++){
		freeFrames.removeElement(i);
		freeFrames.removeElement(i+100);
		
		}
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
		for (int i = 0; i < paginasNecessarias; i++) {
			//System.out.println(paginasAlocadas + " " + paginasNecessarias + " " + tabelaPaginas[frame]);
			try{
			tabelaPaginas[paginasAlocadas] = freeFrames.pop();

			paginasAlocadas++;
			} catch (java.util.EmptyStackException e) {
				System.out.println("Não há mais frames disponíveis para alocação");
				break;
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
			freeFrames.push(tabelaPaginas[i]);
		}
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