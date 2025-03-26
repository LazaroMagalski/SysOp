package SW;

import java.util.HashMap;

import HW.CPU.CPU;
import VM.Program;
import SW.GM;
import HW.CPU.CPU;

public class GP {

    public class PCB{
        public int id;
        public int[] tabPag;
        public int pc;

        public PCB(){
            id = pcbId;
            pcbId++;
            tabPag = new int[0];
            pc = 0;
        }

    }

    private int pcbId; 
    private GM gm;
    private CPU cpu;
    private HashMap<Integer, PCB> readyList;
    
    public GP(CPU cpu, GM gm){
        this.cpu = cpu;
        this.gm = gm;
    }

    public boolean criaProcesso(Program program) {

        int[] alocacao = new int[program.image.length];
        System.out.println("program.length = "+program.image.length + " gm.tamMem = "+gm.tamMem);
        if(program.image.length > gm.tamMem) return false;//verifica tam do programa
        if(gm.aloca(program.image.length, alocacao)==false){//pede alocacao
           System.out.println("Erro: Memória insuficiente para alocar o programa");
           return false;
        }
        PCB novoPCB = new PCB();//cria pcb
        //Seta partição usada no pcb
        novoPCB.tabPag = alocacao;
        //carrega programa na memória
        for(int i = 0; i < program.image.length; i++){
            gm.memory.pos[novoPCB.tabPag[i]] = program.image[i];
        }
        //Seta demais parâmetros do PCB (id, pc=0, etc)
        //Coloca PCB na fila de prontos
        readyList.put(novoPCB.id, novoPCB);
        return true;
    }

    public void desalocaProcesso(int id){
        PCB pcb = readyList.get(id);
        if(pcb == null){
            System.out.println("Processo inexistente");
            return; 
        }
        gm.desaloca(pcb.tabPag);
        readyList.remove(id);
    }
    
}