package HW.CPU;
import  HW.CPU.*;
import HW.Memory.Memory;
import HW.Memory.Word;
import SW.GM;
import SW.InterruptHandling;
import SW.SysCallHandling;
import SW.Utilities;
import SW.GP;

public class CPU {
    private int maxInt; // valores maximo e minimo para inteiros nesta cpu
    private int minInt;
                        // CONTEXTO da CPU ...
    public int pc;     // ... composto de program counter,
    public Word ir;    // instruction register,
    public int[] reg;  // registradores da CPU
    public Interrupts irpt; // durante instrucao, interrupcao pode ser sinalizada
                        // FIM CONTEXTO DA CPU: tudo que precisa sobre o estado de um processo para
                        // executa-lo
                        // nas proximas versoes isto pode modificar

    private Memory m;   // m é o array de memória "física", CPU tem uma ref a m para acessar

    private InterruptHandling ih;    // significa desvio para rotinas de tratamento de Int - se int ligada, desvia
    private SysCallHandling sysCall; // significa desvio para tratamento de chamadas de sistema

    private boolean cpuStop;    // flag para parar CPU - caso de interrupcao que acaba o processo, ou chamada stop - 
                                // nesta versao acaba o sistema no fim do prog

                                // auxilio aa depuração
    private boolean debug;      // se true entao mostra cada instrucao em execucao
    private Utilities u;        // para debug (dump)
    private int[] tabPag;

    // NOVOS ATRIBUTOS PARA CONCORRÊNCIA
    private GP gp; // Referência ao GP para notificar o Scheduler/OS
    public final Object cpuMonitor = new Object(); // Objeto para wait/notify


    public CPU(Memory _mem, boolean _debug) { // ref a MEMORIA passada na criacao da CPU
        maxInt = 32767;            // capacidade de representacao modelada
        minInt = -32767;           // se exceder deve gerar interrupcao de overflow
        m = _mem;              // usa o atributo 'm' para acessar a memoria, só para ficar mais pratico
        reg = new int[10];         // aloca o espaço dos registradores - regs 8 e 9 usados somente para IO                                        [x]

        debug = _debug;            // se true, print da instrucao em execucao

    }
    
    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setAddressOfHandlers(InterruptHandling _ih, SysCallHandling _sysCall) {
        ih = _ih;                  // aponta para rotinas de tratamento de int
        sysCall = _sysCall;        // aponta para rotinas de tratamento de chamadas de sistema
    }

    public void setUtilities(Utilities _u) {
        u = _u;                     // aponta para rotinas utilitárias - fazer dump da memória na tela
    }

                                   // verificação de enderecamento 
    private boolean legal(int e) { // todo acesso a memoria tem que ser verificado se é válido - 
                                   // aqui no caso se o endereco é um endereco valido em toda memoria
        if (e >= 0 && e < m.pos.length) {
            return true;
        } else {
            irpt = Interrupts.intEnderecoInvalido;    // se nao for liga interrupcao no meio da exec da instrucao
            return false;
        }
    }

    private boolean testOverflow(int v) {             // toda operacao matematica deve avaliar se ocorre overflow
        if ((v < minInt) || (v > maxInt)) {
            irpt = Interrupts.intOverflow;            // se houver liga interrupcao no meio da exec da instrucao
            return false;
        }
        ;
        return true;
    }

    public void setContext(int _pc) {                 // usado para setar o contexto da cpu para rodar um processo
                                                      // [ nesta versao é somente colocar o PC na posicao 0 ]
        pc = _pc;                                     // pc cfe endereco logico
        irpt = Interrupts.noInterrupt;                // reset da interrupcao registrada
    }

    public void updateMMU(int[] tabPag) {
        this.tabPag = tabPag;
    }

    // MODIFICADO: Método run para funcionar com wait/notify
    // O parâmetro 'nr_intrs' agora representa a fatia de tempo
    public void run(int fatiaDeTempo) { // execucao da CPU supoe que o contexto da CPU esta setado
        synchronized (cpuMonitor) { // Sincroniza no objeto monitor da CPU
            try {
                // A CPU aguarda até ser notificada para executar
                // No início do ciclo, o scheduler notifica a CPU para iniciar
                // Depois de cada fatia de tempo ou interrupção, ela volta a esperar.
                cpuMonitor.wait(); // CPU espera até ser acordada pelo Scheduler

                // Reset do pc e irpt são feitos pelo setContext() no Scheduler
                cpuStop = false; // Reset da flag de parada da CPU
                int instrucoesExecutadas = 0;

                // Ciclo de instruções
                // A CPU executa até a fatia de tempo acabar, ou uma interrupção/STOP ocorrer
                while (!cpuStop && irpt == Interrupts.noInterrupt && instrucoesExecutadas < fatiaDeTempo) {

                    // FASE DE FETCH
                    if (!legal(GM.tradutor(pc, tabPag, this.gp.gm.tamPag))) { // pc invalido
                        irpt = Interrupts.intEnderecoInvalido; // Liga interrupção
                        break; // Sai do ciclo, interrupção será tratada
                    }
                    ir = m.pos[GM.tradutor(pc, tabPag, this.gp.gm.tamPag)];

                    if (debug) {
                        System.out.print("                                             regs: ");
                        for (int i = 0; i < 10; i++) {
                            System.out.print(" r[" + i + "]:" + reg[i]);
                        }
                        System.out.println();
                    }
                    if (debug) {
                        System.out.print("                      pc: " + pc + "       exec: ");
                        u.dump(ir);
                    }

                    // FASE DE EXECUCAO DA INSTRUCAO CARREGADA NO ir
                    switch (ir.opc) {
                        // Instrucoes de Busca e Armazenamento em Memoria
                        case LDI:
                            reg[ir.ra] = ir.p;
                            pc++;
                            break;
                        case LDD:
                            if (legal(GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag))) {
                                reg[ir.ra] = m.pos[GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag)].p;
                                pc++;
                            } else { irpt = Interrupts.intEnderecoInvalido; } // Interrupção em LDD
                            break;
                        case LDX:
                            if (legal(GM.tradutor(reg[ir.rb], tabPag, this.gp.gm.tamPag))) {
                                reg[ir.ra] = m.pos[GM.tradutor(reg[ir.rb], tabPag, this.gp.gm.tamPag)].p;
                                pc++;
                            } else { irpt = Interrupts.intEnderecoInvalido; } // Interrupção em LDX
                            break;
                        case STD:
                            if (legal(GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag))) {
                                m.pos[GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag)].opc = Opcode.DATA;
                                m.pos[GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag)].p = reg[ir.ra];
                                pc++;
                                if (debug) {
                                    System.out.print("                                  ");
                                    u.dump(ir.p,ir.p+1);
                                }
                            } else { irpt = Interrupts.intEnderecoInvalido; } // Interrupção em STD
                            break;
                        case STX:
                            if (legal(GM.tradutor(reg[ir.ra], tabPag, this.gp.gm.tamPag))) {
                                m.pos[GM.tradutor(reg[ir.ra], tabPag, this.gp.gm.tamPag)].opc = Opcode.DATA;
                                m.pos[GM.tradutor(reg[ir.ra], tabPag, this.gp.gm.tamPag)].p = reg[ir.rb];
                                pc++;
                            } else { irpt = Interrupts.intEnderecoInvalido; } // Interrupção em STX
                            break;
                        case MOVE:
                            reg[ir.ra] = reg[ir.rb];
                            pc++;
                            break;
                        // Instrucoes Aritmeticas
                        case ADD:
                            if (!testOverflow(reg[ir.ra] + reg[ir.rb])) { irpt = Interrupts.intOverflow; break; } // Check overflow
                            reg[ir.ra] = reg[ir.ra] + reg[ir.rb];
                            pc++;
                            break;
                        case ADDI:
                            if (!testOverflow(reg[ir.ra] + ir.p)) { irpt = Interrupts.intOverflow; break; } // Check overflow
                            reg[ir.ra] = reg[ir.ra] + ir.p;
                            pc++;
                            break;
                        case SUB:
                            if (!testOverflow(reg[ir.ra] - reg[ir.rb])) { irpt = Interrupts.intOverflow; break; } // Check overflow
                            reg[ir.ra] = reg[ir.ra] - reg[ir.rb];
                            pc++;
                            break;
                        case SUBI:
                            if (!testOverflow(reg[ir.ra] - ir.p)) { irpt = Interrupts.intOverflow; break; } // Check overflow
                            reg[ir.ra] = reg[ir.ra] - ir.p;
                            pc++;
                            break;
                        case MULT:
                            if (!testOverflow(reg[ir.ra] * reg[ir.rb])) { irpt = Interrupts.intOverflow; break; } // Check overflow
                            reg[ir.ra] = reg[ir.ra] * reg[ir.rb];
                            pc++;
                            break;

                        // Instrucoes JUMP
                        case JMP:
                            pc = ir.p;
                            break;
                        case JMPIM:
                            if (legal(ir.p)) { pc = m.pos[ir.p].p; } else { irpt = Interrupts.intEnderecoInvalido; } // Interrupção em JMPIM
                            break;
                        case JMPIG:
                            if (reg[ir.rb] > 0) { pc = reg[ir.ra]; } else { pc++; }
                            break;
                        case JMPIGK:
                            if (reg[ir.rb] > 0) { pc = ir.p; } else { pc++; }
                            break;
                        case JMPILK:
                            if (reg[ir.rb] < 0) { pc = ir.p; } else { pc++; }
                            break;
                        case JMPIEK:
                            if (reg[ir.rb] == 0) { pc = ir.p; } else { pc++; }
                            break;
                        case JMPIL:
                            if (reg[ir.rb] < 0) { pc = reg[ir.ra]; } else { pc++; }
                            break;
                        case JMPIE:
                            if (reg[ir.rb] == 0) { pc = reg[ir.ra]; } else { pc++; }
                            break;
                        case JMPIGM:
                            if (legal(ir.p)){
                                if (reg[ir.rb] > 0) { pc = m.pos[ir.p].p; } else { pc++; }
                            } else { irpt = Interrupts.intEnderecoInvalido; } // Interrupção em JMPIGM
                            break;
                        case JMPILM:
                            if (legal(ir.p)) { // Check if address is legal
                                if (reg[ir.rb] < 0) { pc = m.pos[ir.p].p; } else { pc++; }
                            } else { irpt = Interrupts.intEnderecoInvalido; } // Interrupção em JMPILM
                            break;
                        case JMPIEM:
                            if (legal(ir.p)) { // Check if address is legal
                                if (reg[ir.rb] == 0) { pc = m.pos[ir.p].p; } else { pc++; }
                            } else { irpt = Interrupts.intEnderecoInvalido; } // Interrupção em JMPIEM
                            break;
                        case JMPIGT:
                            if (reg[ir.ra] > reg[ir.rb]) { pc = ir.p; } else { pc++; }
                            break;

                        case DATA:
                            irpt = Interrupts.intInstrucaoInvalida;
                            break;

                        // Chamadas de sistema
                        case SYSCALL:
                            sysCall.handle(); // <<<<< aqui desvia para rotina de chamada de sistema, no momento so temos IO
                            pc++; // PC é incrementado ANTES do SYSCALL, para que o processo retorne para a próxima instrução
                            // O scheduler precisa saber que o processo bloqueou
                            // O SysCallHandling já chama gp.blockProcess(), que muda o estado do PCB.
                            // A CPU deve parar sua execução para este processo.
                            cpuStop = true; // Sinaliza para a CPU parar de executar esta fatia de tempo
                            break;

                        case STOP:
                            sysCall.stop(debug);
                            cpuStop = true; // Sinaliza que a CPU deve parar para este processo
                            break;

                        default:
                            irpt = Interrupts.intInstrucaoInvalida;
                            break;
                    }
                    instrucoesExecutadas++; // Incrementa o contador de instruções executadas
                } // FIM DO CICLO DE INSTRUÇÕES

                // VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
                if (irpt != Interrupts.noInterrupt) { // existe interrupção
                    ih.handle(irpt); // desvia para rotina de tratamento - esta rotina é do SO
                    cpuStop = true; // Interrupção fatal, para a CPU para este processo.
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaura o status de interrupção
                System.err.println("CPU thread interrupted: " + e.getMessage());
            } finally {
                // Após a fatia de tempo, SYSCALL, STOP ou interrupção fatal,
                // a CPU informa ao Scheduler que terminou sua rodada.
                // Isso permite que o Scheduler salve o contexto e escolha o próximo processo.
                gp.notifyScheduler(); // Notifica o Scheduler que a CPU terminou sua fatia
            }
        } // FIM DO SYNC
    }

    public void setGP(GP _gp) {
        this.gp = _gp; // Define a referência ao GP para notificar o Scheduler
    }

    // NOVO METODO: O loop principal da CPU Thread
    // Esta Thread ficara esperando por trabalho do Scheduler
    public void startCPUThread() {
        new Thread(() -> {
            while (true) {
                synchronized (cpuMonitor) {
                    try {
                        cpuMonitor.wait(); // CPU espera ser acordada pelo Scheduler
                        // Quando acordada, o contexto já foi carregado pelo Scheduler
                        // Agora, execute a fatia de tempo
                        executeCurrentProcessSlice(2); // Executa uma fatia de 2 instruções
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("CPU thread interrupted: " + e.getMessage());
                        break; // Sai do loop se a thread for interrompida
                    }
                } // FIM DO SYNC
            }
        }, "CPU-Core-Thread").start(); // Inicia a thread da CPU
    }

    // MODIFICADO: A logica principal de execucao da CPU para uma fatia de tempo
    // Este metodo e chamado internamente pela CPU Thread
    private void executeCurrentProcessSlice(int fatiaDeTempo) {
        cpuStop = false;
        irpt = Interrupts.noInterrupt;
        int instrucoesExecutadas = 0;

        while (!cpuStop && irpt == Interrupts.noInterrupt && instrucoesExecutadas < fatiaDeTempo) {
            // FASE DE FETCH
            // Certifique-se de que a tradução de endereço usa o tamPag
            if (!legal(GM.tradutor(pc, tabPag, this.gp.gm.tamPag))) {
                irpt = Interrupts.intEnderecoInvalido;
                break;
            }
            ir = m.pos[GM.tradutor(pc, tabPag, this.gp.gm.tamPag)]; // Lendo a instrução do endereço físico

            if (debug) {
                System.out.print("                                             regs: ");
                for (int i = 0; i < 10; i++) {
                    System.out.print(" r[" + i + "]:" + reg[i]);
                }
                System.out.println();
            }
            if (debug) {
                System.out.print("                      pc: " + pc + "       exec: ");
                u.dump(ir); // Este dump mostra a instrução corretamente lida
            }

            // FASE DE EXECUCAO DA INSTRUCAO CARREGADA NO ir
            switch (ir.opc) {
                // Instrucoes de Busca e Armazenamento em Memoria
                case LDI: // Rd ← k
                    reg[ir.ra] = ir.p;
                    pc++;
                    break;
                case LDD: // Rd <- [A]
                    if (legal(GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag))) { // Passa tamPag
                        reg[ir.ra] = m.pos[GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag)].p; // Passa tamPag
                        pc++;
                    } else { irpt = Interrupts.intEnderecoInvalido; }
                    break;
                case LDX: // RD <- [RS] // NOVA
                    if (legal(GM.tradutor(reg[ir.rb], tabPag, this.gp.gm.tamPag))) { // Passa tamPag
                        reg[ir.ra] = m.pos[GM.tradutor(reg[ir.rb], tabPag, this.gp.gm.tamPag)].p; // Passa tamPag
                        pc++;
                    } else { irpt = Interrupts.intEnderecoInvalido; }
                    break;
                case STD: // [A] ← Rs
                    if (legal(GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag))) { // Passa tamPag
                        m.pos[GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag)].opc = Opcode.DATA; // Passa tamPag
                        m.pos[GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag)].p = reg[ir.ra]; // Passa tamPag
                        pc++;
                        if (debug) {
                            System.out.print("                                  ");
                            u.dump(ir.p,ir.p+1);
                        }
                    } else { irpt = Interrupts.intEnderecoInvalido; }
                    break;
                case STX: // [Rd] ←Rs
                    if (legal(GM.tradutor(reg[ir.ra], tabPag, this.gp.gm.tamPag))) { // Passa tamPag
                        m.pos[GM.tradutor(reg[ir.ra], tabPag, this.gp.gm.tamPag)].opc = Opcode.DATA; // Passa tamPag
                        m.pos[GM.tradutor(reg[ir.ra], tabPag, this.gp.gm.tamPag)].p = reg[ir.rb]; // Passa tamPag
                        pc++;
                    } else { irpt = Interrupts.intEnderecoInvalido; }
                    break;
                case MOVE: // RD <- RS
                    reg[ir.ra] = reg[ir.rb];
                    pc++;
                    break;
                // Instrucoes Aritmeticas
                case ADD: // Rd ← Rd + Rs
                    if (!testOverflow(reg[ir.ra] + reg[ir.rb])) { irpt = Interrupts.intOverflow; break; }
                    reg[ir.ra] = reg[ir.ra] + reg[ir.rb];
                    pc++;
                    break;
                case ADDI: // Rd ← Rd + k
                    if (!testOverflow(reg[ir.ra] + ir.p)) { irpt = Interrupts.intOverflow; break; }
                    reg[ir.ra] = reg[ir.ra] + ir.p;
                    pc++;
                    break;
                case SUB: // Rd ← Rd - Rs
                    if (!testOverflow(reg[ir.ra] - reg[ir.rb])) { irpt = Interrupts.intOverflow; break; }
                    reg[ir.ra] = reg[ir.ra] - reg[ir.rb];
                    pc++;
                    break;
                case SUBI: // RD <- RD - k // NOVA
                    if (!testOverflow(reg[ir.ra] - ir.p)) { irpt = Interrupts.intOverflow; break; }
                    reg[ir.ra] = reg[ir.ra] - ir.p;
                    pc++;
                    break;
                case MULT: // Rd <- Rd * Rs
                    if (!testOverflow(reg[ir.ra] * reg[ir.rb])) { irpt = Interrupts.intOverflow; break; }
                    reg[ir.ra] = reg[ir.ra] * reg[ir.rb];
                    pc++;
                    break;

                // Instrucoes JUMP
                case JMP: // PC <- k
                    pc = ir.p;
                    break;
                case JMPIM: // PC <- [A]
                    if (legal(ir.p)) { // Verifica se A é legal
                        pc = m.pos[GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag)].p; // Passa tamPag
                    } else { irpt = Interrupts.intEnderecoInvalido; }
                    break;
                case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1
                    if (reg[ir.rb] > 0) { pc = reg[ir.ra]; } else { pc++; }
                    break;
                case JMPIGK: // If RC > 0 then PC <- k else PC++
                    if (reg[ir.rb] > 0) { pc = ir.p; } else { pc++; }
                    break;
                case JMPILK: // If RC < 0 then PC <- k else PC++
                    if (reg[ir.rb] < 0) { pc = ir.p; } else { pc++; }
                    break;
                case JMPIEK: // If RC = 0 then PC <- k else PC++
                    if (reg[ir.rb] == 0) { pc = ir.p; } else { pc++; }
                    break;
                case JMPIL: // if Rc < 0 then PC <- Rs Else PC <- PC +1
                    if (reg[ir.rb] < 0) { pc = reg[ir.ra]; } else { pc++; }
                    break;
                case JMPIE: // If Rc = 0 Then PC <- Rs Else PC <- PC +1
                    if (reg[ir.rb] == 0) { pc = reg[ir.ra]; } else { pc++; }
                    break;
                case JMPIGM: // If RC > 0 then PC <- [A] else PC++
                    if (legal(ir.p)){
                        if (reg[ir.rb] > 0) {
                            pc = m.pos[GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag)].p; // Passa tamPag
                        } else { pc++; }
                    } else { irpt = Interrupts.intEnderecoInvalido; }
                    break;
                case JMPILM: // If RC < 0 then PC <- [A] else PC++
                    if (legal(ir.p)) {
                        if (reg[ir.rb] < 0) {
                            pc = m.pos[GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag)].p; // Passa tamPag
                        } else { pc++; }
                    } else { irpt = Interrupts.intEnderecoInvalido; }
                    break;
                case JMPIEM: // If RC = 0 then PC <- [A] else PC++
                    if (legal(ir.p)) {
                        if (reg[ir.rb] == 0) {
                            pc = m.pos[GM.tradutor(ir.p, tabPag, this.gp.gm.tamPag)].p; // Passa tamPag
                        } else { pc++; }
                    } else { irpt = Interrupts.intEnderecoInvalido; }
                    break;
                case JMPIGT: // If RS>RC then PC <- k else PC++
                    if (reg[ir.ra] > reg[ir.rb]) { pc = ir.p; } else { pc++; }
                    break;

                // Casos especiais: DATA e ___
                case DATA: // pc está sobre área supostamente de dados
                    irpt = Interrupts.intInstrucaoInvalida;
                    break;
                case ___: // Opcode nulo, significa posição de memória não inicializada ou vazia
                    irpt = Interrupts.intInstrucaoInvalida;
                    break;

                // Chamadas de sistema
                case SYSCALL:
                    sysCall.handle();
                    pc++;
                    cpuStop = true; // Sinaliza para a CPU parar de executar esta fatia de tempo
                    break;

                case STOP:
                    sysCall.stop(debug);
                    cpuStop = true;
                    break;

                default: // Este default só deve ser alcançado se um novo opcode for adicionado ao enum, mas não ao switch
                    irpt = Interrupts.intInstrucaoInvalida;
                    break;
            }
            instrucoesExecutadas++;
        } // FIM DO CICLO DE INSTRUÇÕES DA FATIA

        // VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
        if (irpt != Interrupts.noInterrupt) {
            ih.handle(irpt);
            cpuStop = true; // Interrupção fatal, para a CPU para este processo.
        }

        // Após a fatia de tempo, SYSCALL, STOP ou interrupção fatal,
        // a CPU informa ao Scheduler que terminou sua rodada.
        // Isso permite que o Scheduler salve o contexto e escolha o próximo processo.
        gp.notifyScheduler(); // Notifica o Scheduler que a CPU terminou sua fatia
    }
}