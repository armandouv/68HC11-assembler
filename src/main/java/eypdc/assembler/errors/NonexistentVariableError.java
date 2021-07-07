package eypdc.assembler.errors;

public class NonexistentVariableError extends AssemblerError
{
    public NonexistentVariableError(int errorOffset)
    {
        super("La variable a la que se intenta hacer referencia es inexistente", errorOffset, 2);
    }
}
