package eypdc.assembler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        BufferedReader inputStream;

        try
        {
            inputStream = new BufferedReader(new FileReader(sourcePath));
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException("Could not open input file", e);
        }


        StringBuilder output = new StringBuilder();
        Map<String, Integer> labels;
        try
        {
            labels = assembler.firstPass(inputStream, output);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while performing first pass", e);
        }
        assembler.secondPass(output, labels);

    }

    private void secondPass(StringBuilder output, Map<String, Integer> labels)
    {

    }

    private Map<String, Integer> firstPass(BufferedReader reader, StringBuilder output) throws IOException
    {
        Map<String, Integer> labels = new HashMap<>();

        String line;
        while ((line = reader.readLine()) != null)
        {
            if (!requiresSecondPass(line))
                output.append(compileLine(line)).append("\n");
        }

        return labels;
    }

    private boolean requiresSecondPass(String line)
    {
        return false;
    }

    private String compileLine(String target)
    {
        return "";
    }
}
