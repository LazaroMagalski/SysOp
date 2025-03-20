package VM;

import HW.HW;
import SW.SO;

public class Sistema {
    public HW hw;
	public SO so;
	public Programs progs;

	public Sistema(int tamMem) {
		hw = new HW(tamMem);           // memoria do HW tem tamMem palavras
		so = new SO(hw);
		hw.cpu.setUtilities(so.utils); // permite cpu fazer dump de memoria ao avancar
		progs = new Programs();
	}

	public void run() {

		so.utils.loadAndExec(progs.retrieveProgram("sum"));//<========================================================================================

		// so.utils.loadAndExec(progs.retrieveProgram("fatorial"));
		// fibonacci10,
		// fibonacci10v2,
		// progMinimo,
		// fatorialWRITE, // saida
		// fibonacciREAD, // entrada
		// PB
		// PC, // bubble sort
	}
	// ------------------- S I S T E M A - fim
	// --------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// ------------------- instancia e testa sistema
	public static void main(String args[]) {
		Sistema s = new Sistema(1024);
		s.run();
	}
}
