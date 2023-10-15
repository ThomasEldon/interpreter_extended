import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MogusFile {

    private static Logger reader_logger = LogManager.getLogger("MainReader");
    InputStream file_scanner;

    ArrayList<Instruction> instruction_history = new ArrayList<>();
    int main_file_cur = 0;
    int instruction_history_checkpoint = -1;

    public MogusFile() {
        File path_absolute = new File("prog");
        try {
            this.file_scanner = new FileInputStream(path_absolute); //FileReader or Scanner could be used by not working with characters
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Instruction read_line(Integer dest_line) {
        Instruction inst_ret = null;
        try {
            if (dest_line == (main_file_cur + 1)) {
                //next line is going to be correct
                inst_ret = read_line_main_file();

                // line of next instruction to be read into instruction history
                if (instruction_history_checkpoint != -1) {
                    if (instruction_history_checkpoint+instruction_history.size() == dest_line) {
                        //Add instruction being requested to the list as history is on and inst being requested is the same as what's needed
                        instruction_history.add(inst_ret);
                    }
                }

                main_file_cur += 1;
            } else if (dest_line <= main_file_cur) {
                if (instruction_history_checkpoint != -1) {
                    if (instruction_history_checkpoint+instruction_history.size() >= dest_line) {
                        //Line requested exists in the history
                        inst_ret = instruction_history.get(dest_line-instruction_history_checkpoint);
                    } else {
                        System.out.println("impossible error, line too far requested");
                    }
                } else {
                    System.out.println("error, invalid line requested from program");
                }
            } else {
                System.out.println("line requested does not exist in program");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return inst_ret;
    }

    public void set_history(int new_checkpoint, Instruction current_instr) {
        this.instruction_history_checkpoint = new_checkpoint;
        if (main_file_cur == new_checkpoint) {
            System.out.println("added beginner to inst history");
            this.instruction_history.add(current_instr);
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
                reader_logger.debug("Start of command, next byte: " + next + "(" + String.format("%02x", (int) next) + ")");
                if (next == 0x0A) {
                    reader_logger.debug("Line feed");
                } else if (next == 0x0D) {
                    reader_logger.debug("Carriage return");
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
                        reader_logger.debug("Invalid character, skipping ...");
                }

                //Reset identifier & segment due to new command
                identifier = new StringBuilder();
                segment = 0;
            } else {
                char cmd_read_byte = (char) file_scanner.read();
                reader_logger.debug("Processing command " + current_cmd + " with next byte: " + cmd_read_byte + "(" + String.format("%02x", (int) cmd_read_byte) + ")" + " on segment: " + segment);

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
                                reader_logger.debug("Skipping \"not\" part of while");
                                file_scanner.skip(2);
                            } else if (segment == 3) {
                                //Next chars are probably
                                if (cmd_read_byte == 0x30) {
                                    //Is zero, so second operator is likely zero "0"
                                    reader_logger.debug("valid while loop due to 0");
                                } else {
                                    //give error because something should always be 0 here
                                    reader_logger.debug("Error, should always be zero for second operand of while statement");
                                }
                            } else if (segment == 4) {
                                if (cmd_read_byte == 0x64) {
                                    reader_logger.debug("\"do\" part of while loop, skipping");
                                    file_scanner.skip(1);
                                } else {
                                    reader_logger.debug("syntax error");
                                }
                            }
                        }
                        case 3 -> {
                            //Do nothing
                            reader_logger.debug("...");
                        }
                        case 4 -> {
                            identifier.append(cmd_read_byte);
                        }
                        case 5 -> {
                            identifier.append(cmd_read_byte);
                        }
                    }
                }

                reader_logger.debug("Current identifier: " + identifier);
            }
        }
        return null;
    }
}
