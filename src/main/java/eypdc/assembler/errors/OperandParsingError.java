package eypdc.assembler.errors;

public class OperandParsingError extends CompileError
{
    public OperandParsingError(int errorOffset)
    {
        super("Error al tratar de analizar el operando", errorOffset, 17);
    }
}
