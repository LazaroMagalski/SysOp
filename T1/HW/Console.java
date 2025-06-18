package HW;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import HW.CPU.CPU;
import HW.CPU.Interrupts;
import HW.CPU.Opcode;
import HW.CPU.CPU.Request;
import SW.GM;

public class Console implements Runnable {

    ConcurrentLinkedQueue<Request> requests;
    GM gm;
    CPU cpu;
    Boolean wantsRead;
    Integer result;

    public Console(ConcurrentLinkedQueue<Request> _requests, GM _gm, CPU _cpu, Boolean _wants, Integer _result) {
        requests = _requests;
        gm = _gm;
        cpu = _cpu;
        wantsRead = _wants;
        result = _result;
    }

    @Override
    public void run() {
        while (true) {
            if (!requests.isEmpty()) {
                Request rq = requests.poll();
                switch (rq.request) {
                    case IN:
                        System.out.println("IN");
                        
                        wantsRead = true;
				        System.out.println("Console "+wantsRead);
                        while (wantsRead);
				        System.out.println("Console "+wantsRead);
                        
                        int phys = GM.tradutor(rq.num, cpu.tabPag);
                        gm.memory.pos[phys].opc = Opcode.DATA;
                        gm.memory.pos[phys].p = result;
                        break;
                    case OUT:
                        System.out.println("OUT");
                        result = rq.num;
                        System.out.println(result);
                        break;
                    default:
                        break;
                }
                cpu.irpt = Interrupts.intIOCompleta;
            }
        }
    }
    
}
