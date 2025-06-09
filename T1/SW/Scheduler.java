package SW;

import java.util.Queue;
import java.util.LinkedList; // Certifique-se de ter este import

import HW.HW;
import HW.CPU.Interrupts;
import HW.CPU.Opcode;
import SW.GP.PCB;
import SW.GP.State; // Importa o enum State

public class Scheduler {
    private GP gp; // Adicionando referência ao GP para usar seus métodos de bloqueio/desbloqueio e listas
    private HW hw;
    private Queue<PCB> readyQueue; // Renomeado de 'q' para clareza
    private Queue<PCB> blockedQueue; // feat: Fila de bloqueados

    public Scheduler(GP _gp, HW _hw, LinkedList<PCB> _readyQueue, LinkedList<PCB> _blockedQueue) { // Construtor atualizado
        gp = _gp;
        hw = _hw;
        readyQueue = _readyQueue; //
        blockedQueue = _blockedQueue; //
    }

    public void schedule() {
        // 1. Limpeza de processos finalizados/interrompidos fatalmente na CPU
        //    Esta lógica foi movida para depois da CPU executar,
        //    pois o Scheduler precisa do estado final da CPU.

        // 2. Escolher o próximo processo para executar
        PCB chosenPCB = null;
        if (!readyQueue.isEmpty()) {
            chosenPCB = readyQueue.poll();
            if (chosenPCB != null) {
                chosenPCB.state = State.RUNNING;
                gp.procExec = chosenPCB.id;
                System.out.println("Scheduler: Processo " + chosenPCB.id + " escolhido para execução.");

                // Carrega o contexto do processo escolhido na CPU
                hw.cpu.setContext(chosenPCB.pc);
                hw.cpu.updateMMU(chosenPCB.tabPag);
                System.arraycopy(chosenPCB.regs, 0, hw.cpu.reg, 0, chosenPCB.regs.length);
                hw.cpu.irpt = Interrupts.noInterrupt; // Limpa qualquer interrupção anterior

                // A CPU já está rodando em sua própria Thread (ver Sistema.java).
                // Precisamos apenas acordá-la e esperar que ela termine sua fatia.
                gp.wakeCPU(); // Acorda a CPU para executar este processo

                // Scheduler aguarda a CPU terminar sua fatia de tempo ou um evento
                synchronized (gp.schedulerMonitor) { // Sincroniza no monitor do Scheduler
                    try {
                        gp.schedulerMonitor.wait(); // Scheduler espera a CPU terminar
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Scheduler thread interrupted: " + e.getMessage());
                    }
                }

                // A CPU acordou o Scheduler. Agora, o Scheduler verifica o que aconteceu.
                // O contexto (PC e regs) já foi salvo no PCB pela CPU antes de notificar.

                // Verifica o estado final do processo que acabou de rodar
                if (hw.cpu.irpt != Interrupts.noInterrupt) { // Se houve uma interrupção
                    System.out.println("Scheduler: Processo " + chosenPCB.id + " interrompido por: " + hw.cpu.irpt);
                    if (hw.cpu.irpt == Interrupts.intEnderecoInvalido ||
                        hw.cpu.irpt == Interrupts.intInstrucaoInvalida ||
                        hw.cpu.irpt == Interrupts.intOverflow) { // Interrupções fatais
                        System.out.println("Scheduler: Processo " + chosenPCB.id + " finalizado devido a interrupção fatal.");
                        gp.desalocaProcesso(chosenPCB.id); // Finaliza e desaloca
                    }
                    // Interrupções de I/O (intIOCompleta) são tratadas por InterruptHandling
                    // e movem o PCB para READY antes do scheduler rodar.
                } else if (hw.mem.pos[GM.tradutor(chosenPCB.pc, chosenPCB.tabPag)].opc == Opcode.STOP) { // Se a instrução atual é STOP
                    System.out.println("Scheduler: Processo " + chosenPCB.id + " encontrou STOP e será finalizado.");
                    gp.desalocaProcesso(chosenPCB.id); // Finaliza e desaloca
                } else if (chosenPCB.state == State.RUNNING) { // Se ainda estava RUNNING (preempção por tempo)
                    chosenPCB.state = State.READY; // Volta para READY
                    readyQueue.add(chosenPCB); // Adiciona de volta na fila de prontos
                    System.out.println("Scheduler: Processo " + chosenPCB.id + " preemptado e movido para READY.");
                }
                // Se o estado do chosenPCB já mudou para BLOCKED pelo SysCallHandling,
                // ele já foi movido para blockedQueue e não será adicionado de volta aqui.
            }
        } else {
            System.out.println("Scheduler: Fila de prontos vazia. CPU ociosa.");
            gp.procExec = -1; // Sinaliza que nenhum processo está executando
            // O Scheduler agora também precisa esperar se não há processos para executar.
            // Isso será tratado no loop principal do SO/Sistema.
        }
    }
}