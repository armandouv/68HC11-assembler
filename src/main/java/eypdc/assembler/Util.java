package eypdc.assembler;

public class Util
{
    public static String toHexString(Integer value)
    {
        if (value < 0) throw new RuntimeException("Negative values are not supported");
        int hexDigits = Integer.toHexString(value).length();
        int sizeInBytes = hexDigits % 2 == 0 ? hexDigits / 2 : hexDigits / 2 + 1;
        return String.format("%0" + (sizeInBytes * 2) + "x", value).toUpperCase();
    }

    public static int getMaxValueFromBytes(int sizeInBytes)
    {
        return getMaxValueFromBits(8 * sizeInBytes);
    }

    public static int getMaxValueFromBits(int sizeInBits)
    {
        return (int) (Math.pow(2, sizeInBits) - 1);
    }

    public static String getHexRepresentation(Integer value, int sizeInBytes)
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
}
