package eypdc.assembler.errors;

public class NonexistentLabelError extends AssemblerError
{
    public NonexistentLabelError(int errorOffset)
    {
        super("La etiqueta a la que se intenta hacer referencia es inexistente", errorOffset, 3);
    }
}
