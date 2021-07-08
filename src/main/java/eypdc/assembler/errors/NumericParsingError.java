package eypdc.assembler.errors;

public class NumericParsingError extends CompileError
{
    public NumericParsingError(int errorOffset)
    {
        super("No se pudo traducir la literal numerica especificada", errorOffset, 15);
    }
}
