package cn.fudan.analysis.cfg;

import org.jf.dexlib2.dexbacked.DexBackedExceptionHandler;
import org.jf.dexlib2.dexbacked.DexBackedTryBlock;
import org.jf.dexlib2.dexbacked.instruction.*;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class CFGAnalysis {

    public static HashSet<BasicBlock> getBasicBlocks(MethodImplementation mp) {
        int firstInstuctionAddr = Integer.MAX_VALUE;
        int lastInstructionAddr = 0;
        BasicBlock entryBB = null;// the entry block of this method
        HashSet<BasicBlock> blocks = new HashSet<BasicBlock>();
        HashSet<BasicBlock> catchBlocks = new HashSet<BasicBlock>();

        // Leader is the first instruction of a basic block.
        ArrayList<Integer> leaders = new ArrayList<Integer>();

        // Since switch-payloads actually are just data, not an instruction.
        // Though dexlib treat them as special
        // instructions(sparse-switch-payload
        // & packed-switch-payload),
        // we will not include them in our CFG.

        // Take the following switch instruction for example.
        // 0xAA switch : switch_payload_0
        // ...
        // 0xBB switch_payload_0:
        // 0x1-> 20(offset)
        // 0x6-> 50(offset)
        // The offset in a payload instruction points to the instruction
        // whose address is relative to the address of the switch opcode(0xAA),
        // not of this table(0xBB).
        // We use the following two maps to store this information when we are
        // finding the leaders.
        // key: payload address
        // value: corresponding switch opcode address.
        HashMap<Integer, Integer> switchPayload2Instruction = new HashMap<Integer, Integer>();

        // key: the start offset of a payload
        // value: an offset list of all the switch branches.
        HashMap<Integer, ArrayList<Integer>> switchPayloadOffsets = new HashMap<Integer, ArrayList<Integer>>();

        Iterable<? extends Instruction> instructions = mp.getInstructions();

        // Whether the next instruction is a leader
        boolean addNext = false;

        for (Instruction i : instructions) {
            DexBackedInstruction dbi = (DexBackedInstruction) i;
            // ATTENTION:
            // Out of this loop, variable addNext is initialized to be false.
            // Through every start instruction of a method must be a leader,
            // we cannot assume that the first instruction retrieved in this
            // for-each loop
            // happen to be the first instruction of this method.
            // Thus, our algorithm is implemented to work even if instructions
            // are not sorted.

            // In fact, dexlib2's instructions are sorted by address,
            // so if addNext is initialized to be true will also be right.

            if (addNext) {
                leaders.add(dbi.instructionStart);
                addNext = false;
            }

            switch (dbi.opcode) {
                case GOTO:
                    leaders.add(((DexBackedInstruction10t) dbi).getCodeOffset() * 2
                            + dbi.instructionStart);
                    addNext = true;
                    break;
                case GOTO_16:
                    leaders.add(((DexBackedInstruction20t) dbi).getCodeOffset() * 2
                            + dbi.instructionStart);
                    addNext = true;
                    break;
                case GOTO_32:
                    leaders.add(((DexBackedInstruction30t) dbi).getCodeOffset() * 2
                            + dbi.instructionStart);
                    addNext = true;
                    break;
                case IF_EQ:
                case IF_NE:
                case IF_LT:
                case IF_GE:
                case IF_GT:
                case IF_LE:
                    leaders.add(((DexBackedInstruction22t) dbi).getCodeOffset() * 2
                            + dbi.instructionStart);
                    addNext = true;
                    break;
                case IF_EQZ:
                case IF_NEZ:
                case IF_LTZ:
                case IF_GEZ:
                case IF_GTZ:
                case IF_LEZ:
                    leaders.add(((DexBackedInstruction21t) dbi).getCodeOffset() * 2
                            + dbi.instructionStart);
                    addNext = true;
                    break;

                case PACKED_SWITCH:
                case SPARSE_SWITCH:
                    switchPayload2Instruction.put(
                            ((DexBackedInstruction31t) dbi).getCodeOffset() * 2
                                    + dbi.instructionStart, dbi.instructionStart);
                    addNext = true;
                    break;
                case PACKED_SWITCH_PAYLOAD:
                case SPARSE_SWITCH_PAYLOAD:
                    int payloadStart = dbi.instructionStart;
                    List<? extends SwitchElement> switchElements = null;
                    if (dbi instanceof DexBackedPackedSwitchPayload) {
                        switchElements = ((DexBackedPackedSwitchPayload) dbi)
                                .getSwitchElements();
                    } else {
                        switchElements = ((DexBackedSparseSwitchPayload) dbi)
                                .getSwitchElements();
                    }

                    // used to link a basic to all of its switch branches
                    ArrayList<Integer> switchBranches = new ArrayList<Integer>();

                    for (SwitchElement s : switchElements) {
                        // according to sparse-switch-payload Format :
                        // The targets are relative to the address of the switch
                        // opcode, not of this table.
                        leaders.add(s.getOffset()
                                * 2
                                + switchPayload2Instruction
                                .get(dbi.instructionStart));
                        switchBranches.add(s.getOffset()
                                * 2
                                + switchPayload2Instruction
                                .get(dbi.instructionStart));
                    }
                    switchPayloadOffsets.put(payloadStart, switchBranches);
                    break;
            }
            if (dbi.instructionStart > lastInstructionAddr) {
                lastInstructionAddr = dbi.instructionStart;
            }
            if (dbi.instructionStart < firstInstuctionAddr) {
                firstInstuctionAddr = dbi.instructionStart;
            }

        }
        // The first instruction is a leader
        leaders.add(firstInstuctionAddr);

        // Till now, we have found all the basic block leaders.
        // Attention:
        // We handle catch blocks as isolated blocks, i.e. there is no other
        // blocks that can lead to catch blocks.
        // Find all catch block leaders.
        HashSet<Integer> catchBlockLeaders = new HashSet<Integer>();
        for (TryBlock<? extends ExceptionHandler> tryBlock : mp.getTryBlocks()) {
            DexBackedTryBlock dbtb = (DexBackedTryBlock) tryBlock;
            for (DexBackedExceptionHandler dbeh : dbtb.getExceptionHandlers()) {
                catchBlockLeaders.add(dbeh.getHandlerCodeAddress() * 2 + firstInstuctionAddr);
            }
        }

        // Build all blocks.
        BasicBlock tempBlock = null;
        for (Instruction i : instructions) {
            // Drop all catch blocks
            if (catchBlockLeaders
                    .contains(((DexBackedInstruction) i).instructionStart)) {
                // Create a new block to "eat" all the following instructions
                // until next block.
                // ATTENTION:
                // Will not add to basicBlocks, because we will not handle catch
                // blocks. :)
                tempBlock = new BasicBlock(null);
                tempBlock
                        .setStartAddress(((DexBackedInstruction) i).instructionStart);
                catchBlocks.add(tempBlock);
            } else if (leaders
                    .contains(((DexBackedInstruction) i).instructionStart)) {
                tempBlock = new BasicBlock(mp);
                tempBlock
                        .setStartAddress(((DexBackedInstruction) i).instructionStart);

                // Find entrance Block
                if (entryBB == null) {
                    entryBB = tempBlock;
                    entryBB.setEntryBlock(true);
                }
                blocks.add(tempBlock);
            }
            tempBlock.addInstruction(i);
        }

        // Link these blocks.
        for (BasicBlock bb : blocks) {
            Instruction lastInstruction = bb.getInstructions().get(
                    bb.getInstructions().size() - 1);
            DexBackedInstruction dbi = (DexBackedInstruction) lastInstruction;

            int offset;
            switch (dbi.opcode) {
                case GOTO:
                    offset = ((DexBackedInstruction10t) dbi).getCodeOffset() * 2
                            + dbi.instructionStart;
                    linkBlock(blocks, bb, offset);
                    break;
                case GOTO_16:
                    offset = ((DexBackedInstruction20t) dbi).getCodeOffset() * 2
                            + dbi.instructionStart;
                    linkBlock(blocks, bb, offset);
                    break;
                case GOTO_32:
                    offset = ((DexBackedInstruction30t) dbi).getCodeOffset() * 2
                            + dbi.instructionStart;
                    linkBlock(blocks, bb, offset);
                    break;
                case IF_EQ:
                case IF_NE:
                case IF_LT:
                case IF_GE:
                case IF_GT:
                case IF_LE:
                    // Link the following basic block.
                    offset = dbi.instructionStart + dbi.getOpcode().format.size;
                    linkBlock(blocks, bb, offset);
                    // Link branch target
                    offset = ((DexBackedInstruction22t) dbi).getCodeOffset() * 2
                            + dbi.instructionStart;
                    linkBlock(blocks, bb, offset);
                    break;
                case IF_EQZ:
                case IF_NEZ:
                case IF_LTZ:
                case IF_GEZ:
                case IF_GTZ:
                case IF_LEZ:
                    // Link the following basic block.
                    offset = dbi.instructionStart + dbi.getOpcode().format.size;
                    linkBlock(blocks, bb, offset);
                    // Link branch target.
                    offset = ((DexBackedInstruction21t) dbi).getCodeOffset() * 2
                            + dbi.instructionStart;
                    linkBlock(blocks, bb, offset);
                    break;

                case PACKED_SWITCH:
                case SPARSE_SWITCH:
                    // link the following basic block
                    offset = dbi.instructionStart + dbi.getOpcode().format.size;
                    linkBlock(blocks, bb, offset);
                    // link all the switch branches
                    for (int o : switchPayloadOffsets
                            .get(((DexBackedInstruction31t) dbi).getCodeOffset()
                                    * 2 + dbi.instructionStart)) {
                        linkBlock(blocks, bb, o);
                    }
                    break;

                // Some special cases:
                // not link the following block

                // returns
                case RETURN_VOID:
                case RETURN:
                case RETURN_WIDE:
                case RETURN_OBJECT:
                case RETURN_VOID_BARRIER:
                    // & throw instruction
                case THROW:
                    // case THROW_VERIFICATION_ERROR:
                    break;

                // default : link the next block
                default:
                    // link the following basic block
                    offset = dbi.instructionStart + dbi.getOpcode().format.size;
                    // we have observed that packed-switch opcode.size = -1;
                    if (dbi.getOpcode().format.size < 0) {
                        break;
                    }

                    // boundary check
                    if (offset > lastInstructionAddr) {
                        break;
                    }
                    linkBlock(blocks, bb, offset);
            }
        }

        return blocks;
    }

    private static void linkBlock(HashSet<BasicBlock> blocks, BasicBlock bb, int offset) {
        for (BasicBlock basicBlock : blocks) {
            if (basicBlock.getStartAddress() == offset) {
                bb.getSuccessors().add(basicBlock);
                basicBlock.getPredecessors().add(bb);
                return;
            }
        }

        // Typically, no exception will be threw.
        throw new RuntimeException("basic block exception: " + offset);
    }
}
