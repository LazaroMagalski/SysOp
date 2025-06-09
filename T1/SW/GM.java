package SW;

import java.util.Stack;

import HW.Memory.Memory;
import HW.Memory.Word;
// -------------------------------------------------------------------------------------------------------
// --------------------- M E M O R I A - definicoes de palavra de memoria,
// memória ----------------------
public class GM {
	public Memory memory;
	public int tamMem;
	public int tamPag;
	public int frames;
	private Stack<Integer> freeFrames;

	public GM(Memory memory, int _tamPag) {
		this.memory = memory;
		this.tamMem = memory.pos.length;
		this.tamPag = _tamPag;
		this.frames = this.tamMem / this.tamPag;
		freeFrames = new Stack<>();
		for (int i = frames-1; i >= 0; i--) { // i-- para incluir todos os frames
			freeFrames.push(i);
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
			
			tabelaPaginas[paginasAlocadas] = freeFrames.pop();

			paginasAlocadas++;
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
		System.out.println("GM.carregarPrograma: Carregando programa de " + programa.length + " palavras.");
        for (int i = 0; i < programa.length;i++) {
            // Chamando o tradutor estático com o tamPag da instância
            int posicaoTransladada = GM.tradutor(i, tabelaPaginas, this.tamPag); // <--- PASSA this.tamPag
            
			if (posicaoTransladada != -1) {
                System.out.println("  Carregando: Logico " + i + " -> Fisico " + posicaoTransladada +
                                   " : " + programa[i].opc + " " + programa[i].ra + " " + programa[i].rb + " " + programa[i].p);
                
				memory.pos[posicaoTransladada].opc = programa[i].opc;
                memory.pos[posicaoTransladada].p = programa[i].p;
                memory.pos[posicaoTransladada].ra = programa[i].ra;
                memory.pos[posicaoTransladada].rb = programa[i].rb;
            } else {
                System.err.println("  Erro GM.carregarPrograma: Não foi possível carregar a palavra " + i +
                                   " do programa. Endereço lógico inválido após tradução.");
            }
        }
        System.out.println("GM.carregarPrograma: Fim do carregamento.");
	}

	public static int tradutor(int enderecoLogico, int[] tabelaPaginas, int tamPaginaDoProcesso){
		int numPagina = enderecoLogico / tamPaginaDoProcesso;
		int offset = enderecoLogico % tamPaginaDoProcesso;

		if (enderecoLogico < 0 || tabelaPaginas == null) {
            System.err.println("Erro GM.tradutor: Endereço ou tabela de páginas inválidos."); // Saída de erro mais consistente
			return -1;
		}

		if (tabelaPaginas == null || numPagina >= tabelaPaginas.length || tabelaPaginas[numPagina] == -1) {
			System.err.println("Erro GM.tradutor: Página " + numPagina + " para endereço lógico " + enderecoLogico +
							" não existe/não foi alocada na memória RAM ou tabela de páginas inválida.");
			return -1;
		}

		int numFrame = tabelaPaginas[numPagina];
		int enderecoFisico = (numFrame * tamPaginaDoProcesso) + offset; // Usa o tamPag passado

		System.out.println("GM.tradutor: Logico " + enderecoLogico + " (Pag " + numPagina + ", Offset " + offset +
		                   ") -> Fisico " + enderecoFisico + " (Frame " + numFrame + ")");

		return enderecoFisico;
	}

}