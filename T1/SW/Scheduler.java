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
        // Se a CPU parou ou teve uma interrupção fatal (não-SYSCALL)
        // O processo que estava rodando antes de parar precisa ser removido.
        // O PCB do processo que estava em execução na CPU precisa ser identificado.
        // No momento, a CPU não tem um ponteiro direto para o PCB.
        // Isso é uma lacuna que precisa ser resolvida para um gerenciamento preciso.
        // Por agora, o GP tem o 'procExec' que aponta para o ID do processo.
        // O scheduler deve ser chamado periodicamente pelo SO ou por uma thread da CPU.

        // Lógica de Preempção ou Término
        PCB currentRunningPCB = null;
        if (gp.procExec != -1 && !readyQueue.isEmpty()) { // Se tem um processo que estava 'rodando' e não é o dummy
             for (PCB pcb : readyQueue) { // Busca o PCB do processo que estava em 'running' (se houver)
                 if (pcb.id == gp.procExec && pcb.state == State.RUNNING) { // Identifica o processo que estava em execução
                     currentRunningPCB = pcb;
                     break;
                 }
             }
             // Se o processo que estava em 'running' não foi encontrado na readyQueue,
             // pode ser que ele tenha sido bloqueado ou finalizado.

            if (currentRunningPCB != null) {
                // Salva o contexto do processo que acabou de rodar sua fatia de tempo
                currentRunningPCB.pc = hw.cpu.pc; // Salva o Program Counter
                System.arraycopy(hw.cpu.reg, 0, currentRunningPCB.regs, 0, hw.cpu.reg.length); // Salva os registradores

                // Determina o que aconteceu com o processo que estava na CPU
                if (hw.cpu.irpt != Interrupts.noInterrupt) { // Se houve uma interrupção
                    System.out.println("Scheduler: Processo " + currentRunningPCB.id + " interrompido por: " + hw.cpu.irpt);
                    if (hw.cpu.irpt == Interrupts.intEnderecoInvalido ||
                        hw.cpu.irpt == Interrupts.intInstrucaoInvalida ||
                        hw.cpu.irpt == Interrupts.intOverflow) { // Interrupções fatais
                        System.out.println("Scheduler: Processo " + currentRunningPCB.id + " finalizado devido a interrupção fatal.");
                        gp.desalocaProcesso(currentRunningPCB.id); // Finaliza e desaloca
                        currentRunningPCB = null; // Não adicionar de volta
                    }
                    // Interrupções de I/O serão tratadas pela rotina de interrupção, que chamará gp.unblockProcess()
                    // Por enquanto, outras interrupções ainda param a CPU (no InterruptHandling).
                } else if (hw.mem.pos[GM.tradutor(currentRunningPCB.pc, currentRunningPCB.tabPag)].opc == Opcode.STOP) { // Se a instrução atual é STOP
                    System.out.println("Scheduler: Processo " + currentRunningPCB.id + " encontrou STOP e será finalizado.");
                    gp.desalocaProcesso(currentRunningPCB.id); // Finaliza e desaloca
                    currentRunningPCB = null; // Não adicionar de volta
                } else if (currentRunningPCB.state == State.RUNNING) { // Se ainda estava RUNNING e não parou/bloqueou
                    currentRunningPCB.state = State.READY; // Volta para READY
                    readyQueue.add(currentRunningPCB); // Adiciona de volta na fila de prontos
                    System.out.println("Scheduler: Processo " + currentRunningPCB.id + " preemptado e movido para READY.");
                }
                // Se o estado do currentRunningPCB já mudou para BLOCKED pelo SysCallHandling,
                // ele já foi movido para blockedQueue e não será adicionado de volta aqui.
            }
        }


        // 2. Escolher o próximo processo para executar
        PCB chosenPCB = null;
        // Priorizar a execução de um processo que esteja READY
        if (!readyQueue.isEmpty()) {
            chosenPCB = readyQueue.poll(); // Pega o próximo da fila de prontos
            if (chosenPCB != null) {
                chosenPCB.state = State.RUNNING; // Define o estado como RUNNING
                gp.procExec = chosenPCB.id; // Atualiza o ID do processo em execução no GP
                System.out.println("Scheduler: Processo " + chosenPCB.id + " escolhido para execução.");

                // Carrega o contexto do processo escolhido na CPU
                hw.cpu.setContext(chosenPCB.pc);
                hw.cpu.updateMMU(chosenPCB.tabPag);
                System.arraycopy(chosenPCB.regs, 0, hw.cpu.reg, 0, chosenPCB.regs.length); // Carrega os registradores

                // Reseta a interrupção da CPU para a próxima execução
                hw.cpu.irpt = Interrupts.noInterrupt; // Limpa qualquer interrupção anterior

                hw.cpu.run(2); // Roda por 2 instruções (fatia de tempo)
            }
        } else {
            System.out.println("Scheduler: Fila de prontos vazia. CPU ociosa.");
            gp.procExec = -1; // Sinaliza que nenhum processo está executando
        }

        // Após a execução da fatia de tempo, o processo pode ter parado, bloqueado,
        // ou a fatia de tempo pode ter terminado.
        // A lógica de salvar o contexto e mudar o estado já está no bloco inicial do schedule,
        // mas precisamos garantir que o SysCallHandling também salve e bloqueie o processo.
        // No momento, o `hw.cpu.run(2)` retorna o controle para o `schedule()`,
        // que então pode avaliar o estado final da CPU (`hw.cpu.irpt` ou `hw.mem.pos[...].opc`).
    }
}