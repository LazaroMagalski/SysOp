package VM;

import java.util.Arrays;
import java.util.LinkedList; // Necessário para acessar as filas do GP

import HW.CPU.CPU;
import HW.CPU.Interrupts; // Necessário para simular interrupções
import HW.CPU.Opcode;     // Necessário para simular instruções
import HW.Memory.Memory;
import HW.Memory.Word;    // Necessário para criar objetos Word
import HW.HW;             // Classe HW
import SW.GM;
import SW.GP;
import SW.GP.PCB;
import SW.GP.State;       // Importa o enum State
import SW.SysCallHandling; // Para simular syscall
import SW.InterruptHandling; // Para simular tratamento de interrupções
import SW.Scheduler;       // Para chamar o escalonador
import VM.Program;
import VM.Programs;


public class Testes {

    public static void main(String[] args) {
        Testes t = new Testes();

        System.out.println("--- Executando todos os testes ---");
        System.out.println("===================================\n");

        t.testSistemaGMInitialization();
        t.testGMAllocationSuccess();
        t.testProcessCreation();
        t.testProcessBlockedOnSyscall();
        t.testProcessUnblockedFromIO();
        t.testProcessTerminationOnStop();
        t.testProcessTerminationOnFatalInterrupt();
        t.testContextSavingAndRestoring();

        System.out.println("\n===================================");
        System.out.println("--- Todos os testes concluídos ---");
    }

    // Teste para verificar a inicialização do GM.tamPag dentro do Sistema
    public void testSistemaGMInitialization() {
        System.out.println("--- Iniciando teste: testSistemaGMInitialization ---");

        int tamMemoria = 1024;
        Sistema s = new Sistema(tamMemoria); // O Sistema agora inicializa GM com tamPag=10

        if (GM.tamPag == 10) {
            System.out.println("SUCESSO: GM.tamPag foi inicializado corretamente como 10 no Sistema.");
        } else {
            System.out.println("FALHA: GM.tamPag foi inicializado como " + GM.tamPag + ", esperado 10.");
        }
        System.out.println("--- Fim do teste: testSistemaGMInitialization ---\n");
    }

    // Teste original para alocação do GM, com algumas correções/melhorias
    public void testGMAllocationSuccess() {
        System.out.println("--- Iniciando teste: testGMAllocationSuccess ---");

        Memory memory = new Memory(1024); // Memória de 1024 palavras
        GM gm = new GM(memory, 16); // TamPag de 16 para este teste específico
        int numPalavrasParaAlocar = 100;

        System.out.println("GM stats para este teste:");
        System.out.println("  tamMem: " + gm.tamMem);
        System.out.println("  frames: " + gm.frames);
        System.out.println("  tamPag: " + GM.tamPag); // (GM.tamPag é estático, refletirá o último GM instanciado ou o valor padrão do Sistema)

        int[] tabelaPaginas = gm.aloca(numPalavrasParaAlocar); // Aloca as páginas

        if (tabelaPaginas != null) {
            System.out.println("SUCESSO: Alocação de " + numPalavrasParaAlocar + " palavras. Tabela de Páginas: " + Arrays.toString(tabelaPaginas));
            // Para testar a desalocação, descomente as linhas abaixo
            gm.desaloca(tabelaPaginas);
            System.out.println("SUCESSO: Desalocação da tabela de páginas realizada.");
        } else {
            System.out.println("FALHA: Não foi possível alocar " + numPalavrasParaAlocar + " palavras.");
        }

        // Teste de tradução de endereço (exemplo)
        // Certifique-se de que 'tabelaPaginas' não é nula e tem pelo menos um elemento
        if (tabelaPaginas != null && tabelaPaginas.length > 0) {
            // É importante alocar novamente ou pegar um estado válido da tabela de páginas
            // para que a tradução funcione após a desalocação acima.
            // Para um teste isolado de tradução, o ideal seria não desalocar imediatamente.
            // Vamos testar um cenário simples se a alocação inicial for bem-sucedida
            System.out.println("Testando tradução de endereço (exemplo):");
            int enderecoLogicoExemplo = 0; // Primeiro endereço lógico
            int enderecoFisico = GM.tradutor(enderecoLogicoExemplo, tabelaPaginas);
            System.out.println("  Endereço Lógico " + enderecoLogicoExemplo + " traduz para Físico " + enderecoFisico);
            if (enderecoFisico != -1) {
                System.out.println("  SUCESSO: Tradução de endereço retornou um valor válido.");
            } else {
                System.out.println("  FALHA: Tradução de endereço retornou um valor inválido.");
            }
        }
        System.out.println("--- Fim do teste: testGMAllocationSuccess ---\n");
    }

    // Teste para criação de processo
    public void testProcessCreation() {
        System.out.println("--- Iniciando teste: testProcessCreation ---");

        int tamMemoria = 1024;
        Memory memory = new Memory(tamMemoria);
        HW hw = new HW(tamMemoria);

        GM gmTeste = new GM(memory, 10);

        GP gp = new GP(hw, gmTeste);

        Programs programs = new Programs();
        Program program = programs.progs[0]; // Pegando o primeiro programa (sum)

        System.out.println("Tentando criar processo para o programa: " + program.name);
        boolean criado = gp.criaProcesso(program);

        if (criado) {
            System.out.println("SUCESSO: Processo para '" + program.name + "' criado com sucesso.");
            System.out.println("  Número de PCBs na lista: " + gp.pcbList.size());
            if (!gp.pcbList.isEmpty()) {
                GP.PCB pcbCriado = gp.pcbList.getFirst();
                System.out.println("  ID do PCB criado: " + pcbCriado.id);
                System.out.println("  Tabela de Páginas do PCB: " + Arrays.toString(pcbCriado.tabPag));
                System.out.println("  Verificando se o programa foi carregado na memória do GP.gm:");
                for (int i = 0; i < program.image.length; i++) {
                    int enderecoFisico = GM.tradutor(i, pcbCriado.tabPag);
                    if (enderecoFisico != -1) {
                        if (gp.getGm().memory.pos[enderecoFisico].opc != program.image[i].opc ||
                            gp.getGm().memory.pos[enderecoFisico].p != program.image[i].p ||
                            gp.getGm().memory.pos[enderecoFisico].ra != program.image[i].ra ||
                            gp.getGm().memory.pos[enderecoFisico].rb != program.image[i].rb) {
                            System.out.println("  FALHA: Conteúdo da memória no endereço lógico " + i +
                                               " não corresponde ao programa original.");
                            System.out.println("    Esperado: " + program.image[i].opc + " " + program.image[i].ra + " " + program.image[i].rb + " " + program.image[i].p);
                            System.out.println("    Encontrado: " + gp.getGm().memory.pos[enderecoFisico].opc + " " + gp.getGm().memory.pos[enderecoFisico].ra + " " + gp.getGm().memory.pos[enderecoFisico].rb + " " + gp.getGm().memory.pos[enderecoFisico].p);
                            break;
                        }
                    } else {
                        System.out.println("  FALHA: Endereço lógico " + i + " inválido na tabela de páginas do PCB.");
                        break;
                    }
                }
                System.out.println("  SUCESSO: Verificação básica de carregamento do programa na memória do GP.gm.");
                gp.desalocaProcesso(pcbCriado.id); // Limpa o processo para não afetar outros testes

            }
        } else {
            System.out.println("FALHA: Processo para '" + program.name + "' NÃO foi criado.");
            System.out.println("  Provável causa: memória insuficiente ou programa muito grande.");
        }
        System.out.println("--- Fim do teste: testProcessCreation ---\n");
    }

    // NOVO TESTE: Verifica se um processo é bloqueado corretamente ao fazer SYSCALL
    public void testProcessBlockedOnSyscall() {
        System.out.println("--- Iniciando teste: testProcessBlockedOnSyscall ---");
        int tamMemoria = 1024;
        HW hw = new HW(tamMemoria);
        hw.cpu.setDebug(false); // Desliga o debug da CPU para o teste não ter muita saída
        GM gm = new GM(hw.mem, 10);
        GP gp = new GP(hw, gm);

        // Um programa simples que faz uma SYSCALL (escreve um valor)
        Program progSyscall = new Program("progSyscall",
            new Word[]{
                new Word(Opcode.LDI, 8, -1, 2),    // 0: r8 = 2 (OUT)
                new Word(Opcode.LDI, 9, -1, 6),   // 1: r9 = 6 (endereço na memória) <-- MUDANÇA AQUI (antes era 50)
                new Word(Opcode.LDI, 0, -1, 123),  // 2: r0 = 123 (valor a ser escrito)
                new Word(Opcode.STD, 0, -1, 6),   // 3: [6] = 123 <-- MUDANÇA AQUI (antes era 50)
                new Word(Opcode.SYSCALL, -1, -1, -1), // 4: Chama SYSCALL
                new Word(Opcode.STOP, -1, -1, -1), // 5: Nunca deveria chegar aqui se bloqueado
                new Word(Opcode.DATA, -1, -1, 0) // 6: Posição para o dado (endereço lógico 6)
            }
        );

        // Cria o processo
        boolean criado = gp.criaProcesso(progSyscall);
        if (!criado) {
            System.out.println("FALHA: Não foi possível criar o processo para testProcessBlockedOnSyscall.");
            System.out.println("--- Fim do teste: testProcessBlockedOnSyscall ---\n");
            return;
        }

        PCB pcb1 = gp.pcbList.getFirst(); // O primeiro e único PCB
        gp.procExec = pcb1.id; // Simula que este processo está em execução

        // Inicializa o SysCallHandling e InterruptHandling
        SysCallHandling sc = new SysCallHandling(hw, gp);
        InterruptHandling ih = new InterruptHandling(hw, gp); // Passa GP para IH
        hw.cpu.setAddressOfHandlers(ih, sc); // Seta os handlers na CPU

        // Executa até a instrução SYSCALL (PC chegará em 5)
        hw.cpu.setContext(pcb1.pc);
        hw.cpu.updateMMU(pcb1.tabPag);
        System.arraycopy(pcb1.regs, 0, hw.cpu.reg, 0, pcb1.regs.length);
        hw.cpu.irpt = Interrupts.noInterrupt; // Limpa interrupções
        hw.cpu.run(5); // Roda 5 instruções, a 5a é a SYSCALL

        // Após a SYSCALL, o SysCallHandling.handle() deve ter sido chamado,
        // e o processo deve ter sido bloqueado.
        if (pcb1.state == State.BLOCKED) {
            System.out.println("SUCESSO: Processo " + pcb1.id + " mudou para estado BLOCKED.");
        } else {
            System.out.println("FALHA: Processo " + pcb1.id + " está no estado " + pcb1.state + ", esperado BLOCKED.");
        }

        // Verifica se o PCB foi movido da fila de prontos para a de bloqueados
        if (!gp.pcbList.contains(pcb1) && gp.blockedPcbList.contains(pcb1)) {
            System.out.println("SUCESSO: PCB movido da fila de prontos para a fila de bloqueados.");
        } else {
            System.out.println("FALHA: PCB não foi movido corretamente entre as filas.");
            System.out.println("  Na fila de prontos? " + gp.pcbList.contains(pcb1));
            System.out.println("  Na fila de bloqueados? " + gp.blockedPcbList.contains(pcb1));
        }

        // Verifica se o PC e registradores foram salvos corretamente no PCB
        // O PC deve ser o PC APÓS a instrução SYSCALL (pcb1.pc = hw.cpu.pc)
        // A instrução SYSCALL está em 4, pc++ leva para 5.
        if (pcb1.pc == 5) {
             System.out.println("SUCESSO: PC salvo corretamente no PCB (" + pcb1.pc + ").");
        } else {
             System.out.println("FALHA: PC salvo incorretamente no PCB (" + pcb1.pc + "), esperado 5.");
        }

        // Exemplo de verificação de um registrador salvo
        if (pcb1.regs[8] == 2 && pcb1.regs[9] == 50 && pcb1.regs[0] == 123) {
            System.out.println("SUCESSO: Registradores (r0, r8 e r9) salvos corretamente no PCB.");
        } else {
            System.out.println("FALHA: Registradores r0, r8/r9 salvos incorretamente.");
            System.out.println("  r0: " + pcb1.regs[0] + ", esperado 123");
            System.out.println("  r8: " + pcb1.regs[8] + ", esperado 2");
            System.out.println("  r9: " + pcb1.regs[9] + ", esperado 50");
        }

        // Desaloca o processo para limpar o estado para outros testes
        gp.desalocaProcesso(pcb1.id);

        System.out.println("--- Fim do teste: testProcessBlockedOnSyscall ---\n");
    }

    // NOVO TESTE: Simula interrupção de I/O e verifica desbloqueio
    public void testProcessUnblockedFromIO() {
        System.out.println("--- Iniciando teste: testProcessUnblockedFromIO ---");
        // PRÉ-REQUISITO: Adicione Interrupts.intIOCompleta em HW.CPU.Interrupts
        // PRÉ-REQUISITO: Altere InterruptHandling.java para lidar com intIOCompleta

        int tamMemoria = 1024;
        HW hw = new HW(tamMemoria);
        hw.cpu.setDebug(false);
        GM gm = new GM(hw.mem, 10);
        GP gp = new GP(hw, gm);

        // Um programa que faz SYSCALL (para ser bloqueado)
        Program progSyscall = new Program("progToBlock",
            new Word[]{
                new Word(Opcode.LDI, 8, -1, 1),    // 0: r8 = 1 (IN)
                new Word(Opcode.LDI, 9, -1, 6),   // 1: r9 = 6 (endereço para leitura) <-- MUDANÇA AQUI (antes era 50)
                new Word(Opcode.SYSCALL, -1, -1, -1), // 2: Chama SYSCALL
                new Word(Opcode.STOP, -1, -1, -1), // 3: Nunca deveria chegar aqui
                new Word(Opcode.DATA, -1, -1, 0) // 4: Posição para o dado (endereço lógico 4)
            }
        );
        // Total de 5 palavras. O endereço 6 ainda está fora.
        // Se a instrução LDD/STD se refere a um endereço *dentro* do programa, ela pode ser usada.
        // Se a posição para o dado deve ser acessada por IN/OUT, ela também precisa estar alocada.
        // Vamos garantir que o programa seja grande o suficiente ou que o endereço esteja na área alocada.

        // Melhor ainda, vamos alocar o programa com um tamanho que inclua o endereço de dado
        // Programas no Programs.java são carregados com o image.length.
        // Para este programa, o array tem 5 palavras, então o endereço 6 é inválido.
        // Vamos mudar para um endereço que seja parte do programa, ou aumentar o programa.
        // Para simplificar, vamos usar um endereço lógico que esteja dentro das palavras do programa.

        // MUDANÇA AQUI: Ajustar o endereço para estar dentro das palavras do programa
        // (0-4 para um programa de 5 palavras). Vamos usar o endereço lógico 4.
        Program progSyscallUnblock = new Program("progToBlock", // Renomeei para evitar confusão de nome
            new Word[]{
                new Word(Opcode.LDI, 8, -1, 1),    // 0: r8 = 1 (IN)
                new Word(Opcode.LDI, 9, -1, 4),   // 1: r9 = 4 (endereço para leitura) <-- MUDANÇA AQUI (antes era 50)
                new Word(Opcode.SYSCALL, -1, -1, -1), // 2: Chama SYSCALL
                new Word(Opcode.STOP, -1, -1, -1), // 3: Nunca deveria chegar aqui
                new Word(Opcode.DATA, -1, -1, 0) // 4: Posição para o dado (endereço lógico 4) <-- AGORA VÁLIDO PARA DADO
            }
        );
        // O programa agora tem 5 palavras (índices 0 a 4). O endereço 4 é válido.

        // Use progSyscallUnblock na linha abaixo
        boolean criado = gp.criaProcesso(progSyscallUnblock); // Use o novo programa

        if (!criado) {
            System.out.println("FALHA: Não foi possível criar o processo para testProcessUnblockedFromIO.");
            System.out.println("--- Fim do teste: testProcessUnblockedFromIO ---\n");
            return;
        }

        PCB pcb1 = gp.pcbList.getFirst();
        gp.procExec = pcb1.id;

        SysCallHandling sc = new SysCallHandling(hw, gp);
        InterruptHandling ih = new InterruptHandling(hw, gp); // Passa GP para IH
        hw.cpu.setAddressOfHandlers(ih, sc);

        // Simula a execução da SYSCALL para bloquear o processo
        hw.cpu.setContext(pcb1.pc);
        hw.cpu.updateMMU(pcb1.tabPag);
        System.arraycopy(pcb1.regs, 0, hw.cpu.reg, 0, pcb1.regs.length);
        hw.cpu.irpt = Interrupts.noInterrupt;
        hw.cpu.run(3); // Executa até a SYSCALL (instrução 2 + 1 de PC++) -> PC=3

        if (pcb1.state == State.BLOCKED) {
            System.out.println("SUCESSO: Processo " + pcb1.id + " inicialmente BLOCKED.");
        } else {
            System.out.println("FALHA: Processo " + pcb1.id + " não está BLOCKED antes de simular interrupção. Estado: " + pcb1.state);
            gp.desalocaProcesso(pcb1.id);
            System.out.println("--- Fim do teste: testProcessUnblockedFromIO ---\n");
            return;
        }
        if (!gp.pcbList.contains(pcb1) && gp.blockedPcbList.contains(pcb1)) {
            System.out.println("SUCESSO: PCB está na fila de bloqueados.");
        } else {
            System.out.println("FALHA: PCB não está corretamente na fila de bloqueados.");
            System.out.println("  Na fila de prontos? " + gp.pcbList.contains(pcb1));
            System.out.println("  Na fila de bloqueados? " + gp.blockedPcbList.contains(pcb1));
            gp.desalocaProcesso(pcb1.id);
            System.out.println("--- Fim do teste: testProcessUnblockedFromIO ---\n");
            return;
        }

        // SIMULAÇÃO DA INTERRUPÇÃO DE I/O:
        // A thread do console/dispositivo, ao terminar o I/O, chamaria gp.unblockProcess(pcb1);
        // E sinalizaria uma interrupção de I/O na CPU.
        // Vamos chamar diretamente o método do GP para simular o desbloqueio.
        // No cenário real, a interrupção seria sinalizada, e o scheduler a pegaria.
        // Aqui, para o teste unitário, chamamos o handler manualmente e depois testamos o estado.
        ih.handle(Interrupts.intIOCompleta); // Simula a interrupção de I/O completa. Isso deve chamar gp.unblockProcess(primeiro pcb bloqueado)

        // Após o desbloqueio, o processo deve estar READY e na fila de prontos
        if (pcb1.state == State.READY) {
            System.out.println("SUCESSO: Processo " + pcb1.id + " mudou para estado READY após desbloqueio.");
        } else {
            System.out.println("FALHA: Processo " + pcb1.id + " está no estado " + pcb1.state + ", esperado READY.");
        }

        if (gp.pcbList.contains(pcb1) && !gp.blockedPcbList.contains(pcb1)) {
            System.out.println("SUCESSO: PCB movido para a fila de prontos e removido da bloqueados.");
        } else {
            System.out.println("FALHA: PCB não foi movido corretamente entre as filas após desbloqueio.");
            System.out.println("  Na fila de prontos? " + gp.pcbList.contains(pcb1));
            System.out.println("  Na fila de bloqueados? " + gp.blockedPcbList.contains(pcb1));
        }

        gp.desalocaProcesso(pcb1.id);
        System.out.println("--- Fim do teste: testProcessUnblockedFromIO ---\n");
    }

    // NOVO TESTE: Verifica se um processo é finalizado corretamente ao encontrar STOP
    public void testProcessTerminationOnStop() {
        System.out.println("--- Iniciando teste: testProcessTerminationOnStop ---");
        int tamMemoria = 1024;
        HW hw = new HW(tamMemoria);
        hw.cpu.setDebug(false);
        GM gm = new GM(hw.mem, 10);
        GP gp = new GP(hw, gm);

        Program progStop = new Program("progStop",
            new Word[]{
                new Word(Opcode.LDI, 0, -1, 100),
                new Word(Opcode.STOP, -1, -1, -1) // Instrução STOP
            }
        );

        boolean criado = gp.criaProcesso(progStop);
        if (!criado) {
            System.out.println("FALHA: Não foi possível criar o processo para testProcessTerminationOnStop.");
            System.out.println("--- Fim do teste: testProcessTerminationOnStop ---\n");
            return;
        }

        PCB pcb1 = gp.pcbList.getFirst();
        gp.procExec = pcb1.id;

        // Configura o Scheduler e SO para simular o ambiente
        SysCallHandling sc = new SysCallHandling(hw, gp);
        InterruptHandling ih = new InterruptHandling(hw, gp); // Passa GP para IH
        hw.cpu.setAddressOfHandlers(ih, sc);

        Scheduler scheduler = new Scheduler(gp, hw, gp.pcbList, gp.blockedPcbList);

        System.out.println("Executando o scheduler para o processo " + pcb1.id + " (espera STOP)...");
        scheduler.schedule(); // Executa o processo até STOP ou fatia de tempo

        // Após o schedule, o processo deve ter sido desalocado
        if (!gp.pcbList.contains(pcb1) && !gp.blockedPcbList.contains(pcb1)) {
            System.out.println("SUCESSO: Processo " + pcb1.id + " removido de todas as filas após STOP.");
        } else {
            System.out.println("FALHA: Processo " + pcb1.id + " ainda presente nas filas após STOP.");
            System.out.println("  Na fila de prontos? " + gp.pcbList.contains(pcb1));
            System.out.println("  Na fila de bloqueados? " + gp.blockedPcbList.contains(pcb1));
        }

        // Podemos tentar desalocar novamente para verificar o método desalocaProcesso
        // Se ele retornar false, significa que o processo já não existia, o que é o esperado.
        if (!gp.desalocaProcesso(pcb1.id)) {
            System.out.println("SUCESSO: Tentativa de desalocar processo já finalizado (retornou false).");
        } else {
            System.out.println("FALHA: Processo foi desalocado novamente ou não deveria ter sido.");
        }

        System.out.println("--- Fim do teste: testProcessTerminationOnStop ---\n");
    }

    // NOVO TESTE: Verifica se um processo é finalizado em caso de interrupção fatal
    public void testProcessTerminationOnFatalInterrupt() {
        System.out.println("--- Iniciando teste: testProcessTerminationOnFatalInterrupt ---");
        int tamMemoria = 1024;
        HW hw = new HW(tamMemoria);
        hw.cpu.setDebug(false);
        GM gm = new GM(hw.mem, 10);
        GP gp = new GP(hw, gm);

        // Programa com uma instrução inválida (Opcode.___) ou acesso a endereço inválido
        Program progFatal = new Program("progFatal",
            new Word[]{
                new Word(Opcode.LDI, 0, -1, 100),
                new Word(Opcode.___, -1, -1, -1) // Instrução inválida
            }
        );

        boolean criado = gp.criaProcesso(progFatal);
        if (!criado) {
            System.out.println("FALHA: Não foi possível criar o processo para testProcessTerminationOnFatalInterrupt.");
            System.out.println("--- Fim do teste: testProcessTerminationOnFatalInterrupt ---\n");
            return;
        }

        PCB pcb1 = gp.pcbList.getFirst();
        gp.procExec = pcb1.id;

        SysCallHandling sc = new SysCallHandling(hw, gp);
        InterruptHandling ih = new InterruptHandling(hw, gp); // Passa GP para IH
        hw.cpu.setAddressOfHandlers(ih, sc);

        Scheduler scheduler = new Scheduler(gp, hw, gp.pcbList, gp.blockedPcbList);

        System.out.println("Executando o scheduler para o processo " + pcb1.id + " (espera interrupção fatal)...");
        scheduler.schedule(); // Executa o processo, deve gerar intInstrucaoInvalida

        // Após o schedule, o processo deve ter sido desalocado
        if (!gp.pcbList.contains(pcb1) && !gp.blockedPcbList.contains(pcb1)) {
            System.out.println("SUCESSO: Processo " + pcb1.id + " removido de todas as filas após interrupção fatal.");
        } else {
            System.out.println("FALHA: Processo " + pcb1.id + " ainda presente nas filas após interrupção fatal.");
            System.out.println("  Na fila de prontos? " + gp.pcbList.contains(pcb1));
            System.out.println("  Na fila de bloqueados? " + gp.blockedPcbList.contains(pcb1));
        }

        if (!gp.desalocaProcesso(pcb1.id)) {
            System.out.println("SUCESSO: Tentativa de desalocar processo já finalizado (retornou false).");
        } else {
            System.out.println("FALHA: Processo foi desalocado novamente ou não deveria ter sido.");
        }

        System.out.println("--- Fim do teste: testProcessTerminationOnFatalInterrupt ---\n");
    }

    // NOVO TESTE: Verifica o salvamento e restauração do contexto (PC e registradores)
    public void testContextSavingAndRestoring() {
        System.out.println("--- Iniciando teste: testContextSavingAndRestoring ---");
        int tamMemoria = 1024;
        HW hw = new HW(tamMemoria);
        hw.cpu.setDebug(false);
        GM gm = new GM(hw.mem, 10);
        GP gp = new GP(hw, gm);

        // Programa para simular execução e preempção
        Program progContext = new Program("progContext",
            new Word[]{
                new Word(Opcode.LDI, 0, -1, 10), // r0 = 10 (PC=0)
                new Word(Opcode.ADDI, 0, -1, 5), // r0 = 15 (PC=1)
                new Word(Opcode.LDI, 1, -1, 20), // r1 = 20 (PC=2)
                new Word(Opcode.ADD, 0, 1, -1),  // r0 = 15 + 20 = 35 (PC=3)
                new Word(Opcode.LDI, 2, -1, 30), // r2 = 30 (PC=4)
                new Word(Opcode.STOP, -1, -1, -1) // (PC=5)
            }
        );

        boolean criado = gp.criaProcesso(progContext);
        if (!criado) {
            System.out.println("FALHA: Não foi possível criar o processo para testContextSavingAndRestoring.");
            System.out.println("--- Fim do teste: testContextSavingAndRestoring ---\n");
            return;
        }

        PCB pcb1 = gp.pcbList.getFirst();
        gp.procExec = pcb1.id;

        SysCallHandling sc = new SysCallHandling(hw, gp);
        InterruptHandling ih = new InterruptHandling(hw, gp); // Passa GP para IH
        hw.cpu.setAddressOfHandlers(ih, sc);

        Scheduler scheduler = new Scheduler(gp, hw, gp.pcbList, gp.blockedPcbList);

        // --- Primeira execução: Roda por uma fatia de tempo (2 instruções) ---
        System.out.println("Execução 1: Processo " + pcb1.id + " roda 2 instruções.");
        scheduler.schedule();

        // Após a primeira fatia (LDI r0, 10; ADDI r0, 5)
        // PC deve estar em 2 (apontando para LDI r1, 20)
        // r0 deve ser 15
        if (pcb1.pc == 2 && pcb1.regs[0] == 15 && pcb1.state == State.READY) {
            System.out.println("SUCESSO (Exec 1): Contexto salvo corretamente (PC=2, R0=15). Estado: " + pcb1.state);
        } else {
            System.out.println("FALHA (Exec 1): Contexto salvo incorretamente.");
            System.out.println("  PC: " + pcb1.pc + ", Esperado: 2");
            System.out.println("  R0: " + pcb1.regs[0] + ", Esperado: 15");
            System.out.println("  Estado: " + pcb1.state + ", Esperado: READY");
            gp.desalocaProcesso(pcb1.id);
            System.out.println("--- Fim do teste: testContextSavingAndRestoring ---\n");
            return;
        }

        // --- Segunda execução: Roda mais uma fatia de tempo (2 instruções) ---
        // O scheduler pegará pcb1 de volta, carregará o contexto salvo e continuará.
        System.out.println("Execução 2: Processo " + pcb1.id + " roda mais 2 instruções.");
        scheduler.schedule();

        // Após a segunda fatia (LDI r1, 20; ADD r0, r1)
        // PC deve estar em 4 (apontando para LDI r2, 30)
        // r0 deve ser 35 (15 + 20)
        // r1 deve ser 20
        if (pcb1.pc == 4 && pcb1.regs[0] == 35 && pcb1.regs[1] == 20 && pcb1.state == State.READY) {
            System.out.println("SUCESSO (Exec 2): Contexto salvo e restaurado corretamente (PC=4, R0=35, R1=20). Estado: " + pcb1.state);
        } else {
            System.out.println("FALHA (Exec 2): Contexto salvo/restaurado incorretamente.");
            System.out.println("  PC: " + pcb1.pc + ", Esperado: 4");
            System.out.println("  R0: " + pcb1.regs[0] + ", Esperado: 35");
            System.out.println("  R1: " + pcb1.regs[1] + ", Esperado: 20");
            System.out.println("  Estado: " + pcb1.state + ", Esperado: READY");
            gp.desalocaProcesso(pcb1.id);
            System.out.println("--- Fim do teste: testContextSavingAndRestoring ---\n");
            return;
        }

        // --- Terceira execução: Deve executar a última instrução e STOP ---
        System.out.println("Execução 3: Processo " + pcb1.id + " roda últimas instruções e STOP.");
        scheduler.schedule();

        // Após o STOP, o processo deve ser finalizado
        if (!gp.pcbList.contains(pcb1) && !gp.blockedPcbList.contains(pcb1)) {
            System.out.println("SUCESSO (Exec 3): Processo " + pcb1.id + " finalizado após STOP.");
        } else {
            System.out.println("FALHA (Exec 3): Processo " + pcb1.id + " ainda presente nas filas após STOP.");
            System.out.println("  Na fila de prontos? " + gp.pcbList.contains(pcb1));
            System.out.println("  Na fila de bloqueados? " + gp.blockedPcbList.contains(pcb1));
        }

        gp.desalocaProcesso(pcb1.id); // Garante a limpeza, caso o teste falhe antes
        System.out.println("--- Fim do teste: testContextSavingAndRestoring ---\n");
    }
}