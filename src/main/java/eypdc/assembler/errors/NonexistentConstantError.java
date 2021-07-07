package eypdc.assembler.errors;

public class NonexistentConstantError extends AssemblerError
{

    public NonexistentConstantError(int errorOffset)
    {
        super("La constante a la que se intenta hacer referencia es inexistente", errorOffset, 1);
    }
}
