package T1;

import java.util.Arrays;
import T1.Sistema;

public class Testes {

    public static void main(String[] args) {
        Testes t = new Testes();
        t.testaSucessoAlocaGM();



    }   
    public void testaSucessoAlocaGM(){
        Sistema sistema = new Sistema(1024);
        Sistema.GM gm = sistema.new GM();
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
