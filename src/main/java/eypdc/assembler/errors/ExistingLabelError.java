package eypdc.assembler.errors;

public class ExistingLabelError extends CompileError
{
    public ExistingLabelError(int errorOffset)
    {
        super("La etiqueta especificada ya existe", errorOffset, 12);
    }
}
