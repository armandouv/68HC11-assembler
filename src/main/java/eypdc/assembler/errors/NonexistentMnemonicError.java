package eypdc.assembler.errors;

public class NonexistentMnemonicError extends AssemblerError
{
    public NonexistentMnemonicError(int errorOffset)
    {
        super("El mnemonico al que se hace referencia no se encuentra en el set de instrucciones", errorOffset, 4);
    }
}
