package eypdc.assembler.errors;

public class BadFormatError extends CompileError
{
    public BadFormatError(int errorOffset)
    {
        super("El formato especificado es incorrecto", errorOffset, 19);
    }
}
