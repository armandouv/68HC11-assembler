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
    private final String HTML_STYLE = "<style>" +
                                      ".a { color: red }" +
                                      ".b { color: blue }" +
                                      ".c { color: green }" +
                                      ".d { color: purple }" +
                                      "</style>"
                                      + "<p>El color rojo indica un codigo de instruccion. Los demas colores indican operandos.</p>";

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

        printer.println(HTML_STYLE);
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
        int maxLineSizeInBytes = 16;
        Map<Integer, String> mergedRepresentation = CompiledLine.getMergedRepresentation(compiledLines);

        for (Map.Entry<Integer, String> entry : mergedRepresentation.entrySet())
        {
            int startAddress = entry.getKey();
            String binaryRepresentation = entry.getValue();

            int address = startAddress;
            int totalLength = binaryRepresentation.length();
            for (int i = 0; i < totalLength; i += maxLineSizeInBytes * 2)
            {
                int endIndex = Math.min(totalLength, i + maxLineSizeInBytes * 2);
                String line = binaryRepresentation.substring(i, endIndex);
                String spacedLine = Util.addSpaceToHexString(line);
                printer.println("<" + Integer.toHexString(address) + "> " + spacedLine);
                address += maxLineSizeInBytes;
            }
        }

        printer.close();
    }

    private String calculateChecksum(String objectCode)
    {
        if (objectCode.length() % 2 != 0) throw new RuntimeException("Line must contain a whole number of bytes");
        String[] splitLine = Util.addSpaceToHexString(objectCode).split(" ");
        int totalSum = 0;
        for (String hexByte : splitLine)
        {
            totalSum += Integer.parseInt(hexByte, 16);
        }
        byte truncatedSum = (byte) ~((byte) totalSum);
        return Util.getHexRepresentation((int) truncatedSum, 1);
    }

    public void printOfficialObjectCode(List<CompiledLine> compiledLines)
    {
        PrintWriter printer = createOutputFile(rawFilename + "_official.s19");
        int maxLineSizeInBytes = 32;
        Map<Integer, String> mergedRepresentation = CompiledLine.getMergedRepresentation(compiledLines);

        for (Map.Entry<Integer, String> entry : mergedRepresentation.entrySet())
        {
            int startAddress = entry.getKey();
            String binaryRepresentation = entry.getValue();

            int address = startAddress;
            int totalLength = binaryRepresentation.length();
            for (int i = 0; i < totalLength; i += maxLineSizeInBytes * 2)
            {
                int endIndex = Math.min(totalLength, i + maxLineSizeInBytes * 2);
                String line = binaryRepresentation.substring(i, endIndex);
                int lineSizeInBytes = line.length() / 2;

                String formattedLine =
                        Util.toHexString(lineSizeInBytes + 3) + Util.toHexString(address) + line;
                printer.println("S1" + formattedLine + calculateChecksum(formattedLine));
                address += maxLineSizeInBytes;
            }
        }

        printer.println("S9030000FC");
        printer.close();
    }

    public void printColoredOfficialObjectCode(List<CompiledLine> compiledLines)
    {
        PrintWriter printer = createOutputFile(rawFilename + "_official_colored.html");
        printer.println(HTML_STYLE);

        int maxLineSizeInBytes = 32;
        Map<Integer, String> mergedColoredRepresentation = CompiledLine.getMergedColoredRepresentation(compiledLines);
        Map<Integer, String> mergedRepresentation = CompiledLine.getMergedRepresentation(compiledLines);


        for (Map.Entry<Integer, String> entry : mergedRepresentation.entrySet())
        {
            int startAddress = entry.getKey();
            String binaryRepresentation = mergedRepresentation.get(startAddress);
            String coloredRepresentation = mergedColoredRepresentation.get(startAddress);

            int address = startAddress;
            int totalLength = binaryRepresentation.length();
            int startColoredIndex = 0;
            for (int i = 0; i < totalLength; i += maxLineSizeInBytes * 2)
            {
                int endIndex = Math.min(totalLength, i + maxLineSizeInBytes * 2);
                String line = binaryRepresentation.substring(i, endIndex);

                int lineSizeInBytes = line.length() / 2;
                String formattedLine =
                        Util.toHexString(lineSizeInBytes + 3) + Util.toHexString(address) + line;
                String checksum = calculateChecksum(formattedLine);

                String prefix = "<p>S1" + Util.toHexString(lineSizeInBytes + 3) + Util.toHexString(address);
                String suffix = checksum + "</p>";

                int endColoredIndex = CompiledLine
                        .getColoredRepresentationEnd(coloredRepresentation, startColoredIndex, lineSizeInBytes);
                String colored = coloredRepresentation.substring(startColoredIndex, endColoredIndex);
                startColoredIndex = endColoredIndex;
                printer.println(prefix + colored + suffix);
                address += maxLineSizeInBytes;
            }
        }

        printer.println("<p>S9030000FC</p>");
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
