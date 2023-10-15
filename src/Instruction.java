public class Instruction {
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