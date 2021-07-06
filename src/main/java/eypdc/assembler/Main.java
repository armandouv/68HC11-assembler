package eypdc.assembler;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        InstructionSet instructionSet = InstructionSet.parseFromJson("instruction_set.json");
    }
}
