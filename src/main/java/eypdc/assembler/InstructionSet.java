package eypdc.assembler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Data
public class InstructionSet {

    private Map<String, Map<String, String>> standard;
    private Map<String, SpecialInstructionInfo> exceptions;

    public static InstructionSet parseFromJson(String filename) throws IOException {
        ClassLoader classLoader = InstructionSet.class.getClassLoader();
        String path = Objects.requireNonNull(classLoader.getResource("instruction_set.json")).getFile();

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(path), InstructionSet.class);
    }

    @Data
    public static class SpecialInstructionInfo {
        private int operands;
        private Map<String, String> addressingModes;
    }
}
