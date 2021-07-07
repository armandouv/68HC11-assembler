package eypdc.assembler;

import eypdc.assembler.errors.CompileError;

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
        this.instructionSet = InstructionSet.parseFromJson("instruction_set.json");
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

        List<String> rawCompiledLines = new ArrayList<>();

        try
        {
            Map<String, Integer> labels = assembler.firstPass(lines, rawCompiledLines);
            assembler.secondPass(rawCompiledLines, labels);
        }
        catch (CompileError compileError)
        {
            compileError.printStackTrace();
        }

        // TODO: Write formatted output to *.ASC and *.LST files.

    }

    private void secondPass(List<String> outputLines, Map<String, Integer> labels) throws CompileError
    {

    }

    private Map<String, Integer> firstPass(List<String> inputLines, List<String> outputLines) throws CompileError
    {
        Map<String, Integer> labels = new HashMap<>();

        for (String line : inputLines)
        {
            outputLines.add(compileLine(line));
        }

        return labels;
    }

    private String compileLine(String target)
    {
        return "";
    }
}
