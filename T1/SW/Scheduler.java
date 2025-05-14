package SW;

import java.util.Queue;
import java.util.LinkedList;

import HW.HW;
import HW.CPU.Interrupts;
import HW.CPU.Opcode;
import SW.GP.*;

public class Scheduler {
    //private GP gp;
    private HW hw;
    private Queue<PCB> q;

    public Scheduler(GP _gp, HW _hw, LinkedList<PCB> ps) {
        //gp = _gp;
        hw = _hw;
        q = ps;
    }

    public void schedule() {
        PCB chosenPCB = q.poll();
        if (chosenPCB.state == State.READY) {
            hw.cpu.setContext(chosenPCB.pc);
            hw.cpu.updateMMU(chosenPCB.tabPag);
            hw.cpu.reg = chosenPCB.regs;
            hw.cpu.run(2);
            System.out.println(chosenPCB.id);
        }
        if (hw.mem.pos[GM.tradutor(chosenPCB.pc, chosenPCB.tabPag)].opc == Opcode.STOP || hw.cpu.irpt != Interrupts.noInterrupt) {
            chosenPCB.state = State.RUNNING;
        }
        chosenPCB.pc = hw.cpu.pc;
        q.add(chosenPCB);
    }
}
