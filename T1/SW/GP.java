package SW;

import java.util.HashMap;

import HW.CPU.CPU;
import HW.Memory.Memory;
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
    public HashMap<Integer, PCB> readyList;
    public HashMap<Integer, PCB> pcbList;
    public int procExec;
    public Memory memory;
    
    public GP(CPU cpu, GM gm){
        this.memory = new Memory(1024);
        this.cpu = cpu;
        this.gm = new GM(memory, 10);
        this.readyList = new HashMap<>();
        this.pcbList = new HashMap<>();
        this.procExec = 0;
    }


    public boolean criaProcesso(Program program) {

        int[] alocacao = gm.aloca(program.image.length);
        //System.out.println("program.length = "+program.image.length + " gm.tamMem = "+gm.tamMem);
        if(program.image.length > gm.tamMem) return false;//verifica tam do programa
        if(alocacao == null){//pede alocacao
           System.out.println("Erro: Memória insuficiente para alocar o programa");
           return false;
        }
        PCB novoPCB = new PCB();//cria pcb
        //Seta partição usada no pcb
        novoPCB.tabPag = alocacao;//<- 0

        //carrega programa na memória
        //for(int i = 0; i < program.image.length; i++){
          //  gm.memory.pos[novoPCB.tabPag[i]] = program.image[i];
            //System.out.println(program.image[i].p);
        //}
        gm.carregarPrograma(program.image, alocacao);
        //Seta demais parâmetros do PCB (id, pc=0, etc)
        //Coloca PCB na fila de prontos
        pcbList.put(novoPCB.id, novoPCB);
        readyList.put(novoPCB.id, novoPCB);
        return true;
    }

    public boolean desalocaProcesso(int id){
        PCB pcb = pcbList.get(id);
        if(pcb == null){
            System.out.println("Processo inexistente");
            return false; 
        }
        gm.desaloca(pcb.tabPag);
        readyList.remove(id);
        pcbList.remove(id);
        return true;
    }

    public void executarProcesso(int id_processo){
        PCB pcb = pcbList.get(id_processo);

        if(pcb == null){
            System.out.println("Processo não existe");
            return;
        }
        pcb = readyList.remove(id_processo);
        if(pcb == null){
            System.out.println("Processo ocupado");
            return;
        }
        if(procExec == id_processo){
            System.out.println("Processo em execução");
            return;
        }
        procExec = id_processo;

        cpu.setContext(id_processo);
        cpu.run();

        readyList.put(id_processo, pcb);
    }
    public int getPcbId() {
        return pcbId;
    }
    public void dump(int id){
       PCB pcb = pcbList.get(id);
       if(pcb == null){
            System.out.println("Processo invalido");
            return;
       }
       System.out.println("PCB ID: "+pcb.id);
       System.out.println("PCB PC: "+pcb.pc);
       System.out.println("TabPag");
       for(int i=0; i < pcb.tabPag.length; i++){
            
       }
    }

    public void setPcbId(int pcbId) {
        this.pcbId = pcbId;
    }

    
}