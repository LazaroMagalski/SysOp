package SW;

import java.util.HashMap;

import HW.HW;
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
        public boolean ready;

        public PCB(){
            id = pcbId;
            pcbId++;
            tabPag = new int[0];
            pc = 0;
            ready = true;
        }

    }

    private int pcbId; 
    private GM gm;
    private CPU cpu;
    public HashMap<Integer, PCB> pcbList;
    public int procExec;
    public Memory memory;
    private Scheduler scheduler;
    
    public GP(HW hw, GM gm){
        this.cpu = hw.cpu;
        this.gm = new GM(hw.mem, 10);
        this.pcbList = new HashMap<>();
        this.procExec = 0;
        this.scheduler = new Scheduler(this, hw);
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
        return true;
    }

    public boolean desalocaProcesso(int id){
        PCB pcb = pcbList.get(id);
        if(pcb == null){
            System.out.println("Processo inexistente");
            return false; 
        }
        gm.desaloca(pcb.tabPag);
        pcbList.remove(id);
        return true;
    }

    public void executarProcesso(int id_processo){
        PCB pcb = pcbList.get(id_processo);

        if(pcb == null){
            System.out.println("Processo não existe");
            return;
        }
       // if(procExec == id_processo){
         //   System.out.println("Processo em execução");
           // return;
        //}
        procExec = id_processo;
        cpu.setContext(pcb.pc);
        cpu.run(-1);

    }

    public void executarTodosProcessos() {
       while (!pcbList.isEmpty()) {
            scheduler.schedule();
       }
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
       System.out.println();
       System.out.println("PCB ID: "+pcb.id);
       System.out.println("PCB PC: "+pcb.pc);
       System.out.println("TabPag");
       for(int i=0; i < pcb.tabPag.length; i++){
            for (int j=0; j < GM.tamPag; j++) {
                System.out.println(gm.memory.pos[GM.tradutor((i*GM.tamPag)+j, pcb.tabPag)]);
            }
       }
    }
    public void dumpM(int dumpM_start,int dumpM_end){
        int dumpSize = dumpM_end - dumpM_start;
        for(int i=0; i < dumpSize; i++){
            System.out.println(gm.memory.pos[dumpM_start+i]);
        }
    }

    public void setPcbId(int pcbId) {
        this.pcbId = pcbId;
    }

    
}