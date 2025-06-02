package SW;

import HW.HW;
import SW.GP.State;
import HW.CPU.*;

public class SysCallHandling {
    private HW hw; // referencia ao hw se tiver que setar algo
    private GP gp;

    public SysCallHandling(HW _hw, GP _gp) {
        hw = _hw;
        gp = _gp;
    }

    public void stop(boolean flag) { // chamada de sistema indicando final de programa
                         // nesta versao cpu simplesmente pára
        if(flag)
            System.out.println("                                               SYSCALL STOP");
            // gp.desalocaProcesso(hw.cpu.pcbAtual.id);
    }

    public void handle() {
        System.out.println("SYSCALL pars:  " + hw.cpu.reg[8] + " / " + hw.cpu.reg[9]);
        // MELHORIA FUTURA: A CPU deve ter uma referência ao PCB do processo que está rodando.
        GP.PCB currentPCB = null;
        for (GP.PCB pcb : gp.pcbList) { // Busca na fila de prontos
            if (pcb.id == gp.procExec) {
                currentPCB = pcb;
                break;
            }
        }
        if (currentPCB == null) { // Se não encontrou na fila de prontos, procura nos bloqueados (não deveria ser o caso aqui)
             for (GP.PCB pcb : gp.blockedPcbList) {
                if (pcb.id == gp.procExec) {
                    currentPCB = pcb;
                    break;
                }
            }
        }

        if (currentPCB == null) {
            System.out.println("ERRO SYSCALL: Não foi possível identificar o PCB do processo em execução.");
            hw.cpu.irpt = Interrupts.intInstrucaoInvalida; // Ou uma interrupção de erro de sistema
            return;
        }

        // SALVA O CONTEXTO DO PROCESSO NA SYSCALL
        currentPCB.pc = hw.cpu.pc; // Salva o PC atual
        System.arraycopy(hw.cpu.reg, 0, currentPCB.regs, 0, hw.cpu.reg.length); // Salva os registradores

        if  (hw.cpu.reg[8]==1){ // Leitura
              // Por enquanto, apenas printa. A thread do console vai lidar com isso.
              System.out.println("SYSCALL IN (leitura) solicitada pelo processo " + currentPCB.id + " para o endereço " + hw.cpu.reg[9]);
              gp.blockProcess(currentPCB); // Bloqueia o processo
              // A CPU PRECISA PARAR DE EXECUTAR ESTE PROCESSO APÓS BLOQUEAR
              // Isso será feito pelo Scheduler ao detectar que o estado mudou.
        } else if (hw.cpu.reg[8]==2){ // Escrita
              System.out.println("SYSCALL OUT (escrita) solicitada pelo processo " + currentPCB.id + " do endereço " + hw.cpu.reg[9] + " (valor: " + hw.mem.pos[hw.cpu.reg[9]].p + ")");
              gp.blockProcess(currentPCB); // Bloqueia o processo
        } else {
            System.out.println("  PARAMETRO INVALIDO EM SYSCALL");
            // Em caso de parâmetro inválido, podemos gerar uma interrupção de instrução inválida, por exemplo.
            hw.cpu.irpt = Interrupts.intInstrucaoInvalida;
        }
        // O PC da CPU será incrementado pelo próprio CPU (pc++ após SYSCALL) antes de bloquear.
        // O escalonador será responsável por escolher o próximo processo.
    }
}