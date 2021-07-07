package eypdc.assembler.errors;

public class NonexistentEndDirectiveError extends CompileError
{
    public NonexistentEndDirectiveError(int errorOffset)
    {
        super("No se coloco la directiva END en el codigo fuente", errorOffset, 10);
    }
}
