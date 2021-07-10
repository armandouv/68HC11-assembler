package eypdc.assembler;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class CompiledLine
{
    private Integer address;
    private Integer opcode;
    private List<Integer> operands = new ArrayList<>();
    private Map<Integer, String> pendingIndexes = new HashMap<>();
    private int sizeInBytes = 0;

    public static Map<Integer, String> getMergedRepresentation(List<CompiledLine> compiledLines)
    {
        StringBuilder stringBuilder = new StringBuilder();
        Map<Integer, String> startAddressToRepresentation = new HashMap<>();
        int currentAddress = -1;
        int startAddress = -1;

        for (CompiledLine compiledLine : compiledLines)
        {
            if (compiledLine.isEmpty()) continue;
            // Check if addresses are correct
            if (startAddress == -1) startAddress = compiledLine.getAddress();
            else if (currentAddress != -1 && compiledLine.getAddress() != currentAddress)
            {
                startAddressToRepresentation.put(startAddress, stringBuilder.toString());
                stringBuilder.setLength(0);
                startAddress = compiledLine.getAddress();
            }

            stringBuilder.append(compiledLine.getBinaryRepresentation());
            currentAddress = compiledLine.getAddress() + compiledLine.getSizeInBytes();
        }
        if (startAddress == -1) throw new RuntimeException("Start address was not obtained");
        else startAddressToRepresentation.put(startAddress, stringBuilder.toString());

        return startAddressToRepresentation;
    }

    public String getSpacedRepresentation()
    {
        if (isEmpty()) return "<Vacio>";

        String repr = Integer.toString(address, 16) + " (" + getBinaryRepresentation() + ")";
        return repr.toUpperCase();
    }

    public int getOpcodeSizeInBytes()
    {
        if (opcode >= 0 && opcode <= 0xFF) return 1;
        else if (opcode >= 0 && opcode <= 0xFFFF) return 2;
        else throw new RuntimeException("Unexpected opcode size");
    }

    public int getOperandsSizeInBytes()
    {
        if (operands.size() > 1) return 1;
        else return sizeInBytes - getOpcodeSizeInBytes();
    }

    public List<String> getSplitBinaryRepresentation()
    {
        List<String> binaryRepresentation = new ArrayList<>();
        if (isEmpty()) return binaryRepresentation;
        String opcodeRepr = Util.getHexRepresentation(opcode, getOpcodeSizeInBytes());
        binaryRepresentation.add(opcodeRepr);

        int operandSize = getOperandsSizeInBytes();
        for (Integer operand : operands)
        {
            binaryRepresentation.add(Util.getHexRepresentation(operand, operandSize));
        }

        return binaryRepresentation;
    }

    public String getColoredRepresentation()
    {
        List<String> splitRepresentation = getSplitBinaryRepresentation();
        if (splitRepresentation.isEmpty()) return "";

        StringBuilder coloredRepresentation = new StringBuilder();

        char cssClass = 'a';
        for (String element : splitRepresentation)
        {
            coloredRepresentation.append("<span class=\"").append(cssClass).append("\">")
                    .append(element).append("</span>");
            cssClass++;
        }

        return coloredRepresentation.toString();
    }

    public String getColoredSpacedRepresentation()
    {
        List<String> splitRepresentation = getSplitBinaryRepresentation();
        if (splitRepresentation.isEmpty()) return " Vacio ";
        StringBuilder coloredRepresentation = new StringBuilder();

        for (String element : splitRepresentation)
        {
            coloredRepresentation.append("<span>").append(element).append("</span>");
        }

        return Integer.toString(address, 16).toUpperCase() + " (" + coloredRepresentation + ")";
    }

    public String getBinaryRepresentation()
    {
        StringBuilder representation = new StringBuilder();
        for (String element : getSplitBinaryRepresentation())
        {
            representation.append(element);
        }
        return representation.toString();
    }

    public boolean isPending()
    {
        return !pendingIndexes.isEmpty();
    }

    public boolean isEmpty()
    {
        return (address == null && opcode == null && operands.isEmpty());
    }
}

