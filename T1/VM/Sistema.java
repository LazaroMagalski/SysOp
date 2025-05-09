package VM;

import java.util.*;

import HW.HW;
import SW.GM;
import SW.GP;
import SW.GP.PCB;
import SW.SO;

public class Sistema {
    public HW hw;
	public SO so;
	public Programs progs;
	public GM gm;
	public GP gp;

	public Sistema(int tamMem) {
		hw = new HW(tamMem);           // memoria do HW tem tamMem palavras
		so = new SO(hw);
		hw.cpu.setUtilities(so.utils); // permite cpu fazer dump de memoria ao avancar
		progs = new Programs();
		gm =  new GM(hw.mem, tamMem);
		gp = new GP(hw, gm);
	}
	public void menu(){
		Scanner sc = new Scanner(System.in);
		while(true){
			System.out.println("Digite um comando:");
			String command = sc.nextLine();
			switch (command) {
				case "new":
					System.out.println("Digite o nome do programa: ");
					String name = sc.nextLine();
					if (name.compareTo("teste") == 0) {
						gp.criaProcesso(progs.retrieveProg("sum"));
						gp.criaProcesso(progs.retrieveProg("fatorial"));
						break;
					} else if (name.compareTo("teste2") == 0) {
						gp.criaProcesso(progs.retrieveProg("fatorial"));
						gp.criaProcesso(progs.retrieveProg("fatorialV2"));
						gp.desalocaProcesso(0);
						gp.criaProcesso(progs.retrieveProg("fibonacci10v2"));
					} else {
						gp.criaProcesso(progs.retrieveProg(name));
					}
					break;
				case "rm":
					System.out.println("Digite o id do programa: ");
					int rm_id = sc.nextInt();
					if(gp.desalocaProcesso(rm_id) == true)
						System.out.println("Processo Removido");
					break;
				case "ps":
					System.out.println(Arrays.toString(gp.pcbList.stream().mapToInt(p -> p.id).toArray()));
					break;
				case "dump":
					System.out.println("Digite o id do programa: ");
					int dump_id = sc.nextInt();
					gp.dump(dump_id);
					break;
				case "dumpM":
					System.out.println("Digite o inicio: ");
					int dumpM_start = sc.nextInt();
					System.out.println("Digite o final: ");
					int dumpM_end = sc.nextInt();
					gp.dumpM(dumpM_start, dumpM_end);
					break;
				case "dumpTabPag":
					System.out.println("Digite o id do programa: ");
					int id = sc.nextInt();
					var pcbList = gp.pcbList;
					for (var p : pcbList) {
						if (p.id == id) {
							for (int e : p.tabPag) {
								System.out.printf("%d ", e);
							}
							System.out.println();
						}
					}
					break;
				case "exec":
					System.out.println("Digite o id do programa: ");
					int exec_id = sc.nextInt();
					gp.executarProcesso(exec_id);
					break;
				case "traceOn":
					hw.cpu.setDebug(true);
					break;
				case "traceOff":
					hw.cpu.setDebug(false);
					break;
				case "execAll":
					System.out.println();
					gp.executarTodosProcessos();
					break;
				case "exit":
					sc.close();
					System.exit(0);
					break;
				default:
					break;
			}
		}
	}

	public void run() {//remover

		so.utils.loadAndExec(progs.retrieveProgram("sum"));
	}
	
	public static void main(String args[]) {
		Sistema s = new Sistema(1024);
		s.menu();
		//s.run();
	}
}
