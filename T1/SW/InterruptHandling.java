package SW;

import HW.CPU.Interrupts;
import HW.HW;

public class InterruptHandling {
    private HW hw; // referencia ao hw se tiver que setar algo
    private SO so;

    public InterruptHandling(HW _hw, SO _so) {
        hw = _hw;
        so = _so;
    }

    public void handle(Interrupts irpt) {
        // apenas avisa - todas interrupcoes neste momento finalizam o programa
        if (irpt == Interrupts.intTimer) {
            so.gp.scheduler.schedule(so.gp.nopPCB);
        }
        if (irpt == Interrupts.intPageFault){
            
        }
    }
}
