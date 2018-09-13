package cn.fudan.analysis.cfg;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

public class BasicBlock {
    private MethodImplementation mp;
    private ArrayList<Instruction> instructions = new ArrayList<Instruction>();
    private LinkedList<BasicBlock> successors = new LinkedList <BasicBlock>();
    private LinkedList<BasicBlock> predecessors = new LinkedList<BasicBlock>();
    private LinkedList<InvokeInstruction> invokeInstructions = null;

    private int startAddress;
    private boolean isEntry = false;

    public BasicBlock(MethodImplementation mp){
        this.mp = mp;
    }

    public LinkedList<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public LinkedList<BasicBlock> getSuccessors() {
        return successors;
    }

    public boolean isEntryBlock() {
        return isEntry;
    }

    public void setEntryBlock(boolean isEntry) {
        this.isEntry = isEntry;
    }

    public void setStartAddress(int startAddress) {
        this.startAddress = startAddress;
    }

    public int getStartAddress() {
        return startAddress;
    }

    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }

    public void addInstruction(Instruction i){
        this.instructions.add(i);
    }

    public LinkedList<InvokeInstruction> getInvokeInstructions(){
        if(this.invokeInstructions != null){
            return this.invokeInstructions;
        }
        this.invokeInstructions = new LinkedList<InvokeInstruction>();
        Instruction instruction;
        Opcode opcode;
        for (int i = 0; i < this.instructions.size(); i++) {
            instruction = this.instructions.get(i);
            opcode = instruction.getOpcode();

            switch(opcode){
                case INVOKE_VIRTUAL:
                case INVOKE_SUPER:
                case INVOKE_DIRECT:
                case INVOKE_STATIC:
                case INVOKE_INTERFACE:
                case INVOKE_VIRTUAL_RANGE:
                case INVOKE_SUPER_RANGE:
                case INVOKE_DIRECT_RANGE:
                case INVOKE_STATIC_RANGE:
                case INVOKE_INTERFACE_RANGE:
                    InvokeInstruction ii = new InvokeInstruction(this,instruction, i);
                    invokeInstructions.add(ii);
            }
        }

        return invokeInstructions;
    }

    /**
     *
     * @return A set S. S = all basic blocks that for each of them  there is a cfg path  start from it and pass through
     * this block. S DOES NOT contains this block itself!
     */
    public HashSet<BasicBlock> getSuccessorSet(){
        HashSet<BasicBlock> result = new HashSet<BasicBlock>();
        HashSet<BasicBlock> visited = new HashSet<BasicBlock>();
        LinkedList<BasicBlock> testQueue = new LinkedList<BasicBlock>();
        testQueue.push(this);
        while (testQueue.size() > 0) {
            BasicBlock basicBlock = testQueue.pop();
            visited.add(basicBlock);
            for (BasicBlock successor : basicBlock.getSuccessors()) {
                if (visited.contains(successor)) {
                    continue;
                }else {
                    testQueue.push(successor);
                }
            }
            result.add(basicBlock);
        }
        return result;
    }
}

