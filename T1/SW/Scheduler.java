package SW;

import HW.CPU.Interrupts;
import HW.CPU.Opcode;
import HW.HW;
import SW.GP.PCB;
import SW.GP.State;
import java.util.LinkedList;
import java.util.Queue;

public class Scheduler {
    //private GP gp;
    private HW hw;
    public Queue<PCB> q;

    public Scheduler(GP _gp, HW _hw, LinkedList<PCB> ps) {
        //gp = _gp;
        hw = _hw;
        q = ps;
    }

    public void schedule(PCB nopPCB) {
        PCB chosenPCB = q.poll();
        if (chosenPCB == null) {
            chosenPCB = nopPCB;
        }

        if (chosenPCB.state == State.READY) {
            hw.cpu.setContext(chosenPCB.pc);
            hw.cpu.updateMMU(chosenPCB.tabPag);
            hw.cpu.reg = chosenPCB.regs;
        }
        if (hw.mem.pos[GM.tradutor(chosenPCB.pc, chosenPCB.tabPag)].opc == Opcode.STOP || hw.cpu.irpt != Interrupts.noInterrupt) {
            chosenPCB.state = State.RUNNING;
        }
        chosenPCB.pc = hw.cpu.pc;
        q.add(chosenPCB);
        hw.cpu.procId = chosenPCB.id;
    }
}
