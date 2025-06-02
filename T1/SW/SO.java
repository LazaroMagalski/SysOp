package SW;


import HW.HW;

public class SO {
	public InterruptHandling ih;
	public SysCallHandling sc;
	public Utilities utils;
	public GM gm;
	public GP gp;


	public SO(HW hw) {
		ih = new InterruptHandling(hw, this); // rotinas de tratamento de int
		sc = new SysCallHandling(hw); // chamadas de sistema
		hw.cpu.setAddressOfHandlers(ih, sc);
		utils = new Utilities(hw);
		gm = new GM(hw.mem, 1024);
		gp = new GP(hw, gm);
	}


}
