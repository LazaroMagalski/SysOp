package SW;

import HW.CPU.CPU;
import HW.CPU.Interrupts;

public class Timer implements Runnable {

    CPU cpu;
    public Timer(CPU _cpu) {
        cpu = _cpu;
    }

    @Override
    public void run() {
        while (true) {
            cpu.irpt.set(Interrupts.intTimer);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
}
