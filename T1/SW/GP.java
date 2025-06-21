package SW;

import HW.CPU.CPU;
import HW.CPU.Opcode;
import HW.HW;
import HW.Memory.Disco;
import HW.Memory.Memory;
import HW.Memory.Word;
import VM.Program;
import java.util.LinkedList;

public class GP {

    public enum State {
        BLOCKED, // novo estado
        READY, // antigo true
        RUNNING // antigo false
    }

    public class PCB {
        public int id;
        public int[] tabPag;
        public int pc;
        public State state;
        public int[] regs;

        public PCB() {
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
    public Disco disco;

    public GP(HW hw, GM gm, Disco disco) {
        this.cpu = hw.cpu;
        this.gm = gm;
        this.pcbList = new LinkedList<>();
        this.procExec = 0;
        this.scheduler = new Scheduler(this, hw, pcbList);
        this.disco = disco;

        int[] alocacao = gm.aloca(1);
        nopPCB = new PCB();
        nopPCB.tabPag = alocacao;
        System.out.println(nopPCB.id);
        pcbList.add(nopPCB);
        gm.carregarPrograma(new Word[] {
                new Word(Opcode.NOP, 0, 0, 0)
        }, alocacao);
    }

    public boolean criaProcesso(Program program) {

        try {
        int tamPag = GM.tamPag;
        int numPaginas = (int) Math.ceil((double) program.image.length / tamPag);

        // Aloca só UMA página na RAM para o processo
        int[] alocacao = gm.aloca(tamPag);
        if (program.image.length > gm.tamMem)
            return false;
        if (alocacao == null) {
            System.out.println("Erro: Memória insuficiente para alocar o programa");
            return false;
        }
        PCB novoPCB = new PCB();
        novoPCB.tabPag = new int[numPaginas];
        // Inicializa todas as páginas como -1 (não carregadas)
        for (int i = 0; i < numPaginas; i++) {
            novoPCB.tabPag[i] = -1;
        }
        // Carrega a primeira página na RAM
        novoPCB.tabPag[0] = alocacao[0];
        Word[] primeiraPagina = new Word[tamPag];
        for (int i = 0; i < tamPag; i++) {
            int idx = i;
            if (idx < program.image.length)
                primeiraPagina[i] = program.image[idx];
            else
                primeiraPagina[i] = new Word(Opcode.___, -1, -1, -1);
        }
        gm.carregarPrograma(primeiraPagina, new int[]{alocacao[0]});

        // Salva as demais páginas no disco
        for (int pag = 1; pag < numPaginas; pag++) {
            Word[] pagina = new Word[tamPag];
            for (int i = 0; i < tamPag; i++) {
                int idx = pag * tamPag + i;
                if (idx < program.image.length)
                    pagina[i] = program.image[idx];
                else
                    pagina[i] = new Word(Opcode.___, -1, -1, -1);
            }
            disco.salvarPagina(pag, pagina);
        }

        pcbList.add(novoPCB);
        System.out.println(pcbList.size());
    } catch (NullPointerException e) {
        System.out.println("Nome de programa inválido");
    }
    return true;
    }

    public boolean desalocaProcesso(int id) {
        PCB pcb = null;
        for (int i = 0; i < pcbList.size(); i++) {
            if (id == pcbList.get(i).id) {
                pcb = pcbList.get(i);
                break;
            }
        }

        if (pcb == null) {
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

    public void dump(int id) {
    System.out.println(pcbList.size());
    PCB pcb = null;
    for (int i = 0; i < pcbList.size(); i++) {
        if (id == pcbList.get(i).id) {
            pcb = pcbList.get(i);
            break;
        }
    }
    if (pcb == null) {
        System.out.println("Processo invalido");
        return;
    }
    System.out.println();
    System.out.println("PCB ID: " + pcb.id);
    System.out.println("PCB PC: " + pcb.pc);
    System.out.println("TabPag");
    for (int i = 0; i < pcb.tabPag.length; i++) {
        for (int j = 0; j < GM.tamPag; j++) {
            int ef = GM.tradutor((i * GM.tamPag) + j, pcb.tabPag);
            if (ef >= 0) {
                System.out.println(gm.memory.pos[ef]);
            } else {
                System.out.println("Página " + i + " não está carregada na RAM.");
            }
        }
    }
}

    public void dumpM(int dumpM_start, int dumpM_end) {
        int dumpSize = dumpM_end - dumpM_start;
        for (int i = 0; i < dumpSize; i++) {
            System.out.println(gm.memory.pos[dumpM_start + i]);
        }
    }

    public void setPcbId(int pcbId) {
        this.pcbId = pcbId;
    }
}