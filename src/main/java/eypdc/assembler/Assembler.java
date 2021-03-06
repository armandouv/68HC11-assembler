package eypdc.assembler;

import eypdc.assembler.errors.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
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
        String filename = Paths.get(sourcePath).getFileName().toString();
        if (!filename.endsWith(".asc")) throw new RuntimeException("Unsupported file format (must be *.asc");
        String rawFilename = filename.substring(0, filename.length() - 4);

        Assembler assembler = new Assembler();
        Printer printer = new Printer(rawFilename);
        List<String> lines;

        // Read input file
        try (BufferedReader inputStream = new BufferedReader(
                new FileReader(sourcePath, Charset.forName("windows-1252"))))
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

        // Compile
        List<CompiledLine> compiledLines = new ArrayList<>();
        try
        {
            Map<String, Integer> labels = assembler.firstPass(lines, compiledLines);
            assembler.secondPass(compiledLines, labels);
        }
        catch (CompileError compileError)
        {
            compileError.printStackTrace();
            printer.printErrorToList(compileError.getMessage());
            return;
        }

        printer.printList(compiledLines, lines);
        printer.printObjectCode(compiledLines);
        printer.printColoredList(compiledLines, lines);
        printer.printOfficialObjectCode(compiledLines);
        printer.printColoredOfficialObjectCode(compiledLines);
    }

    private void secondPass(List<CompiledLine> outputLines, Map<String, Integer> labels) throws CompileError
    {
        String jmpExtOpcodeString = instructionSet.getStandardOpcodes("jmp").get("EXT");
        int jmpExtOpcode = opcodeStringToInt(jmpExtOpcodeString);

        String jsrExtOpcodeString = instructionSet.getStandardOpcodes("jsr").get("EXT");
        int jsrExtOpcode = opcodeStringToInt(jsrExtOpcodeString);

        for (int i = 0; i < outputLines.size(); i++)
        {
            CompiledLine compiledLine = outputLines.get(i);
            if (!compiledLine.isPending()) continue;

            List<Integer> operands = compiledLine.getOperands();
            Map<Integer, String> pendingIndexToLabel = compiledLine.getPendingIndexes();

            for (String label : pendingIndexToLabel.values())
            {
                if (!labels.containsKey(label)) throw new NonexistentLabelError(i);
                // IGNORES INDEX (REFACTOR IF NECESSARY)
                int targetAddress = labels.get(label);

                if (compiledLine.getOpcode().equals(jmpExtOpcode) || compiledLine.getOpcode().equals(jsrExtOpcode))
                {
                    if (targetAddress > 0xFFFF) throw new VeryLargeAbsoluteJumpError(i);
                    operands.add(targetAddress);
                }
                else // Relative jump
                {
                    int nextAddress = compiledLine.getAddress() + compiledLine.getSizeInBytes();
                    int jump = targetAddress - nextAddress;
                    if (Math.abs(jump) > 0xFF) throw new VeryLargeRelativeJumpError(i);
                    operands.add(jump);
                }
            }
        }
    }

    private Map<String, Integer> firstPass(List<String> inputLines, List<CompiledLine> outputLines) throws CompileError
    {
        Map<String, Integer> labels = new HashMap<>();
        Map<String, Integer> constantsAndVariables = new HashMap<>();
        Integer targetAddress = null;
        boolean hasEndDirective = false;

        for (int i = 0; i < inputLines.size(); i++)
        {
            String line = inputLines.get(i).toUpperCase();
            int commentIndex = line.indexOf('*');
            if (commentIndex != -1) line = line.substring(0, commentIndex);

            // Blank line -> empty CompiledLine
            if (line.isBlank())
            {
                outputLines.add(new CompiledLine());
                continue;
            }

            // Line is not empty but END directive was already parsed.
            if (hasEndDirective) throw new EndConflictError(i);

            CompiledLine compiledLine = new CompiledLine();
            String[] splitLine = line.strip().split(" +");

            // First character is not a blank space.
            if (!line.split("")[0].matches("\\s"))
            {
                // Check for EQU expression.
                if (splitLine.length > 1 && splitLine[1].equalsIgnoreCase("EQU"))
                {
                    if (splitLine.length == 2) throw new MissingOperandsError(i);
                    if (splitLine.length > 3) throw new UnnecessaryOperandError(i);
                    constantsAndVariables.put(splitLine[0], parseNumericLiteral(splitLine[2], i));
                    outputLines.add(compiledLine);
                    continue;
                }

                if (splitLine[0].equalsIgnoreCase("RESET"))
                {
                    if (splitLine.length == 1)
                    {
                        outputLines.add(compiledLine);
                        continue;
                    }
                    else splitLine = Arrays.copyOfRange(splitLine, 1, splitLine.length);
                }
                else
                {
                    // If it is not a label, then it is an invalid instruction.
                    if (splitLine.length != 1) throw new NonexistentMarginSpaceError(i);

                    // Add label
                    if (targetAddress == null) throw new NonexistentOrgDirective(i);
                    String label = splitLine[0].replaceAll(":", "");
                    if (labels.containsKey(label)) throw new ExistingLabelError(i);
                    labels.put(label, targetAddress);
                    outputLines.add(compiledLine);
                    continue;
                }
            }

            // Line does have space at start, handle directives.
            if (splitLine[0].equalsIgnoreCase("ORG"))
            {
                if (splitLine.length > 2) throw new UnnecessaryOperandError(i);
                if (splitLine.length == 1) throw new MissingOperandsError(i);
                targetAddress = parseNumericLiteral(splitLine[1], i);
                outputLines.add(compiledLine);
                continue;
            }


            if (splitLine[0].equalsIgnoreCase("END"))
            {
                // TODO: Handle operand
                // if (splitLine.length != 1) throw new UnnecessaryOperandError(i);
                hasEndDirective = true;
                outputLines.add(compiledLine);
                continue;
            }

            // Target address has not been defined yet through the ORG directive, but we need it to generate the object code.
            if (targetAddress == null) throw new NonexistentOrgDirective(i);

            if (splitLine[0].equalsIgnoreCase("FCB"))
            {
                if (splitLine.length > 2) throw new UnnecessaryOperandError(i);
                if (splitLine.length == 1) throw new MissingOperandsError(i);
                String[] operands = splitLine[1].split(",");
                List<Integer> formedBytes = new ArrayList<>();
                for (String operand : operands)
                {
                    int formedByte = parseNumericLiteral(operand, i);
                    if (formedByte > 0xFF) throw new UnsupportedOperandMagnitudeError(i);
                    formedBytes.add(formedByte);
                }
                compiledLine.setAddress(targetAddress);
                compiledLine.setOpcode(formedBytes.get(0));
                for (int j = 1; j < formedBytes.size(); j++)
                {
                    compiledLine.getOperands().add(formedBytes.get(j));
                }
                compiledLine.setSizeInBytes(formedBytes.size());
                outputLines.add(compiledLine);
                targetAddress += compiledLine.getSizeInBytes();
                continue;
            }

            // Line contains a mnemonic
            compiledLine = compileLine(splitLine, i, constantsAndVariables);
            compiledLine.setAddress(targetAddress);
            targetAddress += compiledLine.getSizeInBytes();
            outputLines.add(compiledLine);
        }

        // Document does not contain END directive.
        if (!hasEndDirective) throw new NonexistentEndDirectiveError(inputLines.size());
        return labels;
    }


    private CompiledLine compileLine(String[] target, int lineNumber, Map<String, Integer> constantsAndVariables)
            throws CompileError
    {
        String mnemonic = target[0].toLowerCase();
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

                compiledLine = compileIndInstruction(constantsAndVariables, specialOpcodes, firstOperand, lineNumber,
                                                     numOfSpecialOperands);
            }
            // DIR
            else
            {
                int parsedOperand = parseVariable(constantsAndVariables, firstOperand, lineNumber, 0xFF);

                String opcode = specialOpcodes.get("DIR");
                compiledLine.setOpcode(opcodeStringToInt(opcode));
                compiledLine.getOperands().add(parsedOperand);
                compiledLine.setSizeInBytes(getOpcodeSize(opcode) + numOfSpecialOperands);
            }

            String secondOperand = operands.get(1);
            int parsedSecondOperand = parseVariable(constantsAndVariables, secondOperand, lineNumber, 0xFF);
            compiledLine.getOperands().add(parsedSecondOperand);

            if (numOfSpecialOperands == 3)
            {
                String thirdOperand = operands.get(2);
                Integer parsedThirdOperand =
                        parseNthOperandOrLabel(constantsAndVariables, compiledLine, thirdOperand, lineNumber, 2, 0xFF);
                if (parsedThirdOperand == null) return compiledLine;
                compiledLine.getOperands().add(parsedSecondOperand);
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

        // REL (Does not share mnemonics)
        if (standardOpcodes.containsKey("REL"))
        {
            String opcode = standardOpcodes.get("REL");
            compiledLine.setSizeInBytes(2);
            compiledLine.setOpcode(opcodeStringToInt(opcode));

            Integer parsedOperand =
                    parseOnlyOperandOrLabel(constantsAndVariables, compiledLine, operand, lineNumber, 0xFF);
            if (parsedOperand == null) return compiledLine;

            compiledLine.getOperands().add(parsedOperand);
            return compiledLine;
        }

        // IMM
        if (operand.charAt(0) == '#')
        {
            if (!standardOpcodes.containsKey("IMM")) throw new UnsupportedAddressingModeError(lineNumber);
            operand = operand.substring(1);

            int parsedOperand = parseConstant(constantsAndVariables, operand, lineNumber);

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
            return compileIndInstruction(constantsAndVariables, standardOpcodes, operand, lineNumber, 1);
        }

        // DIR and EXT

        // JMP (EXT)
        if (mnemonic.equalsIgnoreCase("JMP"))
        {
            String opcode = standardOpcodes.get("EXT");
            compiledLine.setOpcode(opcodeStringToInt(opcode));
            compiledLine.setSizeInBytes(getOpcodeSize(opcode) + 2);

            Integer parsedOperand =
                    parseOnlyOperandOrLabel(constantsAndVariables, compiledLine, operand, lineNumber, 0xFFFF);
            if (parsedOperand == null) return compiledLine;

            compiledLine.getOperands().add(parsedOperand);
            return compiledLine;
        }

        // JSR (DIR or EXT)
        if (mnemonic.equalsIgnoreCase("JSR"))
        {
            Integer parsedOperand =
                    parseOnlyOperandOrLabel(constantsAndVariables, compiledLine, operand, lineNumber, 0xFFFF);
            // EXT, label
            if (parsedOperand == null)
            {
                String opcode = standardOpcodes.get("EXT");
                compiledLine.setOpcode(opcodeStringToInt(opcode));
                compiledLine.setSizeInBytes(getOpcodeSize(opcode) + 2);
                return compiledLine;
            }
            // EXT, parsed value
            else if (parsedOperand > 0xFF)
            {
                String opcode = standardOpcodes.get("EXT");
                compiledLine.setOpcode(opcodeStringToInt(opcode));
                compiledLine.setSizeInBytes(getOpcodeSize(opcode) + 2);
                compiledLine.getOperands().add(parsedOperand);
                return compiledLine;
            }

            // DIR, operand <= OxFF
            String opcode = standardOpcodes.get("DIR");
            compiledLine.setOpcode(opcodeStringToInt(opcode));
            compiledLine.setSizeInBytes(getOpcodeSize(opcode) + 1);
            compiledLine.getOperands().add(parsedOperand);
            return compiledLine;
        }

        int parsedOperand = parseVariable(constantsAndVariables, operand, lineNumber, 0xFFFF);

        // DIR
        if (parsedOperand <= 0xFF && standardOpcodes.containsKey("DIR"))
        {
            String opcode = standardOpcodes.get("DIR");
            compiledLine.setOpcode(opcodeStringToInt(opcode));
            compiledLine.setSizeInBytes(getOpcodeSize(opcode) + 1);
        }
        // EXT
        else // (parsedOperand <= 0xFFFF)
        {
            if (!standardOpcodes.containsKey("EXT")) throw new UnsupportedAddressingModeError(lineNumber);
            String opcode = standardOpcodes.get("EXT");
            compiledLine.setOpcode(opcodeStringToInt(opcode));
            compiledLine.setSizeInBytes(getOpcodeSize(opcode) + 2);
        }

        compiledLine.getOperands().add(parsedOperand);
        return compiledLine;
    }

    private CompiledLine compileIndInstruction(Map<String, Integer> constantsAndVariables, Map<String, String> opcodes,
                                               String operand, int lineNumber, int operandsSizeInBytes)
            throws UnsupportedOperandMagnitudeError, NonexistentVariableError, UnsupportedAddressingModeError
    {
        CompiledLine compiledLine = new CompiledLine();
        int operandSize = operand.length();

        String strippedOperand = operand.substring(0, operandSize - 2);
        int parsedOperand = parseVariable(constantsAndVariables, strippedOperand, lineNumber, 0xFF);

        String opcode;
        if (operand.charAt(operandSize - 1) == 'X')
        {
            if (!opcodes.containsKey("IND,X")) throw new UnsupportedAddressingModeError(lineNumber);
            opcode = opcodes.get("IND,X");
        }
        else
        {
            if (!opcodes.containsKey("IND,Y")) throw new UnsupportedAddressingModeError(lineNumber);
            opcode = opcodes.get("IND,Y");
        }

        compiledLine.setOpcode(opcodeStringToInt(opcode));
        compiledLine.getOperands().add(parsedOperand);
        compiledLine.setSizeInBytes(getOpcodeSize(opcode) + operandsSizeInBytes);
        return compiledLine;
    }

    private int parseConstant(Map<String, Integer> constantsAndVariables, String operand, int lineNumber)
            throws NonexistentConstantError, UnsupportedOperandMagnitudeError
    {
        try
        {
            return parseOperand(constantsAndVariables, operand, lineNumber, 65535);
        }
        catch (NonexistentConstantOrVariableError error)
        {
            throw new NonexistentConstantError(lineNumber);
        }
    }

    private int parseVariable(Map<String, Integer> constantsAndVariables, String operand, int lineNumber, int maxValue)
            throws NonexistentVariableError, UnsupportedOperandMagnitudeError
    {
        try
        {
            return parseOperand(constantsAndVariables, operand, lineNumber, maxValue);
        }
        catch (NonexistentConstantOrVariableError error)
        {
            throw new NonexistentVariableError(lineNumber);
        }
    }

    private int parseOperand(Map<String, Integer> constantsAndVariables, String operand, int lineNumber, int maxValue)
            throws UnsupportedOperandMagnitudeError, NonexistentConstantOrVariableError
    {
        int parsedOperand;
        try
        {
            parsedOperand = parseNumericLiteral(operand, lineNumber);
        }
        catch (NumberFormatException | NumericParsingError numberFormatException)
        {
            if (!constantsAndVariables.containsKey(operand)) throw new NonexistentConstantOrVariableError(lineNumber);
            parsedOperand = constantsAndVariables.get(operand);
        }

        if (parsedOperand > maxValue) throw new UnsupportedOperandMagnitudeError(lineNumber);
        return parsedOperand;
    }

    private Integer parseOnlyOperandOrLabel(Map<String, Integer> constantsAndVariables, CompiledLine compiledLine,
                                            String operand, int lineNumber, int maxValue)
            throws UnsupportedOperandMagnitudeError
    {
        return parseNthOperandOrLabel(constantsAndVariables, compiledLine, operand, lineNumber, 0, maxValue);
    }

    private Integer parseNthOperandOrLabel(Map<String, Integer> constantsAndVariables, CompiledLine compiledLine,
                                           String operand, int lineNumber, int operandIndex, int maxValue)
            throws UnsupportedOperandMagnitudeError
    {
        Integer parsedOperand = null;
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
                compiledLine.getPendingIndexes().put(operandIndex, operand);
            }
        }
        if (parsedOperand != null && parsedOperand > maxValue) throw new UnsupportedOperandMagnitudeError(lineNumber);
        return parsedOperand;
    }

    private int parseNumericLiteral(String operand, int lineNumber) throws NumericParsingError, NumberFormatException
    {
        if (operand.isBlank()) throw new NumericParsingError(lineNumber);

        int result;
        char firstChar = operand.charAt(0);
        if (firstChar == '$') result = Integer.parseUnsignedInt(operand.substring(1), 16);
        else if (firstChar == '%') result = Integer.parseUnsignedInt(operand.substring(1), 2);
        else if (firstChar == '\'') result = operand.charAt(1);
        else result = Integer.parseUnsignedInt(operand, 10);

        return result;
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
}
