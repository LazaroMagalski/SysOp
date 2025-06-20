package SW;

import HW.CPU.Interrupts;
import HW.CPU.Opcode;
import HW.HW;
import SW.GP.PCB;
import SW.GP.State;
import java.util.LinkedList;
import java.util.Queue;

public class Scheduler {
    // private GP gp;
    private HW hw;
    public Queue<PCB> q;

    public Scheduler(GP _gp, HW _hw, LinkedList<PCB> ps) {
        // gp = _gp;
        hw = _hw;
        q = ps;
    }

public void schedule(PCB nopPCB) {
    PCB currPCB = null;
    for (PCB pcb : q) {
        if (pcb.state == State.RUNNING) {
            currPCB = pcb;
            break;
        }
    }
    if (currPCB != null) {
        
        System.arraycopy(hw.cpu.reg, 0, currPCB.regs, 0, hw.cpu.reg.length);
        currPCB.pc = hw.cpu.pc;
        currPCB.state = State.READY; 
    }

    PCB chosenPCB = null;
    for (int i = 0; i < q.size(); i++) {
        chosenPCB = q.poll();
        if (chosenPCB.state == State.READY) {
            break;
        }
        q.add(chosenPCB);
    }
    if (chosenPCB == null) {
        chosenPCB = nopPCB;
    }
    System.arraycopy(chosenPCB.regs, 0, hw.cpu.reg, 0, hw.cpu.reg.length);
    hw.cpu.pc = chosenPCB.pc;
    hw.cpu.setContext(chosenPCB.pc);
    hw.cpu.updateMMU(chosenPCB.tabPag);

    int ef = GM.tradutor(chosenPCB.pc, chosenPCB.tabPag);
    if (ef >= 0 && hw.mem.pos[GM.tradutor(chosenPCB.pc, chosenPCB.tabPag)].opc == Opcode.STOP
            || hw.cpu.irpt.get() != Interrupts.noInterrupt) {
        chosenPCB.state = State.RUNNING;
    }
    q.add(chosenPCB);
    hw.cpu.procId.set(chosenPCB.id);
}
}
