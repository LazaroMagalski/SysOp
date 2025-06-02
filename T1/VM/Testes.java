package VM;

import java.util.Arrays;

import HW.CPU.CPU;
import HW.Memory.Memory;
import SW.GM;
import SW.GP;
import HW.CPU.Opcode; // Adicionado para criar Words de teste, se necessário
import HW.HW; // classe HW

public class Testes {

    public static void main(String[] args) {
        Testes t = new Testes();

        // 1. Teste para verificar a inicialização do GM.tamPag no Sistema
        t.testSistemaGMInitialization();

        // 2. Teste da alocação do GM
        t.testGMAllocationSuccess();

        // 3. Teste da criação de processo no GP
        t.testProcessCreation();
    }

    // Teste para verificar a inicialização do GM.tamPag dentro do Sistema
    public void testSistemaGMInitialization() {
        System.out.println("--- Iniciando teste: testSistemaGMInitialization ---");

        int tamMemoria = 1024;
        Sistema s = new Sistema(tamMemoria); // O Sistema agora inicializa GM com tamPag=10

        if (GM.tamPag == 10) { //
            System.out.println("SUCESSO: GM.tamPag foi inicializado corretamente como 10 no Sistema."); //
        } else {
            System.out.println("FALHA: GM.tamPag foi inicializado como " + GM.tamPag + ", esperado 10."); //
        }
        System.out.println("--- Fim do teste: testSistemaGMInitialization ---\n");
    }

    // Teste original para alocação do GM, com algumas correções/melhorias
    public void testGMAllocationSuccess() {
        System.out.println("--- Iniciando teste: testGMAllocationSuccess ---");

        Memory memory = new Memory(1024); // Memória de 1024 palavras
        GM gm = new GM(memory, 16); // TamPag de 16 para este teste específico
        int numPalavrasParaAlocar = 100;

        System.out.println("GM stats para este teste:"); //
        System.out.println("  tamMem: " + gm.tamMem); //
        System.out.println("  frames: " + gm.frames); //
        System.out.println("  tamPag: " + GM.tamPag); // (GM.tamPag é estático, refletirá o último GM instanciado ou o valor padrão do Sistema)

        int[] tabelaPaginas = gm.aloca(numPalavrasParaAlocar); // Aloca as páginas

        if (tabelaPaginas != null) { //
            System.out.println("SUCESSO: Alocação de " + numPalavrasParaAlocar + " palavras. Tabela de Páginas: " + Arrays.toString(tabelaPaginas)); //
            // Para testar a desalocação, descomente as linhas abaixo
            gm.desaloca(tabelaPaginas); //
            System.out.println("SUCESSO: Desalocação da tabela de páginas realizada."); //
        } else {
            System.out.println("FALHA: Não foi possível alocar " + numPalavrasParaAlocar + " palavras."); //
        }

        // Teste de tradução de endereço (exemplo)
        if (tabelaPaginas != null && tabelaPaginas.length > 0) { //
            // É importante alocar novamente ou pegar um estado válido da tabela de páginas
            // para que a tradução funcione após a desalocação acima.
            // Para um teste isolado de tradução, o ideal seria não desalocar imediatamente.
            // Vamos testar um cenário simples se a alocação inicial for bem-sucedida
            System.out.println("Testando tradução de endereço (exemplo):"); //
            int enderecoLogicoExemplo = 0; // Primeiro endereço lógico
            int enderecoFisico = GM.tradutor(enderecoLogicoExemplo, tabelaPaginas); //
            System.out.println("  Endereço Lógico " + enderecoLogicoExemplo + " traduz para Físico " + enderecoFisico); //
            if (enderecoFisico != -1) { //
                System.out.println("  SUCESSO: Tradução de endereço retornou um valor válido."); //
            } else {
                System.out.println("  FALHA: Tradução de endereço retornou um valor inválido."); //
            }
        }
        System.out.println("--- Fim do teste: testGMAllocationSuccess ---\n");
    }

    // Teste original para criação de processo, com correções
    public void testProcessCreation() {
        System.out.println("--- Iniciando teste: testProcessCreation ---");

        int tamMemoria = 1024;
        Memory memory = new Memory(tamMemoria); //
        // Precisamos de uma instância de HW para passar para o GP
        HW hw = new HW(tamMemoria); // O HW já contém a CPU e a Memory
                                   // Note: o HW inicializa a CPU com debug=true por padrão. Se quiser false, você pode setar depois: hw.cpu.setDebug(false);

        // O GM instanciado aqui será passado para o GP,
        // MAS o GP cria um novo GM internamente.
        // Isso pode ser uma inconsistência no design atual.
        // Por enquanto, vamos instanciar para cumprir o construtor,
        // mas é um ponto a ser discutido e talvez alterado no GP.
        GM gmTeste = new GM(memory, 10); // (usando tamPag=10 para consistência com o que o GP criaria internamente)

        // CORREÇÃO AQUI: Passamos o objeto HW e o objeto GM (mesmo que ele seja recriado internamente pelo GP)
        GP gp = new GP(hw, gmTeste); //

        Programs programs = new Programs(); //
        Program program = programs.progs[0]; // Pegando o primeiro programa (sum)

        System.out.println("Tentando criar processo para o programa: " + program.name); //
        boolean criado = gp.criaProcesso(program); //

        if (criado) {
            System.out.println("SUCESSO: Processo para '" + program.name + "' criado com sucesso.");
            System.out.println("  Número de PCBs na lista: " + gp.pcbList.size()); //
            if (!gp.pcbList.isEmpty()) { //
                GP.PCB pcbCriado = gp.pcbList.getFirst(); //
                System.out.println("  ID do PCB criado: " + pcbCriado.id); //
                System.out.println("  Tabela de Páginas do PCB: " + Arrays.toString(pcbCriado.tabPag)); //
                // Verificar se o programa foi carregado na memória do GM INTERNO do GP
                // Atenção: O GM usado pelo GP é 'gp.gm', não 'gmTeste' que foi passado no construtor
                System.out.println("  Verificando se o programa foi carregado na memória do GP.gm:");
                for (int i = 0; i < program.image.length; i++) { //
                    int enderecoFisico = GM.tradutor(i, pcbCriado.tabPag); //
                    if (enderecoFisico != -1) {
                        // Apenas um exemplo de verificação: comparar opcode e parâmetro.
                        // Uma verificação mais robusta incluiria ra e rb.
                        if (gp.getGm().memory.pos[enderecoFisico].opc != program.image[i].opc ||
                            gp.getGm().memory.pos[enderecoFisico].p != program.image[i].p) {
                            System.out.println("  FALHA: Conteúdo da memória no endereço lógico " + i +
                                               " não corresponde ao programa original.");
                            break;
                        }
                    } else {
                        System.out.println("  FALHA: Endereço lógico " + i + " inválido na tabela de páginas do PCB.");
                        break;
                    }
                }
                System.out.println("  SUCESSO: Verificação básica de carregamento do programa na memória do GP.gm.");

            }
        } else {
            System.out.println("FALHA: Processo para '" + program.name + "' NÃO foi criado.");
            System.out.println("  Provável causa: memória insuficiente ou programa muito grande.");
        }
        System.out.println("--- Fim do teste: testProcessCreation ---\n");
    }
}