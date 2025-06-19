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

    public void schedule(PCB nopPCB, PCB currRunningPCB) {
        PCB chosenPCB = q.poll();
        boolean choseOne = false;
        if (chosenPCB.state == State.READY) {
            choseOne = true;
        }
        if (!choseOne) {
            q.add(chosenPCB);
            for (int i = 1; i < q.size(); i++) {
                chosenPCB = q.poll();
                if (chosenPCB.state == State.READY) {
                    choseOne = true;
                    break;
                }
                q.add(chosenPCB);
            }
            if (!choseOne) {
                chosenPCB = nopPCB;
            }
        }
        // System.out.println(chosenPCB.id);
        // System.out.println(chosenPCB.state);

        hw.cpu.setContext(chosenPCB.pc);
        hw.cpu.updateMMU(chosenPCB.tabPag);
        hw.cpu.reg = chosenPCB.regs;
        if (hw.mem.pos[GM.tradutor(chosenPCB.pc, chosenPCB.tabPag)].opc == Opcode.STOP
                || hw.cpu.irpt.get() != Interrupts.noInterrupt) {
            chosenPCB.state = State.RUNNING;
        }
        chosenPCB.pc = hw.cpu.pc;
        q.add(chosenPCB);
        hw.cpu.procId.set(chosenPCB.id);
    }
}
