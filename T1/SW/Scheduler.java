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
        // System.out.println(chosenPCB.id);
        // System.out.println(chosenPCB.state);

        hw.cpu.setContext(chosenPCB.pc);
        hw.cpu.updateMMU(chosenPCB.tabPag);
        hw.cpu.reg = chosenPCB.regs;
        chosenPCB.pc = hw.cpu.pc;
        if (hw.mem.pos[GM.tradutor(chosenPCB.pc, chosenPCB.tabPag)].opc == Opcode.STOP
                || hw.cpu.irpt.get() != Interrupts.noInterrupt) {
            chosenPCB.state = State.RUNNING;
        }
        q.add(chosenPCB);
        hw.cpu.procId.set(chosenPCB.id);
        // System.out.println(q.size());
    }
}
