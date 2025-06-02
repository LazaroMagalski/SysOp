package SW;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import HW.HW;
import HW.CPU.CPU;
import HW.Memory.Memory;
import HW.Memory.Word;
import VM.Program;

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
    public LinkedList<PCB> blockedPcbList;
    public int procExec;
    private Scheduler scheduler;
    
    public GP(HW hw, GM gm){
        this.cpu = hw.cpu;
        this.gm = gm;

        this.pcbList = new LinkedList<>();
        this.blockedPcbList = new LinkedList<>();
        this.procExec = 0;
        this.scheduler = new Scheduler(this, hw, pcbList, blockedPcbList);
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
        return true;
    }

    public boolean desalocaProcesso(int id){
        PCB pcbToRemove = null;
        // Tenta encontrar na fila de prontos
        for (PCB pcb : pcbList) {
            if (pcb.id == id) {
                pcbToRemove = pcb;
                break;
            }
        }
        // Se não encontrou na fila de prontos, tenta na fila de bloqueados
        if (pcbToRemove == null) {
            for (PCB pcb : blockedPcbList) {
                if (pcb.id == id) {
                    pcbToRemove = pcb;
                    break;
                }
            }
        }

        if(pcbToRemove == null){
            System.out.println("Processo com ID " + id + " inexistente nas filas.");
            return false;
        }

        // Desaloca a memória associada ao PCB
        if (pcbToRemove.tabPag != null && pcbToRemove.tabPag.length > 0) {
            gm.desaloca(pcbToRemove.tabPag);
            System.out.println("Memória do processo " + id + " desalocada.");
        }

        // Remove o PCB da lista apropriada
        if (pcbList.contains(pcbToRemove)) {
            pcbList.remove(pcbToRemove);
            System.out.println("Processo " + id + " removido da fila de prontos.");
        } else if (blockedPcbList.contains(pcbToRemove)) {
            blockedPcbList.remove(pcbToRemove);
            System.out.println("Processo " + id + " removido da fila de bloqueados.");
        }

        // Reinicia o procExec se o processo desalocado era o que estava "executando"
        if (procExec == id) {
            procExec = -1; // Sinaliza que não há processo em execução
        }
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
        cpu.updateMMU(pcb.tabPag);
        cpu.run(-1);

    }

    public void executarTodosProcessos() {
        while (!pcbList.isEmpty()) {
            scheduler.schedule();
            boolean running = false;
            for (var pcb : pcbList) {
                if (pcb.state == State.READY) {
                    running = true;
                    break;
                }
            }
            if (!running) {
                break;
            }
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

    public GM getGm() {
       return gm;
    }
    
    public void blockProcess(PCB pcb) {
        pcb.state = State.BLOCKED;
        pcbList.remove(pcb); // Remove da fila de prontos
        blockedPcbList.add(pcb); // Adiciona na fila de bloqueados
        System.out.println("Processo " + pcb.id + " bloqueado."); // Para debug
    }

    public void unblockProcess(PCB pcb) {
        pcb.state = State.READY;
        blockedPcbList.remove(pcb); // Remove da fila de bloqueados
        pcbList.add(pcb); // Adiciona de volta na fila de prontos
        System.out.println("Processo " + pcb.id + " desbloqueado e movido para READY."); // Para debug
    }

}