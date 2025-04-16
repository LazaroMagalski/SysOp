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
        if (i >= gp.pcbList.size()) {
            i = 0;
        }
        PCB chosenPCB = gp.pcbList.get(i);
        if (chosenPCB.ready) {
            hw.cpu.setContext(chosenPCB.pc);
            hw.cpu.updateMMU(chosenPCB.tabPag);
            hw.cpu.run(2);
        }
        gp.pcbList.get(i).pc = hw.cpu.pc;
        i++;
        System.out.println(chosenPCB.id);
    }
}
