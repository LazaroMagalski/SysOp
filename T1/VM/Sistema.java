package VM;

import HW.CPU.Opcode;
import HW.Console;
import HW.HW;
import HW.Memory.Word;
import SW.GM;
import SW.SO;
import SW.Timer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Sistema {
	public HW hw;
	public SO so;
	public Programs progs;

	public Sistema(int tamMem) {
		hw = new HW(tamMem); // memoria do HW tem tamMem palavras
		so = new SO(hw);
		hw.cpu.setUtilities(so.utils); // permite cpu fazer dump de memoria ao avancar
		progs = new Programs();
		wantsRead = new AtomicBoolean(false);
		result = new AtomicInteger(-1);
	}

	public void menu() {
		Scanner sc;
		while (true) {
			while (hw.cpu.procId.get() != 0)
				;
			sc = new Scanner(System.in);
			System.out.println("Digite um comando:");
			String command;
			try {
				command = sc.nextLine();
			} catch (NoSuchElementException e) {
				continue;
			}

			switch (command) {
				case "new":
					System.out.println("Digite o nome do programa: ");
					String name = sc.nextLine();
					if (name.compareTo("teste") == 0) {
						so.gp.criaProcesso(progs.retrieveProg("sum"));
						so.gp.criaProcesso(progs.retrieveProg("fatorial"));
						so.gp.criaProcesso(progs.retrieveProg("fatorial"));
						so.gp.criaProcesso(progs.retrieveProg("fatorial"));
						so.gp.criaProcesso(progs.retrieveProg("fatorial"));
						break;
					} else if (name.compareTo("teste2") == 0) {
						so.gp.criaProcesso(progs.retrieveProg("fatorial"));
						so.gp.criaProcesso(progs.retrieveProg("fatorialV2"));
						so.gp.desalocaProcesso(0);
						so.gp.criaProcesso(progs.retrieveProg("fibonacci10v2"));
					} else {
						so.gp.criaProcesso(progs.retrieveProg(name));
					}
					break;
				case "rm":
					System.out.println("Digite o id do programa: ");
					int rm_id = sc.nextInt();
					if (so.gp.desalocaProcesso(rm_id) == true)
						System.out.println("Processo Removido");
					break;
				case "ps":
					System.out.println(so.gp.pcbList.size());
					for (int i = 0; i < so.gp.pcbList.size(); i++) {
						System.out.println(so.gp.pcbList.get(i).id);
					}
					break;
				case "dump":
					System.out.println("Digite o id do programa: ");
					int dump_id = sc.nextInt();
					so.gp.dump(dump_id);
					break;
				case "dumpM":
					System.out.println("Digite o inicio: ");
					int dumpM_start = sc.nextInt();
					System.out.println("Digite o final: ");
					int dumpM_end = sc.nextInt();
					so.gp.dumpM(dumpM_start, dumpM_end);
					break;
				case "dumpTabPag":
					System.out.println("Digite o id do programa: ");
					int id = sc.nextInt();
					var pcbList = so.gp.pcbList;
					for (var p : pcbList) {
						if (p.id == id) {
							for (int e : p.tabPag) {
								System.out.printf("%d ", e);
							}
							System.out.println();
						}
					}
					break;
				case "traceOn":
					hw.cpu.setDebug(true);
					break;
				case "traceOff":
					hw.cpu.setDebug(false);
					break;
				case "exit":
					sc.close();
					System.exit(0);
					break;
				case "IN":
					wantsRead.set(true);
					result.set(sc.nextInt());
					wantsRead.set(false);

					int[] tabPag = null;
					int procId = hw.cpu.procId.get();
					
					for (int i = 0; i < so.gp.pcbList.size(); i++) {
						if (procId == so.gp.pcbList.get(i).id) {
							tabPag = so.gp.pcbList.get(i).tabPag;
							break;
						}
					}

					int dataLogAddr = 0;
					int dataPhys = GM.tradutor(dataLogAddr, tabPag);
					so.gm.memory.pos[dataPhys] = new Word(Opcode.STOP, -1, -1, -1);

					int phys = GM.tradutor(hw.cpu.reg[5], tabPag);
					so.gm.memory.pos[phys].opc = Opcode.DATA;
					so.gm.memory.pos[phys].p = result.get();

					so.gp.swapOutProcess(procId);

					break;
				default:
					break;
			}
		}
	}

	public void run() {// remover

		so.utils.loadAndExec(progs.retrieveProgram("sum"));
	}

	AtomicInteger result;
	AtomicBoolean wantsRead;

	public static void main(String args[]) {
		Sistema s = new Sistema(1024);
		s.hw.cpu.updateMMU(s.so.gp.nopPCB.tabPag);
		Thread th = new Thread(s.hw.cpu);
		s.hw.cpu.setDebug(false);
		Console c = new Console(s.hw.cpu.requests, s.so.gm, s.hw.cpu, s.wantsRead, s.result, s.so.gp);
		Thread cth = new Thread(c);
		Timer timer = new Timer(s.hw.cpu);
		Thread tth = new Thread(timer);
		th.start();
		cth.start();
		tth.start();
		s.menu();
		// s.run();
	}
}
