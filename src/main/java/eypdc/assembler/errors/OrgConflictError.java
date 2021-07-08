package eypdc.assembler.errors;

public class OrgConflictError extends CompileError
{
    public OrgConflictError(int errorOffset)
    {
        super("No se puede definir la direccion objetivo mas de una vez", errorOffset, 14);
    }
}
