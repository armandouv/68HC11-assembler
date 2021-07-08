package eypdc.assembler.errors;

public class NonexistentConstantOrVariableError extends CompileError
{
    public NonexistentConstantOrVariableError(int errorOffset)
    {
        super("La variable o constante especificada no existe", errorOffset, 19);
    }
}
