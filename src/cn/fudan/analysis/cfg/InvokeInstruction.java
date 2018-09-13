package cn.fudan.analysis.cfg;

import org.jf.dexlib2.dexbacked.instruction.DexBackedInstruction;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.util.ReferenceUtil;

public class InvokeInstruction {
    public final BasicBlock basicBlock;
    public final Instruction instruction;
    /**
     * The instruction index of this inside its basic block.
     */
    public final int index;

    public InvokeInstruction(BasicBlock basicBlock, Instruction i,int index) {
        this.basicBlock = basicBlock;
        this.instruction = i;
        this.index = index;
    }

    /**
     * Get the corresponding callee of this invoke instruction. The result is a
     * string representation, not a method reference, for the callee can be an
     * api or any method implemented by this dex file.
     *
     * Example of return value:
     * 		Ljava/lang/Runnable;->run()V
     *
     * @return The callee of this instruction, represented by a string.
     */
    public String getCallee() {
        StringBuilder s = new StringBuilder();
        Reference reference = ((ReferenceInstruction) instruction)
                .getReference();
        s.append(ReferenceUtil.getReferenceString(reference));

        return s.toString();
    }

    public int getStartAddress(){
        return ((DexBackedInstruction)this.instruction).instructionStart;
    }
}
