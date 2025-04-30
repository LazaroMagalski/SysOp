import java.util.Arrays;

import HW.CPU.CPU;
import HW.Memory.Memory;
import SW.GM;
import SW.GP;
import VM.Program;
import VM.Programs;

public class Testes {

    public static void main(String[] args) {
        Testes t = new Testes();
        //t.testaSucessoAlocaGM();
        t.testaCriacaoProcesso();
    }   
    
    public void testaSucessoAlocaGM(){
        Memory memory = new Memory(1024);//128 frames
        GM gm = new GM(memory, 16);
        int[] paginas = new int[100];
        System.out.println("tamMem: "+gm.tamMem);
        System.out.println("frames: "+gm.frames);
        System.out.println("tamPag: "+gm.tamPag);
       // System.out.println(Arrays.toString(paginas));
        gm.aloca(100);
        System.out.println(Arrays.toString(paginas));
       // gm.desaloca(paginas);
       // System.out.println(Arrays.toString(paginas));
        //------
        //int bottom = gm.traduz(0, paginas);
        //int top = gm.traduz(paginas.length, paginas);
        //System.out.println("bottom = "+bottom);
        //System.out.println("top = "+top);
    }
    public void testaCriacaoProcesso(){
        Memory memory = new Memory(1024);
        CPU cpu = new CPU(memory, false);
        GM gm = new GM(memory, 64);
        GP gp = new GP(cpu, gm);
        Programs programs = new Programs();
        Program program = programs.progs[0];
        System.out.println("name: "+program.name+" "+gp.criaProcesso(program));
    }
}