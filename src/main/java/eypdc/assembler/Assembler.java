package eypdc.assembler;

import eypdc.assembler.errors.*;
import lombok.Data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
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
            int commentIndex = line.indexOf('*');
            if (commentIndex != -1) line = line.substring(0, commentIndex);

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

    private int parseNumericLiteral(String operand, int lineNumber) throws NumericParsingError, NumberFormatException
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
            throws CompileError
    {
        String mnemonic = target[0].toUpperCase();
        int numOperands = target.length - 1;
        if (!instructionSet.containsMnemonic(mnemonic)) throw new NonexistentMnemonicError(lineNumber);

        CompiledLine compiledLine = new CompiledLine();

        // Exceptions
        if (instructionSet.isSpecialMnemonic(mnemonic))
        {
            InstructionSet.SpecialInstructionInfo instructionInfo = instructionSet.getSpecialInstructionInfo(mnemonic);
            Map<String, String> specialOpcodes = instructionInfo.getAddressingModes();
            int numOfSpecialOperands = instructionInfo.getOperands();

            List<String> operands = new ArrayList<>();
            for (int i = 1; i < target.length; i++)
            {
                operands.addAll(Arrays.asList(target[i].split(",#")));
            }
            numOperands = operands.size();

            if (numOperands < numOfSpecialOperands) throw new MissingOperandsError(lineNumber);
            else if (numOperands > numOfSpecialOperands) throw new UnnecessaryOperandError(lineNumber);

            String firstOperand = operands.get(0);
            int operandSize = firstOperand.length();

            // IND,X and IND,Y
            if (operandSize >= 2 && firstOperand.charAt(operandSize - 2) == ',')
            {
                if (firstOperand.charAt(operandSize - 1) != 'X' && firstOperand.charAt(operandSize - 1) != 'Y')
                    throw new BadFormatError(lineNumber);
                firstOperand = firstOperand.substring(0, operandSize - 2);

                int parsedOperand;
                try
                {
                    parsedOperand = parseNumericLiteral(firstOperand, lineNumber);
                }
                catch (NumberFormatException | NumericParsingError numberFormatException)
                {
                    if (!constantsAndVariables.containsKey(firstOperand))
                        throw new NonexistentVariableError(lineNumber);
                    parsedOperand = constantsAndVariables.get(firstOperand);
                }
                if (parsedOperand > 0xFF) throw new UnsupportedOperandMagnitudeError(lineNumber);

                String opcode;
                if (firstOperand.charAt(operandSize - 1) == 'X')
                {
                    if (!specialOpcodes.containsKey("IND,X")) throw new UnsupportedAddressingModeError(lineNumber);
                    opcode = specialOpcodes.get("IND,X");
                }
                else
                {
                    if (!specialOpcodes.containsKey("IND,Y")) throw new UnsupportedAddressingModeError(lineNumber);
                    opcode = specialOpcodes.get("IND,Y");
                }

                compiledLine.setOpcode(opcodeStringToInt(opcode));
                compiledLine.getOperands().add(parsedOperand);
                compiledLine.setSizeInBytes(getOpcodeSize(opcode) + numOfSpecialOperands);
            }
            // DIR
            else
            {
                int parsedOperand;
                try
                {
                    parsedOperand = parseNumericLiteral(firstOperand, lineNumber);
                }
                catch (NumberFormatException | NumericParsingError numberFormatException)
                {
                    if (!constantsAndVariables.containsKey(firstOperand))
                        throw new NonexistentVariableError(lineNumber);
                    parsedOperand = constantsAndVariables.get(firstOperand);
                }
                if (parsedOperand > 0xFF) throw new UnsupportedOperandMagnitudeError(lineNumber);


                String opcode = specialOpcodes.get("DIR");
                compiledLine.setOpcode(opcodeStringToInt(opcode));
                compiledLine.getOperands().add(parsedOperand);
                compiledLine.setSizeInBytes(getOpcodeSize(opcode) + numOfSpecialOperands);
            }

            String secondOperand = operands.get(1);
            int parsedOperand;
            try
            {
                parsedOperand = parseNumericLiteral(secondOperand, lineNumber);
            }
            catch (NumberFormatException | NumericParsingError numberFormatException)
            {
                if (!constantsAndVariables.containsKey(secondOperand)) throw new NonexistentVariableError(lineNumber);
                parsedOperand = constantsAndVariables.get(secondOperand);
            }
            if (parsedOperand > 0xFF) throw new UnsupportedOperandMagnitudeError(lineNumber);
            compiledLine.getOperands().add(parsedOperand);

            if (numOfSpecialOperands == 3)
            {
                String thirdOperand = operands.get(2);
                try
                {
                    parsedOperand = parseNumericLiteral(thirdOperand, lineNumber);
                }
                catch (NumberFormatException | NumericParsingError numberFormatException)
                {
                    if (constantsAndVariables.containsKey(thirdOperand))
                        parsedOperand = constantsAndVariables.get(thirdOperand);
                    else
                    {
                        compiledLine.getPendingIndexes().put(2, thirdOperand);
                    }
                }

                if (parsedOperand > 0xFF) throw new UnsupportedOperandMagnitudeError(lineNumber);
                compiledLine.getOperands().add(parsedOperand);
            }

            return compiledLine;
        }

        if (numOperands > 1) throw new UnnecessaryOperandError(lineNumber);
        Map<String, String> standardOpcodes = instructionSet.getStandardOpcodes(mnemonic);
        // INH
        if (numOperands == 0)
        {
            if (!standardOpcodes.containsKey("INH")) throw new MissingOperandsError(lineNumber);
            String opcode = standardOpcodes.get("INH");
            compiledLine.setOpcode(opcodeStringToInt(opcode));
            compiledLine.setSizeInBytes(getOpcodeSize(opcode));
            return compiledLine;
        }

        // 1 operand at this point
        String operand = target[1];
        // REL
        if (standardOpcodes.containsKey("REL"))
        {
            String opcode = standardOpcodes.get("REL");
            compiledLine.setOpcode(opcodeStringToInt(opcode));

            int parsedOperand;
            try
            {
                parsedOperand = parseNumericLiteral(operand, lineNumber);
            }
            catch (NumberFormatException | NumericParsingError numberFormatException)
            {
                if (constantsAndVariables.containsKey(operand))
                    parsedOperand = constantsAndVariables.get(operand);
                else
                {
                    compiledLine.getPendingIndexes().put(0, operand);
                    return compiledLine;
                }
            }

            if (parsedOperand > 0xFF) throw new UnsupportedOperandMagnitudeError(lineNumber);
            compiledLine.getOperands().add(parsedOperand);
            compiledLine.setSizeInBytes(2);
            return compiledLine;
        }

        // IMM
        if (operand.charAt(0) == '#')
        {
            if (!standardOpcodes.containsKey("IMM")) throw new UnsupportedAddressingModeError(lineNumber);

            int parsedOperand;
            try
            {
                parsedOperand = parseNumericLiteral(operand, lineNumber);
            }
            catch (NumberFormatException | NumericParsingError numberFormatException)
            {
                if (!constantsAndVariables.containsKey(operand)) throw new NonexistentConstantError(lineNumber);
                parsedOperand = constantsAndVariables.get(operand);
            }

            if (parsedOperand > 0xFFFF) throw new UnsupportedOperandMagnitudeError(lineNumber);
            String opcode = standardOpcodes.get("IMM");
            compiledLine.setOpcode(opcodeStringToInt(opcode));
            compiledLine.getOperands().add(parsedOperand);
            compiledLine.setSizeInBytes(getOpcodeSize(opcode) + getOperandSize(parsedOperand));
            return compiledLine;
        }

        int operandSize = operand.length();
        // IND,X and IND,Y
        if (operandSize >= 2 && operand.charAt(operandSize - 2) == ',')
        {
            if (operand.charAt(operandSize - 1) != 'X' && operand.charAt(operandSize - 1) != 'Y')
                throw new BadFormatError(lineNumber);
            operand = operand.substring(0, operandSize - 2);

            int parsedOperand;
            try
            {
                parsedOperand = parseNumericLiteral(operand, lineNumber);
            }
            catch (NumberFormatException | NumericParsingError numberFormatException)
            {
                if (!constantsAndVariables.containsKey(operand)) throw new NonexistentVariableError(lineNumber);
                parsedOperand = constantsAndVariables.get(operand);
            }
            if (parsedOperand > 0xFF) throw new UnsupportedOperandMagnitudeError(lineNumber);

            String opcode;
            if (operand.charAt(operandSize - 1) == 'X')
            {
                if (!standardOpcodes.containsKey("IND,X")) throw new UnsupportedAddressingModeError(lineNumber);
                opcode = standardOpcodes.get("IND,X");
            }
            else
            {
                if (!standardOpcodes.containsKey("IND,Y")) throw new UnsupportedAddressingModeError(lineNumber);
                opcode = standardOpcodes.get("IND,Y");
            }

            compiledLine.setOpcode(opcodeStringToInt(opcode));
            compiledLine.getOperands().add(parsedOperand);
            compiledLine.setSizeInBytes(getOpcodeSize(opcode) + 1);
            return compiledLine;
        }

        // DIR and EXT
        int parsedOperand;
        try
        {
            parsedOperand = parseNumericLiteral(operand, lineNumber);
        }
        catch (NumberFormatException | NumericParsingError numberFormatException)
        {
            if (!constantsAndVariables.containsKey(operand)) throw new NonexistentVariableError(lineNumber);
            parsedOperand = constantsAndVariables.get(operand);
        }

        // DIR
        if (parsedOperand <= 0xFF)
        {
            if (!standardOpcodes.containsKey("DIR")) throw new UnsupportedAddressingModeError(lineNumber);
            String opcode = standardOpcodes.get("DIR");
            compiledLine.setOpcode(opcodeStringToInt(opcode));
            compiledLine.setSizeInBytes(getOpcodeSize(opcode) + 1);
        }
        // EXT
        else if (parsedOperand <= 0xFFFF)
        {
            if (!standardOpcodes.containsKey("EXT")) throw new UnsupportedAddressingModeError(lineNumber);
            String opcode = standardOpcodes.get("EXT");
            compiledLine.setOpcode(opcodeStringToInt(opcode));
            compiledLine.setSizeInBytes(getOpcodeSize(opcode) + 2);
        }
        else throw new UnsupportedOperandMagnitudeError(lineNumber);

        compiledLine.getOperands().add(parsedOperand);
        return compiledLine;
    }

    private int getOperandSize(int operand)
    {
        if (operand <= 0xFF) return 1;
        else return 2;
    }

    private int getOpcodeSize(String opcode)
    {
        return opcode.replaceAll("\\s", "").length() / 2;
    }

    private int opcodeStringToInt(String opcode)
    {
        return Integer.parseUnsignedInt(opcode.replaceAll("\\s", ""), 16);
    }


    @Data
    private static class CompiledLine
    {
        private Integer address;
        private Integer opcode;
        private List<Integer> operands = new ArrayList<>();
        private Map<Integer, String> pendingIndexes = new HashMap<>();
        private int sizeInBytes = 0;

        public boolean isEmpty()
        {
            return (address == null && opcode == null && operands.isEmpty());
        }
    }
}
