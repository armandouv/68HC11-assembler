package eypdc.assembler.errors;

public class UnnecessaryOperandError extends AssemblerError
{
    public UnnecessaryOperandError(int errorOffset)
    {
        super("Se proporciono uno o mas operandos adicionales a la instruccion", errorOffset, 6);
    }
}
