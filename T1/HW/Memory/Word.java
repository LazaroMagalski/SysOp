package HW.Memory;

import HW.CPU.Opcode;

public class Word { // cada posicao da memoria tem uma instrucao (ou um dado)
    public Opcode opc; //
    public int ra; // indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
    public int rb; // indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
    public int p; // parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

    public Word(Opcode _opc, int _ra, int _rb, int _p) { // vide definição da VM - colunas vermelhas da tabela
        opc = _opc;
        ra = _ra;
        rb = _rb;
        p = _p;
    }

    public Word(Word other) {
        this.opc = other.opc;
        this.p = other.p;
        this.ra = other.ra;
        this.rb = other.rb;
    }

    @Override
    public String toString() {
        if (opc == Opcode.DATA) {
            return "DATA " + p;
        } else {
            return opc.name() + ' ' + ra + ' ' + rb;
        }
    }
}