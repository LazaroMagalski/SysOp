package VM;

import java.util.Scanner;

import HW.HW;
import SW.GM;
import SW.GP;
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
		gp = new GP(hw.cpu, gm);
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
					switch (name) {
						case "sum":
							gp.criaProcesso(progs.retrieveProg("sum"));
							break;
						default:
							System.out.println("Nome invalido");
							break;
					}
					break;
				case "rm":
					System.out.println("Digite o id do programa: ");
					String rm_id = sc.nextLine();
					break;
				case "ps":
					break;
				case "dump"://+id
					System.out.println("Digite o id do programa: ");
					String dump_id = sc.nextLine();
					break;
				case "dumpM":
					System.out.println("Digite o inicio: ");
					String dumpM_start = sc.nextLine();
					System.out.println("Digite o final: ");
					String dumpM_end = sc.nextLine();
					break;
				case "exec":
					System.out.println("Digite o id do programa: ");
					String exec_id = sc.nextLine();
					break;
				case "traceOn":
					break;
				case "traceOff":
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
