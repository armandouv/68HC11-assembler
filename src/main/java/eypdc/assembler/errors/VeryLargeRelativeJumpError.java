package eypdc.assembler.errors;

public class VeryLargeRelativeJumpError extends CompileError
{
    public VeryLargeRelativeJumpError(int errorOffset)
    {
        super("El salto relativo especificado es muy lejano", errorOffset, 8);
    }
}
