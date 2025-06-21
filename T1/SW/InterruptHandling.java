package SW;

import HW.CPU.Interrupts;
import HW.HW;
import SW.GP.PCB;
import SW.GP.State;
import java.util.concurrent.atomic.AtomicReference;

public class InterruptHandling {
    private HW hw; // referencia ao hw se tiver que setar algo
    public SO so;

    public InterruptHandling(HW _hw, SO _so) {
        hw = _hw;
        so = _so;
    }

    public void handle(AtomicReference<Interrupts> irpt, boolean pagefault) {

        if (irpt.get() == Interrupts.intIOCompleta) {
            System.out.println("received complete io");
            PCB currPCB;
            currPCB = so.gp.scheduler.q.poll();
            while (hw.cpu.procId.get() != currPCB.id) {
                so.gp.scheduler.q.add(currPCB);
                currPCB = so.gp.scheduler.q.poll();
            }
            so.gp.scheduler.q.add(currPCB);
            currPCB.state = State.READY;
            currPCB.pc++;
        }
        else if (irpt.get() == Interrupts.intPageFault) {
            System.out.println("Tratando page fault");
            int procId = hw.cpu.procId.get();
            PCB currPCB = null;
            for (PCB pcb : so.gp.pcbList) {
                if (pcb.id == procId) {
                    currPCB = pcb;
                    break;
                }
            }
            if (currPCB == null) {
                System.out.println("PCB não encontrado para tratamento de page fault.");
                irpt.set(Interrupts.noInterrupt);
                return;
            }

            int enderecoLogico = currPCB.pc;
            int tamPag = SW.GM.tamPag;
            int numPagina = enderecoLogico / tamPag;

            int frameLivre = -1;

            // 1. Tenta pegar um frame livre
            if (!so.gm.freeFrames.isEmpty()) {
                frameLivre = so.gm.freeFrames.pop();
                System.out.println("Frame livre encontrado: " + frameLivre);
            } else {
                // 2. Se não houver frame livre, substitui uma página já carregada (exceto a
                // requisitada)
                int paginaSubstituida = -1;
                for (int i = 0; i < currPCB.tabPag.length; i++) {
                    if (currPCB.tabPag[i] != -1 && i != numPagina) {
                        paginaSubstituida = i;
                        frameLivre = currPCB.tabPag[i];
                        break;
                    }
                }
                if (paginaSubstituida != -1) {
                    so.gm.salvaPaginaNoDisco(frameLivre, so.gp.disco);
                    currPCB.tabPag[paginaSubstituida] = -1;
                    System.out.println("Substituindo página " + paginaSubstituida + " do frame " + frameLivre);
                } else {
                    System.out.println("ERRO: Não há frame livre nem página para substituir!");
                    irpt.set(Interrupts.noInterrupt);
                    return;
                }
            }

            // Carrega a página requisitada do disco para o frame livre
            so.gm.carregaPaginaDoDisco(numPagina, frameLivre, so.gp.disco);
            currPCB.tabPag[numPagina] = frameLivre;

            System.out.println("Page fault tratado: página " + numPagina + " carregada no frame " + frameLivre);
        } else if (irpt.get() == Interrupts.intTimer && !pagefault) {
            so.gp.scheduler.schedule(so.gp.nopPCB);
        }
        irpt.set(Interrupts.noInterrupt);
    }
}
