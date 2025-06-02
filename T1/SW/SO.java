package SW;

import HW.HW;

public class SO {
	public InterruptHandling ih;
	public SysCallHandling sc;
	public Utilities utils;
    public GP gp; // Adicionar referência ao GP no SO para passar para SysCallHandling

	public SO(HW hw, GP gp) { // Construtor do SO agora recebe o GP
		this.gp = gp; // Inicializa o GP no SO
		ih = new InterruptHandling(hw, gp);
		sc = new SysCallHandling(hw, gp); // ATUALIZAÇÃO: Passa o GP para SysCallHandling
		hw.cpu.setAddressOfHandlers(ih, sc);
		utils = new Utilities(hw);
	}
}