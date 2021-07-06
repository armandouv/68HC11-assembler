package eypdc.assembler;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) throw new IllegalArgumentException("Usage: program <filepath>");
        InstructionSet instructionSet = InstructionSet.parseFromJson("instruction_set.json");
        Assembler.compile(instructionSet, args[1]);
    }
}
