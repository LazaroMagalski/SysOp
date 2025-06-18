package SW;

import HW.CPU.Interrupts;
import SW.GP.PCB;
import SW.GP.State;
import HW.HW;

public class InterruptHandling {
    private HW hw; // referencia ao hw se tiver que setar algo
    public SO so;

    public InterruptHandling(HW _hw, SO _so) {
        hw = _hw;
        so = _so;
    }

    public void handle(Interrupts irpt) {
        // apenas avisa - todas interrupcoes neste momento finalizam o programa
        if (irpt == Interrupts.intTimer) {
            for (int i = 0; i < so.gp.pcbList.size(); i++) {
                if (so.gp.procExec == so.gp.pcbList.get(i).id) {
                    so.gp.pcbList.get(i).state = State.BLOCKED;
                    break;
                }
            }
        }
        if (irpt == Interrupts.intIOCompleta){
            PCB currPCB = so.gp.scheduler.q.poll();
            while (hw.cpu.procId != currPCB.id) {
                so.gp.scheduler.q.add(currPCB);
                currPCB = so.gp.scheduler.q.poll();
            }
            currPCB.state = State.READY;
            System.out.println("READY");
        }
    }
}
