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

    public void handle(AtomicReference<Interrupts> irpt) {
        // apenas avisa - todas interrupcoes neste momento finalizam o programa
        if (irpt.get() == Interrupts.intTimer) {
            so.gp.scheduler.schedule(so.gp.nopPCB);
        } else if (irpt.get() == Interrupts.intIOCompleta) {
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
        if (irpt.get() == Interrupts.intPageFault) {
            
            System.out.println("Tratando page fault");
        }
        irpt.set(Interrupts.noInterrupt);
    }
}
