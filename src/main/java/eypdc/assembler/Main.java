package eypdc.assembler;

public class Main
{
    public static void main(String[] args)
    {
        if (args.length != 1) throw new IllegalArgumentException("Usage: program <filepath>");
        Assembler.compile(args[0]);
    }
}
