package eypdc.assembler.errors;

public class EndConflictError extends CompileError
{
    public EndConflictError(int errorOffset)
    {
        super("Hay instrucciones despues de la directiva END", errorOffset, 11);
    }
}
