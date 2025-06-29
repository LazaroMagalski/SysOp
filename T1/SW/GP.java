package SW;

import HW.CPU.CPU;
import HW.CPU.Opcode;
import HW.HW;
import HW.Memory.Memory;
import HW.Memory.Word;
import VM.Program;
import java.util.LinkedList;

public class GP {

    public enum State {
        BLOCKED, // novo estado
        READY,   // antigo true
        RUNNING  // antigo false
    }

    public class PCB{
        public int id;
        public int[] tabPag;
        public int pc;
        public State state;
        public int[] regs;

        public PCB(){
            id = pcbId;
            pcbId++;
            tabPag = new int[0];
            pc = 0;
            state = State.READY;
            regs = new int[10];
        }

    }

    private int pcbId;
    private GM gm;
    private CPU cpu;
    public LinkedList<PCB> pcbList;
    public PCB nopPCB;
    public int procExec;
    public Memory memory;
    public Scheduler scheduler;
    
    public GP(HW hw, GM gm){
        this.cpu = hw.cpu;
        this.gm = new GM(hw.mem, 10);
        this.pcbList = new LinkedList<>();
        this.procExec = 0;
        this.scheduler = new Scheduler(this, hw, pcbList);

        int[] alocacao = gm.aloca(1);
        nopPCB = new PCB();
        nopPCB.tabPag = alocacao;
        System.out.println(nopPCB.id);
        pcbList.add(nopPCB);
        gm.carregarPrograma(new Word[]{
                new Word(Opcode.NOP, 0, 0, 0)   
            }, alocacao);
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
        pcbList.add(novoPCB);
        System.out.println(pcbList.size());
        return true;
    }

    public boolean desalocaProcesso(int id){
        PCB pcb = null;
        for (int i = 0; i < pcbList.size(); i++) {
            if (id == pcbList.get(i).id) {
                pcb = pcbList.get(i);
                break;
            }
        }
        
        if(pcb == null){
            System.out.println("Processo inexistente");
            return false; 
        }
        gm.desaloca(pcb.tabPag);
        pcbList.remove(id);
        return true;
    }

    public int getPcbId() {
        return pcbId;
    }
    public void dump(int id){
        System.out.println(pcbList.size());
        PCB pcb = null;
        for (int i = 0; i < pcbList.size(); i++) {
            if (id == pcbList.get(i).id) {
                pcb = pcbList.get(i);
                break;
            }
        }
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