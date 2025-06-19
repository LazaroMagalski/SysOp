package SW;

import HW.CPU.Interrupts;
import SW.GP.PCB;
import SW.GP.State;

import java.util.concurrent.atomic.AtomicReference;

import HW.HW;

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
            for (int i = 0; i < so.gp.pcbList.size(); i++) {
                if (so.gp.procExec == so.gp.pcbList.get(i).id) {
                    so.gp.pcbList.get(i).state = State.READY;
                    break;
                }
            }
        } else if (irpt.get() == Interrupts.intIOCompleta){
            System.out.println("received complete io");
            PCB currPCB;
            if (so.gp.pcbList.size() >= 2) {
                currPCB = so.gp.pcbList.get(1);
                currPCB.state = State.READY;
            } else {
                currPCB = so.gp.scheduler.q.poll();
                while (hw.cpu.procId.get() != currPCB.id) {
                    so.gp.scheduler.q.add(currPCB);
                    currPCB = so.gp.scheduler.q.poll();
                }
                currPCB.state = State.READY;
            }
            currPCB.pc++;
        }
        irpt.set(Interrupts.noInterrupt);
    }
}
