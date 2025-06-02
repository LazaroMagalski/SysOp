package SW;

import HW.HW;
import HW.CPU.Interrupts;
import SW.GP.PCB;

public class InterruptHandling {
    private HW hw; // referencia ao hw se tiver que setar algo
    private GP gp;
    
    public InterruptHandling(HW _hw, GP _gp) {
        hw = _hw;
        gp = _gp;  
    }

    public void handle(Interrupts irpt) {
        System.out.println(
                "                                               Interrupcao " + irpt + "   pc: " + hw.cpu.pc);

        // A CPU não tem um ponteiro direto para o PCB atual.
        // Para intIOCompleta, precisaremos saber QUAL PCB precisa ser desbloqueado.
        // Por enquanto, no teste, vamos assumir que o processo a ser desbloqueado
        // é o único que está bloqueado ou que sabemos seu ID.
        // No cenário real, a interrupção de I/O deveria passar o ID do processo ou o próprio PCB.

        if (irpt == Interrupts.intIOCompleta) {
            System.out.println("Rotina de Interrupção: I/O Completa!");
            // Aqui você precisaria identificar qual processo foi desbloqueado.
            // Por simplicidade no teste, se há apenas um bloqueado, podemos pegá-lo.
            if (!gp.blockedPcbList.isEmpty()) {
                PCB pcbToUnblock = gp.blockedPcbList.peek(); // Pega mas não remove
                if (pcbToUnblock != null) {
                    gp.unblockProcess(pcbToUnblock);
                }
            }
        } else {
            // Para interrupções fatais, o Scheduler já lida com a finalização.
            // Mas a rotina de tratamento pode fazer algo como logar o erro.
            System.out.println("Rotina de Interrupção: Interrupção fatal. O processo será finalizado pelo Scheduler.");
        }
        // Não paramos mais a CPU aqui, o Scheduler é quem gerencia o ciclo.
        // hw.cpu.cpuStop = true; // Remover esta linha
    }
}
