package eypdc.assembler.errors;

public class MissingOperandsError extends CompileError
{
    public MissingOperandsError(int errorOffset)
    {
        super("No se proporciono la suficiente cantidad de operandos", errorOffset, 5);
    }
}
