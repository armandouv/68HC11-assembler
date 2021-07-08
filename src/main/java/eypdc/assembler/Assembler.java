package eypdc.assembler;

import eypdc.assembler.errors.*;
import lombok.Data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Assembler
{
    private final InstructionSet instructionSet;

    private Assembler()
    {
        this.instructionSet = InstructionSet.parseFromJson();
    }

    public static void compile(String sourcePath)
    {
        Assembler assembler = new Assembler();
        List<String> lines;

        try (BufferedReader inputStream = new BufferedReader(new FileReader(sourcePath)))
        {
            lines = inputStream.lines().collect(Collectors.toList());
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException("The specified file does not exist", e);
        }
        catch (IOException e)
        {
            throw new RuntimeException("An error occurred while reading the input file", e);
        }

        List<CompiledLine> compiledLines = new ArrayList<>();

        try
        {
            Map<String, Integer> labels = assembler.firstPass(lines, compiledLines);
            assembler.secondPass(compiledLines, labels);
        }
        catch (CompileError compileError)
        {
            compileError.printStackTrace();
        }

        // TODO: Write formatted output to *.ASC and *.LST files.

    }

    private void secondPass(List<CompiledLine> outputLines, Map<String, Integer> labels) throws CompileError
    {

    }

    // Should LST contain all the source code even though some lines might not be mapped to an instruction?
    // What's the difference between constants and variables?

    private Map<String, Integer> firstPass(List<String> inputLines, List<CompiledLine> outputLines) throws CompileError
    {
        Map<String, Integer> labels = new HashMap<>();
        Map<String, Integer> constantsAndVariables = new HashMap<>();
        Integer targetAddress = null;
        boolean hasEndDirective = false;

        for (int i = 0; i < inputLines.size(); i++)
        {
            String line = inputLines.get(i);
            if (line.isBlank())
            {
                outputLines.add(new CompiledLine());
                continue;
            }
            if (hasEndDirective) throw new EndConflictError(i);
            CompiledLine compiledLine = new CompiledLine();

            String[] splitLine = line.strip().split(" +");
            if (line.charAt(0) != ' ')
            {
                if (splitLine.length != 1) throw new NonexistentMarginSpaceError(i);

                String label = splitLine[0];
                if (labels.containsKey(label)) throw new ExistingLabelError(i);
                labels.put(label, i);
                outputLines.add(compiledLine);
                continue;
            }

            if (splitLine[0].equalsIgnoreCase("ORG"))
            {
                if (splitLine.length > 2) throw new UnnecessaryOperandError(i);
                if (splitLine.length == 1) throw new MissingOperandsError(i);
                if (targetAddress != null) throw new OrgConflictError(i);
                targetAddress = parseNumericLiteral(splitLine[1], i);
                outputLines.add(compiledLine);
                continue;
            }

            if (splitLine[0].equalsIgnoreCase("END"))
            {
                if (splitLine.length != 1) throw new UnnecessaryOperandError(i);
                hasEndDirective = true;
                outputLines.add(compiledLine);
                continue;
            }

            if (splitLine.length > 1 && splitLine[1].equalsIgnoreCase("EQU"))
            {
                if (splitLine.length == 2) throw new MissingOperandsError(i);
                if (splitLine.length > 3) throw new UnnecessaryOperandError(i);
                constantsAndVariables.put(splitLine[0], parseNumericLiteral(splitLine[2], i));
                outputLines.add(compiledLine);
                continue;
            }

            if (targetAddress == null) throw new NonexistentOrgDirective(i);

            if (splitLine[0].equalsIgnoreCase("FCB"))
            {
                if (splitLine.length > 2) throw new UnnecessaryOperandError(i);
                if (splitLine.length == 1) throw new MissingOperandsError(i);
                int formedByte = parseNumericLiteral(splitLine[1], i);
                if (formedByte > 0xFF) throw new UnsupportedOperandMagnitudeError(i);
                compiledLine.setAddress(targetAddress);
                compiledLine.setOpcode(formedByte);
                compiledLine.setSizeInBytes(1);
                outputLines.add(compiledLine);
                targetAddress += compiledLine.getSizeInBytes();
                continue;
            }

            compiledLine = compileLine(splitLine, i, constantsAndVariables);
            compiledLine.setAddress(targetAddress);
            targetAddress += compiledLine.getSizeInBytes();
            outputLines.add(compiledLine);
        }

        return labels;
    }

    private int parseNumericLiteral(String operand, int lineNumber) throws NumericParsingError
    {
        if (operand.isBlank()) throw new NumericParsingError(lineNumber);

        int result;
        char firstChar = operand.charAt(0);
        if (firstChar == '$') result = Integer.parseUnsignedInt(operand.substring(1), 16);
        else if (firstChar == '%') result = Integer.parseUnsignedInt(operand.substring(1), 2);
        else result = Integer.parseUnsignedInt(operand, 10);

        return result;
    }


    private CompiledLine compileLine(String[] target, int lineNumber, Map<String, Integer> constantsAndVariables)
            throws NonexistentMnemonicError
    {
        String mnemonic = target[0].toUpperCase();
        if (!instructionSet.containsMnemonic(mnemonic)) throw new NonexistentMnemonicError(lineNumber);

        CompiledLine compiledLine = new CompiledLine();

        return compiledLine;
    }

    @Data
    private static class CompiledLine
    {
        private Integer address;
        private Integer opcode;
        private List<Integer> operands = new ArrayList<>();
        private int sizeInBytes = 0;

        public boolean isEmpty()
        {
            return (address == null && opcode == null && operands.isEmpty());
        }
    }
}
