package eypdc.assembler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class Printer
{
    private final String rawFilename;

    public Printer(String rawFilename)
    {
        this.rawFilename = rawFilename;
    }

    public void printErrorToList(String errorMessage)
    {
        PrintWriter printer = createOutputFile(rawFilename + ".lst");
        printer.println(errorMessage);
        printer.close();
    }

    public void printList(List<CompiledLine> compiledLines, List<String> originalLines)
    {
        PrintWriter printer = createOutputFile(rawFilename + ".lst");

        if (compiledLines.size() != originalLines.size())
            throw new RuntimeException("Compiled lines and original don't match");
        for (int i = 0; i < compiledLines.size(); i++)
        {
            CompiledLine currentCompiledLine = compiledLines.get(i);
            String currentOriginalLine = originalLines.get(i).strip();
            printer.println(i + " : " + currentCompiledLine.getSpacedRepresentation() + " : " + currentOriginalLine);
        }
        printer.close();
    }

    public void printColoredList(List<CompiledLine> compiledLines, List<String> originalLines)
    {
        PrintWriter printer = createOutputFile(rawFilename + "_lst.html");

        if (compiledLines.size() != originalLines.size())
            throw new RuntimeException("Compiled lines and original don't match");

        printer.println("<style>" +
                        "p span:nth-of-type(1) { color: red }" +
                        "p span:nth-of-type(2) { color: blue }" +
                        "p span:nth-of-type(3) { color: green }" +
                        "p span:nth-of-type(4) { color: purple }" +
                        "</style>");
        printer.println("<div>");
        for (int i = 0; i < compiledLines.size(); i++)
        {
            CompiledLine currentCompiledLine = compiledLines.get(i);
            String currentOriginalLine = originalLines.get(i).strip();

            printer.println(
                    "<p>" + i + " : " + currentCompiledLine
                            .getColoredSpacedRepresentation() + " : " + currentOriginalLine + "</p>");
        }
        printer.println("</div>");
        printer.close();
    }

    public void printObjectCode(List<CompiledLine> compiledLines)
    {
        PrintWriter printer = createOutputFile(rawFilename + ".s19");
        int lineSizeInBytes = 16;
        Map<Integer, String> mergedRepresentation = CompiledLine.getMergedRepresentation(compiledLines);

        for (Map.Entry<Integer, String> entry : mergedRepresentation.entrySet())
        {
            int startAddress = entry.getKey();
            String binaryRepresentation = entry.getValue();

            int address = startAddress;
            int totalLength = binaryRepresentation.length();
            for (int i = 0; i < totalLength; i += lineSizeInBytes * 2)
            {
                int endIndex = Math.min(totalLength, i + lineSizeInBytes * 2);
                String line = binaryRepresentation.substring(i, endIndex);
                String spacedLine = CompiledLine.addSpaceToHexString(line);
                printer.println("<" + Integer.toHexString(address) + "> " + spacedLine);
                address += lineSizeInBytes;
            }
        }

        printer.close();
    }

    public void printOfficialObjectCode(List<CompiledLine> compiledLines)
    {
        PrintWriter printer = createOutputFile(rawFilename + "_official.s19");
        Map<Integer, String> mergedRepresentation = CompiledLine.getMergedRepresentation(compiledLines);
        StringBuilder output = new StringBuilder();

        output.append("S110");

        for (Map.Entry<Integer, String> entry : mergedRepresentation.entrySet())
        {
            int startAddress = entry.getKey();
            String compiledCode = entry.getValue();
            output.append(startAddress).append(compiledCode);
        }

        output.append("19S9030000FC");

        printer.println(output);
        printer.close();
    }

    public void printColoredOfficialObjectCode(List<CompiledLine> compiledLines)
    {
        PrintWriter printer = createOutputFile(rawFilename + "_official_colored.html");
        printer.println("<style>" +
                        ".a { color: red }" +
                        ".b { color: blue }" +
                        ".c { color: green }" +
                        ".d { color: purple }" +
                        "</style>");
        printer.println("<p>El color rojo indica un codigo de instruccion. Los demas colores indican operandos.</p>");
        printer.println("<p>");

        // TODO: Generate official Motorola format.

        StringBuilder output = new StringBuilder();

        output.append("S110");

        for (CompiledLine compiledLine : compiledLines)
        {
            output.append(compiledLine.getColoredRepresentation());
        }

        output.append("19S9030000FC").append("</p>");

        printer.println(output);
        printer.close();
    }

    private PrintWriter createOutputFile(String filename)
    {
        try
        {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filename));
            return new PrintWriter(bufferedWriter);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not create " + filename);
        }
    }
}
