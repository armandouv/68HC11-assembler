package eypdc.assembler.errors;

public class UnsupportedOperandMagnitudeError extends CompileError
{
    public UnsupportedOperandMagnitudeError(int errorOffset)
    {
        super("La magnitud proporcionada al operando no esta soportada", errorOffset, 7);
    }
}
