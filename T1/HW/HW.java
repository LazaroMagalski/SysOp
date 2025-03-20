package HW;

import HW.CPU.CPU;
import HW.Memory.Memory;

public class HW {
    public Memory mem;
    public CPU cpu;

    public HW(int tamMem) {
        mem = new Memory(tamMem);
        cpu = new CPU(mem, true); // true liga debug
    }
}
