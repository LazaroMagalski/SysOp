package HW;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import HW.CPU.CPU;
import HW.CPU.Interrupts;
import HW.CPU.Opcode;
import HW.CPU.CPU.Request;
import SW.GM;
import SW.GP;

public class Console implements Runnable {

    ConcurrentLinkedQueue<Request> requests;
    GM gm;
    CPU cpu;
    GP gp;
    AtomicBoolean wantsRead;
    AtomicInteger result;

    public Console(ConcurrentLinkedQueue<Request> _requests, GM _gm, CPU _cpu, AtomicBoolean _wants, AtomicInteger _result, GP _gp) {
        requests = _requests;
        gm = _gm;
        cpu = _cpu;
        wantsRead = _wants;
        result = _result;
        gp = _gp;
    }

    @Override
    public void run() {
        while (true) {
            if (!requests.isEmpty()) {
                Request rq = requests.poll();

                switch (rq.request) {
                    case IN:
                        wantsRead.set(true);
                        while (wantsRead.get());
                        
                        int[] tabPag = null;
                        for (int i = 0; i < gp.pcbList.size(); i++) {
                            if (rq.procId == gp.pcbList.get(i).id) {
                                tabPag = gp.pcbList.get(i).tabPag;
                                break;
                            }
                        }
                        int phys = GM.tradutor(rq.num, tabPag);
                        gm.memory.pos[phys].opc = Opcode.DATA;
                        gm.memory.pos[phys].p = result.get();
                        break;
                    case OUT:
                        System.out.println("OUT");
                        result.set(rq.num);
                        System.out.println(result);
                        break;
                    default:
                        break;
                }
                cpu.irpt.set(Interrupts.intIOCompleta);
                cpu.procId.set(rq.procId);
                System.out.println("sent complete io");
            }
        }
    }
    
}
