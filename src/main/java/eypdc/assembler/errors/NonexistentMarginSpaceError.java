package eypdc.assembler.errors;

public class NonexistentMarginSpaceError extends CompileError
{
    public NonexistentMarginSpaceError(int errorOffset)
    {
        super("La instruccion carece de al menos un espacio relativo al margen", errorOffset, 9);
    }
}
