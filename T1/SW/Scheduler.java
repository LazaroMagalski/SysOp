package SW;

import HW.HW;
import SW.GP.*;

public class Scheduler {
    private GP gp;
    private HW hw;
    private int i;

    public Scheduler(GP _gp, HW _hw) {
        gp = _gp;
        hw = _hw;
        i = 0;
    }

    public void schedule() {
        PCB chosenPCB;
        if (i >= gp.pcbList.size()+gp.readyList.size()) {
            i = 0;
            chosenPCB = gp.readyList.get(i++);
        } else {
            chosenPCB = gp.readyList.get(i++);
        }
    }
}
