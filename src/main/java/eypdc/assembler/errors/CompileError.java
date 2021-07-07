package eypdc.assembler.errors;

import java.text.ParseException;

public class CompileError extends ParseException
{
    public CompileError(String message, int errorOffset, int errorCode)
    {
        super("Error " + errorCode + ": " + message, errorOffset);
    }
}
