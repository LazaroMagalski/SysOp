import java.util.Arrays;

import HW.Memory.Memory;
import SW.GM;

public class Testes {

    public static void main(String[] args) {
        Testes t = new Testes();
        t.testaSucessoAlocaGM();



    }   
    
    public void testaSucessoAlocaGM(){
        Memory memory = new Memory(1024);
        GM gm = new GM(memory, 1024, 64);
        int[] paginas = new int[100];
        for (int i = 0; i < paginas.length; i++) {
            paginas[i] = -1;
        }
        System.out.println(Arrays.toString(paginas));
        gm.aloca(200, paginas);
        System.out.println(Arrays.toString(paginas));
        gm.desaloca(paginas);
        System.out.println(Arrays.toString(paginas));
    }
}