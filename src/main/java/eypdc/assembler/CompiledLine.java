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
            return String.format("%0" + (sizeInBytes * 2) + "x", value);
        }

        // Value is negative, use two's complement
        int maxNegativeValue = getMaxValueFromBits(sizeInBytes * 8 - 1);
        if (value > maxNegativeValue || value < (-maxNegativeValue - 1))
            throw new RuntimeException("Value does not fit size");
        String binaryRepresentation = Integer.toBinaryString(value);
        int length = binaryRepresentation.length();
        String trimmed = binaryRepresentation.substring(length - 8 * sizeInBytes);
        int unsignedValue = Integer.parseUnsignedInt(trimmed, 2);
        return Integer.toHexString(unsignedValue);
    }

    public String getBinaryRepresentation()
    {
        String opcodeRepr = getHexRepresentation(opcode, getOpcodeSizeInBytes());
        StringBuilder operandsRepr = new StringBuilder();
        int operandSize = getOperandsSizeInBytes();
        for (Integer operand : operands)
        {
            operandsRepr.append(getHexRepresentation(operand, operandSize));
        }

        return (opcodeRepr + operandsRepr).toUpperCase();
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

