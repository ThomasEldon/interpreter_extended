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

    Integer code_index = 0;
    ArrayList<String> variable_identifiers = new ArrayList<>();
    HashMap<String, IntprVariable> variables = new HashMap<>();
    Deque<Integer> while_start_stack = new ArrayDeque<>();


    Integer ignore_while = -1; //The line of occurrence of the current skipped while loop

    MogusFile mogus;

    public static void main(String[] args) {
        System.out.println("Bare Bones Interpreter");

        Main main = new Main();
    }

    public Main() {
        MogusFile prog_mog = new MogusFile();
        mogus = prog_mog;

        while (true) {
            process_command();
        }
    }

    public void process_command() {
        //Increment command index
        code_index += 1;

        Instruction instruction = mogus.read_line(code_index);

        System.out.println("Line: " + code_index + ". Processing command. " + instruction.format_string());

        switch (instruction.opcode) {
            case clear -> {
                if (ignore_while == -1) {
                    modifyVariable(instruction.operand, 0);

                }
            }
            case while_ -> {
                // Push while start index to the stack
                while_start_stack.push(code_index);

                if (ignore_while == -1) {
                    if (variables.get(instruction.operand).value != 0) {
                        if (while_start_stack.size() == 1) {
                            // if this is the only item in the stack, will always be location to call back to
                            mogus.set_history(code_index, instruction);
                        }
                    } else {
                        // Mark current line as the while loop to ignore code starting from
                        ignore_while = code_index;
                    }
                }
            }
            case end -> {
                // run instructions from last while statement, when it returns index is pushed back onto stack
                int line_of_while = while_start_stack.pop();
                if (ignore_while == -1) {
                    code_index = line_of_while - 1;
                } else if (ignore_while == line_of_while) {
                    //Stop ignoring
                    ignore_while = -1;
                }
            }
            case incr -> {
                if (ignore_while == -1) {
                    modifyVariable(instruction.operand, 1);

                }
            }
            case decr -> {
                if (ignore_while == -1) {
                    modifyVariable(instruction.operand, -1);

                }
            }
        }


        //Print all vars
        System.out.println("Current stack: " + while_start_stack);
        displayAllVariables();
    }

    public void modifyVariable(String identifier, Integer amount) {
        if (this.variables.containsKey(identifier)) {
            IntprVariable ass = variables.get(identifier);
            ass.value = (amount == 0) ? 0 : ass.value + amount;
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

class IntprVariable {
    public IntprVariable(int value) {
        this.value = value;
    }

    public int value;
};

class Instruction {
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