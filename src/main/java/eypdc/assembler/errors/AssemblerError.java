package eypdc.assembler.errors;

import java.text.ParseException;

public class AssemblerError extends ParseException
{
    public AssemblerError(String message, int errorOffset, int errorCode)
    {
        super("Error " + errorCode + ": " + message, errorOffset);
    }
}
