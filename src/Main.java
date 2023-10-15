import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.sql.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;


// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {

    private static Logger logger = LogManager.getLogger(Main.class);
    private static Logger reader_logger = LogManager.getLogger("MainReader");

    Integer code_index = 0;
    ArrayList<String> variable_identifiers = new ArrayList<>();
    HashMap<String, IntprVariable> variables = new HashMap<>();
    Deque<Integer> while_start_stack = new ArrayDeque<>();


    boolean ignore_while = false;

    public static void main(String[] args) {
        System.out.println("Bare Bones Interpreter");

        Main main = new Main();

        File path_absolute = new File("prog");
        try {
            FileInputStream my_reader = new FileInputStream(path_absolute); //FileReader or Scanner could be used by not working with characters

            main.file_reading_test(my_reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void file_reading_test(InputStream file_scanner) throws IOException {
        int current_cmd = 0; //0 is newline, 1 is clear, 2 is while, 3 is end, 4 is incr, 5 is decr
        int segment = 0; //1 is identifier, 2 is NOT (while), 3 is 2nd operand (while), 4 is "do" (while)

        StringBuilder identifier = new StringBuilder();

        while (file_scanner.available() > 0) {
            if (current_cmd==0) {
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
                    process_command(
                            new Instruction(
                                    IntprOpcode.values()[current_cmd-1],
                                    identifier.toString())
                    );

                    //reset current_cmd to move onto next command
                    current_cmd = 0;
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
    }


    public void process_command(Instruction instruction) {
        System.out.println("Processing command, " + instruction.format_string());
        if (!ignore_while) {
            switch (instruction.opcode) {
                case clear -> {
                    modifyVariable(instruction.operand, 0);
                }
                case while_ -> {
                    //ensure true
                    if (variables.get(instruction.operand).value != 0) {
                        //Condition is true
                        this.while_start_stack.push(code_index);
                    } else {
                        //Skip everything until next "end"
                        ignore_while = true;
                    }
                }
                case end -> {
                    //Queue instructions from last while statement
                    while_start_stack.pop();
                    code_index =
                }
                case incr -> {
                    modifyVariable(instruction.operand, 1);
                }
                case decr -> {
                    modifyVariable(instruction.operand, -1);
                }
            }
        } else {
            if (instruction.opcode == IntprOpcode.end) {
                ignore_while = false;
                //carry on as normal...
            }
        }


        //Increment command index
        code_index += 1;

        //Print all vars
        displayAllVariables();
    }

    public void modifyVariable(String identifier, Integer amount) {
        if (this.variables.containsKey(identifier)) {
            IntprVariable ass = variables.get(identifier);
            ass.value = (amount == 0) ? 0 : ass.value+amount;
        } else {
            variable_identifiers.add(identifier);
            variables.put(identifier, new IntprVariable(amount));
        }
    }

    public void displayAllVariables() {
        for (String variable_name : variable_identifiers) {
            System.out.println("Var: " + variable_name
                    + ". Val: " + this.variables.get(variable_name).value);
        }
    }
}

class IntprVariable
{
    public IntprVariable(int value) {
        this.value = value;
    }
    public int value;
};

class Instruction{
    IntprOpcode opcode;
    String operand;
    public Instruction(IntprOpcode opcode, String operand) {
        this.opcode = opcode;
        this.operand = operand;
    }

    public String format_string() {
        return "Opcode: " + this.opcode.name() + ". Operand: " + this.operand;
    }
}