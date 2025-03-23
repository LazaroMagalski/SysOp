package SW;

import java.util.HashMap;

import HW.CPU.CPU;
import VM.Program;
import SW.GM;
import HW.CPU.CPU;

public class GP {

    public class PCB{
        public int id;
        public int[] particoes;
        public int pc;
        public int base;
        public int limite;

        public PCB(){
            id = pcbId;
            pcbId++;
            particoes = new int[0];
            pc = 0;
            base = 0;
            limite = 0;
        }

    }

    private int pcbId; 
    private GM gm;
    private CPU cpu;
    public HashMap<Integer, PCB> listaPCB;
    
    public GP(CPU cpu, GM gm){
        this.cpu = cpu;
        this.gm = gm;
    }

    public boolean criaProcesso(Program program) {

        //verifica tamanho do programa
        int[] alocacao = new int[program.image.length];
        if(program.image.length > gm.tamMem) return false;
        if(gm.aloca(program.image.length, alocacao)==false){
           System.out.println("Erro: Memória insuficiente para alocar o programa");
           return false;
        }
        PCB novoPCB = new PCB();
        novoPCB.particoes = alocacao;
        novoPCB.base = gm.traduz(0, alocacao);
        novoPCB.limite = gm.traduz(program.image.length, alocacao);
        listaPCB.put(novoPCB.id, novoPCB);
        return true;
    }
    
}