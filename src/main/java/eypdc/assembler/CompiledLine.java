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

    public static String addSpaceToHexString(String hexString)
    {
        StringBuilder stringBuilder = new StringBuilder();
        int length = hexString.length();
        for (int i = 0; i < length; i += 2)
        {
            stringBuilder.append(hexString.charAt(i));
            if (i + 1 < length) stringBuilder.append(hexString.charAt(i + 1));
            if (i < length - 2) stringBuilder.append(' ');
        }
        return stringBuilder.toString();
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

    private int getMaxValueFromBytes(int sizeInBytes)
    {
        return getMaxValueFromBits(8 * sizeInBytes);
    }

    private int getMaxValueFromBits(int sizeInBits)
    {
        return (int) (Math.pow(2, sizeInBits) - 1);
    }

    private String getHexRepresentation(Integer value, int sizeInBytes)
    {
        if (value >= 0)
        {
            if (value > getMaxValueFromBytes(sizeInBytes)) throw new RuntimeException("Value does not fit size");
            return String.format("%0" + (sizeInBytes * 2) + "x", value).toUpperCase();
        }

        // Value is negative, use two's complement
        int maxNegativeValue = getMaxValueFromBits(sizeInBytes * 8 - 1);
        if (value > maxNegativeValue || value < (-maxNegativeValue - 1))
            throw new RuntimeException("Value does not fit size");
        String binaryRepresentation = Integer.toBinaryString(value);
        int length = binaryRepresentation.length();
        String trimmed = binaryRepresentation.substring(length - 8 * sizeInBytes);
        int unsignedValue = Integer.parseUnsignedInt(trimmed, 2);
        return Integer.toHexString(unsignedValue).toUpperCase();
    }

    public List<String> getSplitBinaryRepresentation()
    {
        List<String> binaryRepresentation = new ArrayList<>();
        if (isEmpty()) return binaryRepresentation;
        String opcodeRepr = getHexRepresentation(opcode, getOpcodeSizeInBytes());
        binaryRepresentation.add(opcodeRepr);

        int operandSize = getOperandsSizeInBytes();
        for (Integer operand : operands)
        {
            binaryRepresentation.add(getHexRepresentation(operand, operandSize));
        }

        return binaryRepresentation;
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

