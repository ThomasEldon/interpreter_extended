import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;


// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static final Logger logger_interpreter_main = LogManager.getLogger("Interpreter Main");

    Integer code_index = 0;
    ArrayList<String> variable_identifiers = new ArrayList<>();
    HashMap<String, InterpreterVariable> variables = new HashMap<>();
    Deque<Integer> while_start_stack = new ArrayDeque<>();

    Integer ignore_while = -1; //The line of occurrence of the current skipped while loop

    InstructionReader mogus;

    public static void main(String[] args) {
        logger.info("Bare Bones Interpreter");
        Main main = new Main();
        //GuiTest test = new GuiTest();
    }

    public Main() {
        InstructionReader prog_mog = new InstructionReader();
        mogus = prog_mog;

        while (process_command());
    }

    private boolean process_command() {
        //Increment command index
        code_index += 1;

        Instruction instruction = mogus.read_line(code_index);

        if (instruction == null) {
            logger_interpreter_main.info("No more instructions. Quitting");
            return false;
        }

        logger_interpreter_main.debug("Line: " + code_index + ". Processing command. " + instruction.format_string());

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
        logger_interpreter_main.debug("Current while stack: " + while_start_stack);
        displayAllVariables();

        return true;
    }

    private void modifyVariable(String identifier, Integer amount) {
        if (this.variables.containsKey(identifier)) {
            InterpreterVariable ass = variables.get(identifier);
            ass.value = (amount == 0) ? 0 : ass.value + amount;
        } else {
            variable_identifiers.add(identifier);
            variables.put(identifier, new InterpreterVariable(amount));
        }
    }

    private void displayAllVariables() {
        for (String variable_name : variable_identifiers) {
            logger_interpreter_main.debug("Var: " + variable_name
                    + ". Val: " + this.variables.get(variable_name).value);
        }
    }
}

class InterpreterVariable {
    public InterpreterVariable(int value) {
        this.value = value;
    }

    public int value;
};