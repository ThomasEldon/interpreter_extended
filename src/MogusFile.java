import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MogusFile {

    private static final Logger reader_file_parsing_logger = LogManager.getLogger("Reader File Parsing");
    private static final Logger reader_history_logger = LogManager.getLogger("Reader History");
    private static final Logger reader_instruction_fetch_logger = LogManager.getLogger("Reader Instruction Fetch");
    InputStream file_scanner;

    ArrayList<Instruction> instruction_history = new ArrayList<>();
    int main_file_cur = 0;
    int instruction_history_checkpoint = -1;

    public MogusFile() {
        File path_absolute = new File("prog");
        try {
            this.file_scanner = new FileInputStream(path_absolute); // FileReader or Scanner could be used by not working with characters
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Instruction read_line(Integer dest_line) {
        Instruction inst_ret = null;
        try {
            if (dest_line == (main_file_cur + 1)) {
                // Line can be read from file!
                inst_ret = read_line_main_file();

                // Line of next instruction to be read into instruction history
                if (instruction_history_checkpoint + instruction_history.size() == dest_line) {
                    // Next instruction being read from file is same as what's needed
                    instruction_history.add(inst_ret);
                }

                main_file_cur += 1;
            } else if (dest_line <= main_file_cur) {
                int offset = dest_line - instruction_history_checkpoint;
                if ((instruction_history.size() > offset) && (offset >= 0)) {
                    // Line requested exists in the history
                    inst_ret = instruction_history.get(offset);
                } else {
                    reader_instruction_fetch_logger.error("Line requested is not in instruction history!");
                }
            } else {
                reader_instruction_fetch_logger.error("Line requested too far in program!");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        reader_instruction_fetch_logger.debug("Returning new inst. Current history: " + this.instruction_history);
        return inst_ret;
    }

    public void set_history(int new_checkpoint, Instruction current_instr) {
        reader_history_logger.debug("Setting history to " + new_checkpoint);
        if (new_checkpoint > (this.instruction_history_checkpoint + this.instruction_history.size())) {
            reader_history_logger.debug("Instruction history doesn't contain future instructions, Clearing..");
            this.instruction_history_checkpoint = new_checkpoint;

            this.instruction_history.clear();
            this.instruction_history.add(current_instr);
        } else {
            reader_history_logger.debug("Ignoring new checkpoint as it overlaps current history");
        }
    }

    // Starting from 1, with 1 being first line
    public Instruction read_line_main_file() throws IOException {

        int current_cmd = 0; //0 is newline, 1 is clear, 2 is while, 3 is end, 4 is incr, 5 is decr
        int segment = 0; //1 is identifier, 2 is NOT (while), 3 is 2nd operand (while), 4 is "do" (while)

        StringBuilder identifier = new StringBuilder();

        while (file_scanner.available() > 0) {
            if (current_cmd == 0) {
                char next = (char) file_scanner.read();
                reader_file_parsing_logger.debug("Start of command, next byte: " + next + "(" + String.format("%02x", (int) next) + ")");
                if (next == 0x0A) {
                    reader_file_parsing_logger.debug("Line feed");
                } else if (next == 0x0D) {
                    reader_file_parsing_logger.debug("Carriage return");
                }

                switch (next) {
                    case 0x63:
                        //letter c (clear)
                        //Advance 5 letters
                        file_scanner.skip(4);
                        current_cmd = 1;
                        break;
                    case 0x77:
                        //letter w (while)
                        file_scanner.skip(4);
                        current_cmd = 2;
                        break;
                    case 0x65:
                        //letter e (end)
                        file_scanner.skip(2);
                        current_cmd = 3;
                        break;
                    case 0x69:
                        //letter i (incr)
                        file_scanner.skip(3);
                        current_cmd = 4;
                        break;
                    case 0x64:
                        //letter d (decr)
                        file_scanner.skip(3);
                        current_cmd = 5;
                        break;
                    default:
                        reader_file_parsing_logger.debug("Invalid character, skipping ...");
                }

                //Reset identifier & segment due to new command
                identifier = new StringBuilder();
                segment = 0;
            } else {
                char cmd_read_byte = (char) file_scanner.read();
                reader_file_parsing_logger.debug("Processing command " + current_cmd + " with next byte: " + cmd_read_byte + "(" + String.format("%02x", (int) cmd_read_byte) + ")" + " on segment: " + segment);

                if (cmd_read_byte == 0x3b) {
                    //End of command, save to be processed
                    return new Instruction(IntprOpcode.values()[current_cmd - 1], identifier.toString());
                } else if (cmd_read_byte == 0x20) {
                    //Mark as next
                    segment += 1;
                } else {
                    switch (current_cmd) {
                        case 1 -> {
                            identifier.append(cmd_read_byte);
                        }
                        case 2 -> {
                            //While, if whitespace skip and mark
                            if (segment == 1) {
                                identifier.append(cmd_read_byte);
                            } else if (segment == 2) {
                                //Skip because probably "NOT"
                                reader_file_parsing_logger.debug("Skipping \"not\" part of while");
                                file_scanner.skip(2);
                            } else if (segment == 3) {
                                //Next chars are probably
                                if (cmd_read_byte == 0x30) {
                                    //Is zero, so second operator is likely zero "0"
                                    reader_file_parsing_logger.debug("valid while loop due to 0");
                                } else {
                                    //give error because something should always be 0 here
                                    reader_file_parsing_logger.debug("Error, should always be zero for second operand of while statement");
                                }
                            } else if (segment == 4) {
                                if (cmd_read_byte == 0x64) {
                                    reader_file_parsing_logger.debug("\"do\" part of while loop, skipping");
                                    file_scanner.skip(1);
                                } else {
                                    reader_file_parsing_logger.debug("syntax error");
                                }
                            }
                        }
                        case 3 -> {
                            //Do nothing
                            reader_file_parsing_logger.debug("...");
                        }
                        case 4 -> {
                            identifier.append(cmd_read_byte);
                        }
                        case 5 -> {
                            identifier.append(cmd_read_byte);
                        }
                    }
                }

                reader_file_parsing_logger.debug("Current identifier: " + identifier);
            }
        }
        return null;
    }
}
