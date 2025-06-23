package HW.CPU;

import HW.Memory.Memory;
import HW.Memory.Word;
import SW.GM;
import SW.GP.State;
import SW.InterruptHandling;
import SW.SysCallHandling;
import SW.Utilities;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CPU implements Runnable {
    public enum RequestType {
        IN, OUT
    }

    public class Request {
        public RequestType request;
        public int num;
        public int procId;
    }

    private int maxInt; // valores maximo e minimo para inteiros nesta cpu
    private int minInt;
    // CONTEXTO da CPU ...
    public int pc; // ... composto de program counter,
    public Word ir; // instruction register,
    public int[] reg; // registradores da CPU
    public static AtomicReference<Interrupts> irpt; // durante instrucao, interrupcao pode ser sinalizada
    // FIM CONTEXTO DA CPU: tudo que precisa sobre o estado de um processo para
    // executa-lo
    // nas proximas versoes isto pode modificar

    private Memory m; // m é o array de memória "física", CPU tem uma ref a m para acessar

    private InterruptHandling ih; // significa desvio para rotinas de tratamento de Int - se int ligada, desvia
    private SysCallHandling sysCall; // significa desvio para tratamento de chamadas de sistema

    private boolean cpuStop; // flag para parar CPU - caso de interrupcao que acaba o processo, ou chamada
                             // stop -
                             // nesta versao acaba o sistema no fim do prog

    // auxilio aa depuração
    private boolean debug; // se true entao mostra cada instrucao em execucao
    private Utilities u; // para debug (dump)
    public int[] tabPag;

    public CPU(Memory _mem, boolean _debug) { // ref a MEMORIA passada na criacao da CPU
        maxInt = 32767; // capacidade de representacao modelada
        minInt = -32767; // se exceder deve gerar interrupcao de overflow
        m = _mem; // usa o atributo 'm' para acessar a memoria, só para ficar mais pratico
        reg = new int[10]; // aloca o espaço dos registradores - regs 8 e 9 usados somente para IO [x]

        debug = _debug; // se true, print da instrucao em execucao

        requests = new ConcurrentLinkedQueue<>();
        irpt = new AtomicReference<>();
        irpt.set(Interrupts.noInterrupt);
        procId = new AtomicInteger(0);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setAddressOfHandlers(InterruptHandling _ih, SysCallHandling _sysCall) {
        ih = _ih; // aponta para rotinas de tratamento de int
        sysCall = _sysCall; // aponta para rotinas de tratamento de chamadas de sistema
    }

    public void setUtilities(Utilities _u) {
        u = _u; // aponta para rotinas utilitárias - fazer dump da memória na tela
    }

    // verificação de enderecamento
    private boolean legal(int e) { // todo acesso a memoria tem que ser verificado se é válido -
                                   // aqui no caso se o endereco é um endereco valido em toda memoria
        if (e >= 0 && e < m.pos.length) {
            return true;
        } else {
            irpt.set(Interrupts.intEnderecoInvalido); // se nao for liga interrupcao no meio da exec da instrucao
            return false;
        }
    }

    private boolean testOverflow(int v) { // toda operacao matematica deve avaliar se ocorre overflow
        if ((v < minInt) || (v > maxInt)) {
            irpt.set(Interrupts.intOverflow); // se houver liga interrupcao no meio da exec da instrucao
            return false;
        }
        ;
        return true;
    }

    public void setContext(int _pc) { // usado para setar o contexto da cpu para rodar um processo
                                      // [ nesta versao é somente colocar o PC na posicao 0 ]
        pc = _pc; // pc cfe endereco logico
        irpt.set(Interrupts.noInterrupt); // reset da interrupcao registrada
    }

    public void updateMMU(int[] tabPag) {
        this.tabPag = tabPag;
    }

    public ConcurrentLinkedQueue<Request> requests;
    public AtomicInteger procId;

    @Override
    public void run() { // execucao da CPU supoe que o contexto da CPU, vide acima,
                        // esta devidamente setado

        while (true) { // ciclo de instrucoes. acaba cfe resultado da exec da instrucao, veja cada
                       // caso.

            // --------------------------------------------------------------------------------------------------
            // FASE DE FETCH
            int enderecoFisico = GM.tradutor(pc, tabPag);

            if (irpt.get() != Interrupts.noInterrupt) { // existe interrupção
                ih.handle(irpt, true); // desvia para rotina de tratamento - esta rotina é do SO
                cpuStop = true; // nesta versao, para a CPU
            }

            if (legal(enderecoFisico) && enderecoFisico >= 0) { // pc valido
                ir = m.pos[enderecoFisico]; // FETCH
                if (debug) {
                    System.out.print("                                             regs: ");
                    for (int i = 0; i < 10; i++) {
                        System.out.print(" r[" + i + "]:" + reg[i]);
                    }
                    ;
                    System.out.println();
                }
                if (debug) {
                    System.out.print("                      pc: " + pc + "       exec: ");
                    u.dump(ir);
                }

                // --------------------------------------------------------------------------------------------------
                // FASE DE EXECUCAO DA INSTRUCAO CARREGADA NO ir
                switch (ir.opc) { // conforme o opcode (código de operação) executa

                    // Instrucoes de Busca e Armazenamento em Memoria
                    case LDI: // Rd ← k veja a tabela de instrucoes do HW simulado para entender a semantica
                              // da instrucao
                        reg[ir.ra] = ir.p; // instruction register.parametro
                        pc++;
                        break;
                    case LDD: // Rd <- [A]
                        if (legal(ir.p)) {
                            int ef = GM.tradutor(ir.p, tabPag);
                            if (ef >= 0) {
                                reg[ir.ra] = m.pos[ef].p;
                                pc++;
                            }
                        }
                        break;
                    case LDX: // RD <- [RS]
                        if (legal(reg[ir.rb])) {
                            int ef = GM.tradutor(reg[ir.rb], tabPag);
                            if (ef >= 0) {
                                reg[ir.ra] = m.pos[ef].p;
                                pc++;
                            }
                        }
                        break;
                    case STD: // [A] ← Rs
                        if (legal(ir.p)) {
                            int ef = GM.tradutor(ir.p, tabPag);
                            if (ef >= 0) {
                                m.pos[ef].opc = Opcode.DATA;
                                m.pos[ef].p = reg[ir.ra];
                                pc++;
                                if (debug) {
                                    System.out.print("                                  ");
                                    u.dump(ir.p, ir.p + 1);
                                }
                            }
                        }
                        break;
                    case STX: // [Rd] ←Rs
                        if (legal(reg[ir.ra])) {
                            int ef = GM.tradutor(reg[ir.ra], tabPag);
                            if (ef >= 0) {
                                m.pos[ef].opc = Opcode.DATA;
                                m.pos[ef].p = reg[ir.rb];
                                pc++;
                            }
                        }
                        break;
                    case MOVE: // RD <- RS
                        reg[ir.ra] = reg[ir.rb];
                        pc++;
                        break;
                    // Instrucoes Aritmeticas
                    case ADD: // Rd ← Rd + Rs
                        reg[ir.ra] = reg[ir.ra] + reg[ir.rb];
                        testOverflow(reg[ir.ra]);
                        pc++;
                        break;
                    case ADDI: // Rd ← Rd + k
                        reg[ir.ra] = reg[ir.ra] + ir.p;
                        testOverflow(reg[ir.ra]);
                        pc++;
                        break;
                    case SUB: // Rd ← Rd - Rs
                        reg[ir.ra] = reg[ir.ra] - reg[ir.rb];
                        testOverflow(reg[ir.ra]);
                        pc++;
                        break;
                    case SUBI: // RD <- RD - k // NOVA
                        reg[ir.ra] = reg[ir.ra] - ir.p;
                        testOverflow(reg[ir.ra]);
                        pc++;
                        break;
                    case MULT: // Rd <- Rd * Rs
                        reg[ir.ra] = reg[ir.ra] * reg[ir.rb];
                        testOverflow(reg[ir.ra]);
                        pc++;
                        break;

                    // Instrucoes JUMP
                    case JMP: // PC <- k
                        pc = ir.p;
                        break;
                    case JMPIM: // PC <- [A]
                        pc = m.pos[ir.p].p;
                        break;
                    case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1
                        if (reg[ir.rb] > 0) {
                            pc = reg[ir.ra];
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIGK: // If RC > 0 then PC <- k else PC++
                        if (reg[ir.rb] > 0) {
                            pc = ir.p;
                        } else {
                            pc++;
                        }
                        break;
                    case JMPILK: // If RC < 0 then PC <- k else PC++
                        if (reg[ir.rb] < 0) {
                            pc = ir.p;
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIEK: // If RC = 0 then PC <- k else PC++
                        if (reg[ir.rb] == 0) {
                            pc = ir.p;
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIL: // if Rc < 0 then PC <- Rs Else PC <- PC +1
                        if (reg[ir.rb] < 0) {
                            pc = reg[ir.ra];
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIE: // If Rc = 0 Then PC <- Rs Else PC <- PC +1
                        if (reg[ir.rb] == 0) {
                            pc = reg[ir.ra];
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIGM: // If RC > 0 then PC <- [A] else PC++
                        if (legal(ir.p)) {
                            if (reg[ir.rb] > 0) {
                                pc = m.pos[ir.p].p;
                            } else {
                                pc++;
                            }
                        }
                        break;
                    case JMPILM: // If RC < 0 then PC <- k else PC++
                        if (reg[ir.rb] < 0) {
                            pc = m.pos[ir.p].p;
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIEM: // If RC = 0 then PC <- k else PC++
                        if (reg[ir.rb] == 0) {
                            pc = m.pos[ir.p].p;
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIGT: // If RS>RC then PC <- k else PC++
                        if (reg[ir.ra] > reg[ir.rb]) {
                            pc = ir.p;
                        } else {
                            pc++;
                        }
                        break;

                    case DATA: // pc está sobre área supostamente de dados
                        irpt.set(Interrupts.intInstrucaoInvalida);
                        break;

                    // Chamadas de sistema
                    case SYSCALL:
                        sysCall.handle(); // <<<<< aqui desvia para rotina de chamada de sistema, no momento so
                                          // temos IO
                        pc++;
                        break;

                    case STOP: // por enquanto, para execucao
                        sysCall.stop(debug);
                        // Desaloca o processo automaticamente ao terminar
                        if (ih != null && ih.so != null && ih.so.gp != null) {
                            ih.so.gp.desalocaProcesso(procId.get());
                            // Troca para o processo NOP após desalocar
                            procId.set(ih.so.gp.nopPCB.id);
                            updateMMU(ih.so.gp.nopPCB.tabPag);
                            setContext(0); // Reinicia o PC para o NOP
                        }
                        cpuStop = false; // Permite continuar execução com NOP
                        break;
                    case NOP:
                        // System.out.println("nop");
                        // for (int i = 0; i < ih.so.gp.pcbList.size(); i++) {
                        // if (procId == ih.so.gp.pcbList.get(i).id) {
                        // System.out.println(ih.so.gp.pcbList.get(i).state);
                        // break;
                        // }
                        // }
                        break;

                    case IN:
                        System.out.println("IN " + procId.get());
                        Request rqi = new Request();
                        rqi.request = RequestType.IN;
                        rqi.num = ir.ra;
                        rqi.procId = procId.get();
                        requests.add(rqi);
                        for (int i = 0; i < ih.so.gp.pcbList.size(); i++) {
                            if (procId.get() == ih.so.gp.pcbList.get(i).id) {
                                ih.so.gp.pcbList.get(i).state = State.BLOCKED;
                                break;
                            }
                        }
                        ih.so.gp.scheduler.schedule(ih.so.gp.nopPCB);
                        break;

                    case OUT:
                        System.out.println("OUT");
                        Request rqo = new Request();
                        rqo.request = RequestType.OUT;
                        rqo.num = ir.ra;
                        requests.add(rqo);
                        for (int i = 0; i < ih.so.gp.pcbList.size(); i++) {
                            if (procId.get() == ih.so.gp.pcbList.get(i).id) {
                                ih.so.gp.pcbList.get(i).state = State.BLOCKED;
                                break;
                            }
                        }
                        break;

                    // Inexistente
                    default:
                        irpt.set(Interrupts.intInstrucaoInvalida);
                        break;
                }
            }
            if (irpt.get() != Interrupts.noInterrupt) { // existe interrupção
                ih.handle(irpt, false); // desvia para rotina de tratamento - esta rotina é do SO
                cpuStop = true; // nesta versao, para a CPU
            }
        } // FIM DO CICLO DE UMA INSTRUÇÃO
    }
}