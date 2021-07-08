package eypdc.assembler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Data
public class InstructionSet
{
    private final Set<String> specialDirectives = Set.of("ORG", "EQU", "FCB", "END");
    private Map<String, Map<String, String>> standard;
    private Map<String, SpecialInstructionInfo> exceptions;

    public static InstructionSet parseFromJson()
    {
        ClassLoader classLoader = InstructionSet.class.getClassLoader();
        String path = Objects.requireNonNull(classLoader.getResource("instruction_set.json")).getFile();

        ObjectMapper objectMapper = new ObjectMapper();
        try
        {
            return objectMapper.readValue(new File(path), InstructionSet.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not open instruction set file", e);
        }
    }

    public boolean containsMnemonic(String mnemonic)
    {
        return (standard.containsKey(mnemonic) || exceptions.containsKey(mnemonic));
    }

    public boolean containsSpecialDirective(String directive)
    {
        return specialDirectives.contains(directive);
    }

    @Data
    public static class SpecialInstructionInfo
    {
        private int operands;
        private Map<String, String> addressingModes;
    }
}
