package VM;

import java.util.*;

import HW.HW;
import SW.GM;
import SW.GP;
import SW.GP.PCB;
import SW.SO;
import SW.Scheduler;

public class Sistema {
    public HW hw;
	public SO so;
	public Programs progs;
	public GM gm;
	public GP gp;
	private Thread cpuThread;
	private Thread shellThread;

	public Sistema(int tamMem) {
		hw = new HW(tamMem);
		gm =  new GM(hw.mem, 10); // Mantendo tamPag = 10 como discutido
		gp = new GP(hw, gm); // Instancia GP antes de SO
		so = new SO(hw, gp); // FEAT: Passa o GP para o SO
		hw.cpu.setUtilities(so.utils);
		
		// Inicializa a Thread da CPU
        // A CPU.run() espera uma fatia de tempo. A CPU Thread sempre roda "aguardando"
        // que o Scheduler a acorde para processar o próximo processo.
        cpuThread = new Thread(() -> { // Cria a thread da CPU
            while (true) { // Loop eterno para a CPU
                // A CPU aguarda ser notificada para executar uma fatia de tempo
                hw.cpu.run(2); // Passa a fatia de tempo (2 instruções)
            }
        }, "CPU-Thread"); // Nome da thread
        cpuThread.setDaemon(true); // Define como daemon para que não impeça o programa de terminar

        // Inicializa a Thread do Shell
        shellThread = new Thread(() -> menu(), "Shell-Thread"); // Cria a thread do Shell
        shellThread.setDaemon(true); // Define como daemon

		progs = new Programs();
	}

	public void start() {
        cpuThread.start(); // Inicia a thread da CPU
        shellThread.start(); // Inicia a thread do Shell

        // O loop principal do SO (Scheduler) agora roda em uma thread separada ou na main thread,
        // mas o Scheduler chamará o GP para gerenciar a execução da CPU.
        // Se o Scheduler não tiver mais processos, ele vai esperar.
        // O loop principal do Scheduler também será eterno.
        // Vamos rodar o scheduler em um loop eterno aqui na main thread por enquanto.
        // Posteriormente, pode ser uma thread dedicada para o Scheduler.
        while (true) {
            // Se houver processos prontos para executar, o Scheduler os escalona.
            // Se não houver, o Scheduler vai esperar (via wait() no monitor do GP).
            // O Scheduler vai tentar agendar um processo. Se não houver nenhum, ele esperará.
            if (!gp.pcbList.isEmpty() || !gp.blockedPcbList.isEmpty()) {
                gp.getScheduler().schedule(); // Chama o escalonador para gerenciar a fila de prontos
            } else {
                // Se não há processos, o Scheduler principal (esta thread) pode esperar.
                // Isso evita que o loop gire infinitamente sem trabalho.
                // O Shell criará novos processos e notificará o Scheduler.
                // No futuro, quando o Shell criar um processo, ele deveria notificar o schedulerMonitor.
                synchronized (gp.schedulerMonitor) { // Sincroniza no monitor do Scheduler
                    try {
                        System.out.println("Sistema: Sem processos ativos para escalar. Aguardando...");
                        gp.schedulerMonitor.wait(); // Aguarda novos processos ou eventos de I/O
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Sistema thread interrupted: " + e.getMessage());
                    }
                }
            }

            // Pequeno atraso para não consumir CPU em loop muito apertado
            try {
                Thread.sleep(100); // 100ms de "tempo de ócio" para a main thread
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    public void menu(){ // Mantenha o método menu como estava, mas agora ele roda em uma thread
        Scanner sc = new Scanner(System.in);
        while(true){
            System.out.println("\nDigite um comando (new, rm, ps, dump, dumpM, dumpTabPag, exec, traceOn, traceOff, execAll, exit):");
            String command = sc.nextLine();
            switch (command.toLowerCase()) { // Usar toLowerCase para flexibilidade
                case "new":
                    System.out.println("Digite o nome do programa: ");
                    String name = sc.nextLine();
                    Program selectedProgram = progs.retrieveProg(name);
                    if (selectedProgram != null) {
                        if (gp.criaProcesso(selectedProgram)) {
                            System.out.println("Processo para '" + name + "' criado com sucesso!");
                            // NOVO: Notificar o Scheduler que um novo processo foi criado
                            synchronized (gp.schedulerMonitor) { // Sincroniza no monitor do Scheduler
                                gp.schedulerMonitor.notify(); // Acorda o Scheduler se ele estiver esperando
                            }
                        } else {
                            System.out.println("Erro: Não foi possível criar o processo para '" + name + "'. Memória insuficiente ou programa não encontrado.");
                        }
                    } else {
                        System.out.println("Programa '" + name + "' não encontrado.");
                    }
                    break;
                case "rm":
                    System.out.println("Digite o id do programa: ");
                    if (sc.hasNextInt()) {
                        int rm_id = sc.nextInt();
                        sc.nextLine(); // Consumir a nova linha
                        if(gp.desalocaProcesso(rm_id)) {
                            System.out.println("Processo " + rm_id + " Removido.");
                            // NOVO: Notificar o Scheduler se algum processo parou ou foi removido
                            synchronized (gp.schedulerMonitor) {
                                gp.schedulerMonitor.notify();
                            }
                        } else {
                            System.out.println("Falha ao remover processo " + rm_id + ".");
                        }
                    } else {
                        System.out.println("Entrada inválida. Por favor, digite um número inteiro.");
                        sc.nextLine(); // Consumir a entrada inválida
                    }
                    break;
                case "ps":
                    if (gp.pcbList.isEmpty() && gp.blockedPcbList.isEmpty()) {
                        System.out.println("Nenhum processo ativo.");
                    } else {
                        System.out.println("Processos READY (ID): " + Arrays.toString(gp.pcbList.stream().mapToInt(p -> p.id).toArray()));
                        System.out.println("Processos BLOCKED (ID): " + Arrays.toString(gp.blockedPcbList.stream().mapToInt(p -> p.id).toArray()));
                    }
                    break;
                case "dump":
                    System.out.println("Digite o id do programa: ");
                    if (sc.hasNextInt()) {
                        int dump_id = sc.nextInt();
                        sc.nextLine(); // Consumir a nova linha
                        gp.dump(dump_id);
                    } else {
                        System.out.println("Entrada inválida. Por favor, digite um número inteiro.");
                        sc.nextLine(); // Consumir a entrada inválida
                    }
                    break;
                case "dumpm": // Mudado para dumpm para consistência
                    System.out.println("Digite o inicio: ");
                    if (sc.hasNextInt()) {
                        int dumpM_start = sc.nextInt();
                        System.out.println("Digite o final: ");
                        int dumpM_end = sc.nextInt();
                        sc.nextLine(); // Consumir a nova linha
                        gp.dumpM(dumpM_start, dumpM_end);
                    } else {
                        System.out.println("Entrada inválida. Por favor, digite números inteiros.");
                        sc.nextLine(); // Consumir a entrada inválida
                    }
                    break;
                case "dumptabpag": // Mudado para dumptabpag
                    System.out.println("Digite o id do programa: ");
                    if (sc.hasNextInt()) {
                        int id = sc.nextInt();
                        sc.nextLine(); // Consumir a nova linha
                        var pcbList = gp.pcbList;
                        // Procura em ambas as listas
                        PCB pcbFound = null;
                        for (var p : pcbList) { if (p.id == id) { pcbFound = p; break; }}
                        if (pcbFound == null) { for (var p : gp.blockedPcbList) { if (p.id == id) { pcbFound = p; break; }}}

                        if (pcbFound != null) {
                            System.out.print("Tabela de Páginas do PCB " + id + ": ");
                            for (int e : pcbFound.tabPag) {
                                System.out.printf("%d ", e);
                            }
                            System.out.println();
                        } else {
                            System.out.println("Processo com ID " + id + " não encontrado.");
                        }
                    } else {
                        System.out.println("Entrada inválida. Por favor, digite um número inteiro.");
                        sc.nextLine();
                    }
                    break;
                case "exec": // Executar um processo específico (ainda sincrono, mas agora escalona)
                    System.out.println("Digite o id do programa: ");
                    if (sc.hasNextInt()) {
                        int exec_id = sc.nextInt();
                        sc.nextLine(); // Consumir a nova linha
                        // Encontrar o PCB e colocá-lo na fila de prontos se não estiver lá
                        PCB pcbToExec = null;
                        for (PCB pcb : gp.pcbList) { if (pcb.id == exec_id) { pcbToExec = pcb; break; }}
                        if (pcbToExec == null) { for (PCB pcb : gp.blockedPcbList) { if (pcb.id == exec_id) { pcbToExec = pcb; break; }}}

                        if (pcbToExec != null) {
                            if (pcbToExec.state != SW.GP.State.READY) {
                                System.out.println("Processo " + exec_id + " não está no estado READY. Estado atual: " + pcbToExec.state);
                                // Tentar movê-lo para ready se estiver bloqueado? (Implementação futura)
                            }
                            // Mover para o início da fila de prontos se necessário, ou apenas deixar o scheduler pegar
                            // gp.pcbList.remove(pcbToExec);
                            // gp.pcbList.addFirst(pcbToExec); // Pode ser implementado se houver prioridade de execução
                            System.out.println("Solicitando execução para o processo " + exec_id + "...");
                            // Notificar o scheduler para que ele acorde e pegue o processo
                            synchronized (gp.schedulerMonitor) {
                                gp.schedulerMonitor.notify();
                            }
                        } else {
                            System.out.println("Processo com ID " + exec_id + " não encontrado.");
                        }
                    } else {
                        System.out.println("Entrada inválida. Por favor, digite um número inteiro.");
                        sc.nextLine();
                    }
                    break;
                case "traceon": // Mudado para traceon
                    hw.cpu.setDebug(true);
                    System.out.println("CPU Debug: ON");
                    break;
                case "traceoff": // Mudado para traceoff
                    hw.cpu.setDebug(false);
                    System.out.println("CPU Debug: OFF");
                    break;
                case "execall": // Mudado para execall
                    System.out.println("Iniciando execução de todos os processos agendados...");
                    // Agora o scheduler será acionado pelo loop principal do Sistema
                    // Podemos notificar o scheduler para ter certeza que ele está ativo.
                    synchronized (gp.schedulerMonitor) {
                        gp.schedulerMonitor.notifyAll(); // Acorda todos que esperam no schedulerMonitor
                    }
                    // Este comando agora não bloqueia o shell, pois a execução é concorrente.
                    break;
                case "exit":
                    sc.close();
                    System.out.println("Encerrando o sistema...");
                    System.exit(0); // Força a saída de todas as threads
                    break;
                default:
                    System.out.println("Comando desconhecido.");
                    break;
            }
        }
    }

    public static void main(String args[]) {
        Sistema s = new Sistema(1024);
        s.start(); // Inicia o sistema, que agora gerencia as threads
    }
}
