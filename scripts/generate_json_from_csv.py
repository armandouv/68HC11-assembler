import json
import os.path

BASE_PATH = os.path.join(os.getcwd(), "../src/main/resources")
EXCEPTIONS_FILE_PATH = os.path.join(BASE_PATH, "exceptions.json")
INSTRUCTION_SET_CSV_FILE_PATH = os.path.join(BASE_PATH, "instruction_set.csv")
INSTRUCTION_SET_JSON_FILE_PATH = os.path.join(BASE_PATH, "instruction_set.json")
ADDRESSING_MODES = ["IMM", "DIR", "IND,X", "IND,Y", "EXT", "INH", "REL"]

with open(EXCEPTIONS_FILE_PATH, "r") as f:
    EXCEPTIONS = json.load(f)


def parse_csv(file_path):
    standard = {}

    with open(file_path, "r") as csv_file:
        for line in csv_file:
            split = line.strip().split(",")
            current_mnemonic = split[0]
            if current_mnemonic in EXCEPTIONS:
                continue

            current_addressing_modes = {}
            for i in range(1, len(split)):
                current_opcode = split[i]
                if current_opcode != '':
                    current_addressing_modes[ADDRESSING_MODES[i - 1]] = current_opcode
            standard[current_mnemonic] = current_addressing_modes

    instruction_set = {
        "standard": standard,
        "exceptions": EXCEPTIONS
    }

    with open(INSTRUCTION_SET_JSON_FILE_PATH, "w") as json_file:
        json.dump(instruction_set, json_file)


if __name__ == "__main__":
    parse_csv(INSTRUCTION_SET_CSV_FILE_PATH)
