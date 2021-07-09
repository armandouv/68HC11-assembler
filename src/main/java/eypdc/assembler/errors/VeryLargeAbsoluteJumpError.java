package eypdc.assembler.errors;

public class VeryLargeAbsoluteJumpError extends CompileError
{
    public VeryLargeAbsoluteJumpError(int errorOffset)
    {
        super("El salto absoluto especificado es muy lejano", errorOffset, 20);
    }
}
