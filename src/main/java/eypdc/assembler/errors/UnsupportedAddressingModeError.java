package eypdc.assembler.errors;

public class UnsupportedAddressingModeError extends CompileError
{
    public UnsupportedAddressingModeError(int errorOffset)
    {
        super("La instruccion no soporta el modo de direccionamiento indicado", errorOffset, 18);
    }
}
