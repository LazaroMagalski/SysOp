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
        // Se não há processos prontos E não há processos bloqueados (aguardando I/O),
        // o Scheduler espera por novos processos ou eventos de desbloqueio.
        if (readyQueue.isEmpty() && blockedQueue.isEmpty()) {
            synchronized (gp.schedulerMonitor) { // Sincroniza no monitor do Scheduler
                try {
                    System.out.println("Scheduler: Nenhuma tarefa. Aguardando novos processos ou I/O.");
                    gp.schedulerMonitor.wait(); // Scheduler espera
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Scheduler thread interrupted: " + e.getMessage());
                    return; // Sai se for interrompido
                }
            }
        }

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
                if (hw.cpu.irpt != Interrupts.noInterrupt) {
                    System.out.println("Scheduler: Processo " + chosenPCB.id + " interrompido por: " + hw.cpu.irpt);
                    if (hw.cpu.irpt == Interrupts.intEnderecoInvalido ||
                        hw.cpu.irpt == Interrupts.intInstrucaoInvalida ||
                        hw.cpu.irpt == Interrupts.intOverflow) {
                        System.out.println("Scheduler: Processo " + chosenPCB.id + " finalizado devido a interrupção fatal.");
                        gp.desalocaProcesso(chosenPCB.id);
                    }
                } else if (hw.mem.pos[GM.tradutor(chosenPCB.pc, chosenPCB.tabPag, this.gp.gm.tamPag)].opc == Opcode.STOP) {
                    System.out.println("Scheduler: Processo " + chosenPCB.id + " encontrou STOP e será finalizado.");
                    gp.desalocaProcesso(chosenPCB.id);
                } else if (chosenPCB.state == State.RUNNING) { // Se ainda estava RUNNING (preempção por tempo)
                    chosenPCB.state = State.READY;
                    readyQueue.add(chosenPCB);
                    System.out.println("Scheduler: Processo " + chosenPCB.id + " preemptado e movido para READY.");
                }
            }
        } else if (!blockedQueue.isEmpty()) {
            System.out.println("Scheduler: Fila de prontos vazia, mas há processos bloqueados. Aguardando I/O.");
            gp.procExec = -1; // CPU ociosa
            // O Scheduler irá esperar novamente se a fila de prontos permanecer vazia após esta rodada.
            // A notificação de I/O completa (Interrupts.intIOCompleta) trará processos para a readyQueue.
            synchronized (gp.schedulerMonitor) { // Sincroniza no monitor do Scheduler
                try {
                    gp.schedulerMonitor.wait(); // Scheduler espera por I/O completo ou novos processos
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Scheduler thread interrupted: " + e.getMessage());
                }
            }
        } else {
            // Este bloco é alcançado se readyQueue estiver vazia mas blockedQueue também estiver vazia.
            // O wait inicial no `schedule()` já cobre isso.
            // Este `else` pode ser removido, ou apenas garantir que o `procExec` está em -1.
            gp.procExec = -1; // CPU ociosa
        }
    }
}