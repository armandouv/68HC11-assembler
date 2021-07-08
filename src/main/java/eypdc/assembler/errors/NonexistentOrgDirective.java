package eypdc.assembler.errors;

public class NonexistentOrgDirective extends CompileError
{
    public NonexistentOrgDirective(int errorOffset)
    {
        super("La direccion de inicio no fue especificada con la directiva ORG", errorOffset, 13);
    }
}
