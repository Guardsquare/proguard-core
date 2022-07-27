/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package proguard.analysis.cpa.jvm.state;

import java.util.List;
import java.util.Objects;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.ListAbstractState;
import proguard.analysis.cpa.defaults.StackAbstractState;

/**
 * The {@link JvmFrameAbstractState} combines the operand stack as the {@link StackAbstractState} and the local variable array as the {@link ListAbstractState}.
 * This abstract state does not restrict the way one models values, i.e., one abstract state may correspond to a byte sequence of arbitrary length.
 *
 * @author Dmitry Ivanov
 */
public class JvmFrameAbstractState<StateT extends LatticeAbstractState<StateT>>
    implements LatticeAbstractState<JvmFrameAbstractState<StateT>>
{

    protected final ListAbstractState<StateT>  localVariables;
    protected final StackAbstractState<StateT> operandStack;

    /**
     * Create an empty frame.
     */
    public JvmFrameAbstractState()
    {
        this(new ListAbstractState<>(), new StackAbstractState<>());
    }

    /**
     * Create a frame from a local variable array and an operand stack.
     *
     * @param localVariables a local variable array
     * @param operandStack   an operand stack
     */
    public JvmFrameAbstractState(ListAbstractState<StateT> localVariables, StackAbstractState<StateT> operandStack)
    {
        this.localVariables = localVariables;
        this.operandStack = operandStack;
    }

    // implementations for LatticeAbstractState

    @Override
    public JvmFrameAbstractState<StateT> join(JvmFrameAbstractState<StateT> abstractState)
    {
        JvmFrameAbstractState<StateT> answer = new JvmFrameAbstractState<>(localVariables.join(abstractState.localVariables),
                                                                           operandStack.join(abstractState.operandStack));
        return equals(answer) ? this : answer;
    }

    @Override
    public boolean isLessOrEqual(JvmFrameAbstractState<StateT> abstractState)
    {
        return localVariables.isLessOrEqual(abstractState.localVariables) && operandStack.isLessOrEqual(abstractState.operandStack);
    }

    // implementations for AbstractState

    @Override
    public JvmFrameAbstractState<StateT> copy()
    {
        return new JvmFrameAbstractState<>((ListAbstractState<StateT>) localVariables.copy(), (StackAbstractState<StateT>) operandStack.copy());
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof JvmFrameAbstractState))
        {
            return false;
        }
        JvmFrameAbstractState<StateT> other = (JvmFrameAbstractState) obj;
        return localVariables.equals(other.localVariables) && operandStack.equals(other.operandStack);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(localVariables, operandStack);
    }

    /**
     * Returns the {@code index}th element from the top of the operand stack or returns {@code defaultState} if the stack does not have enough elements.
     */
    public StateT peekOrDefault(int index, StateT defaultState)
    {
        return operandStack.peekOrDefault(index, defaultState);
    }

    /**
     * Returns the {@code index}th element from the top of the operand stack.
     */
    public StateT peek(int index)
    {
        return operandStack.peek(index);
    }

    /**
     * Removes the top element of the operand stack end returns it.
     */
    public StateT pop()
    {
        return operandStack.pop();
    }

    /**
     * Removes the top element of the operand stack end returns it. Returns {@code defaultState} if the stack is empty.
     */
    public StateT popOrDefault(StateT defaultState)
    {
        return operandStack.popOrDefault(defaultState);
    }

    /**
     * Inserts {@code state} to the top of the operand stack and returns it.
     */
    public StateT push(StateT state)
    {
        return operandStack.push(state);
    }

    /**
     * Sequentially inserts elements of {@code states} to the top of the operand stack and returns {@code states}.
     */
    public List<StateT> pushAll(List<StateT> states)
    {
        states.forEach(operandStack::push);
        return states;
    }

    /**
     * Returns an abstract state at the {@code index}th position of the variable array or {@code defaultState} if there is no entry.
     */
    public StateT getVariableOrDefault(int index, StateT defaultState)
    {
        return localVariables.getOrDefault(index, defaultState);
    }

    /**
     * Sets the {@code index}th position of the variable array to {@code state} and returns {@code state}.
     * If the array has to be extended, the added cells are padded with {@code defaultState}.
     */
    public StateT setVariable(int index, StateT state, StateT defaultState)
    {
        return localVariables.set(index, state, defaultState);
    }

    /**
     * Returns the variable array.
     */
    public ListAbstractState<StateT>  getLocalVariables()
    {
        return localVariables;
    }

    /**
     * Returns the operand stack.
     */
    public StackAbstractState<StateT> getOperandStack()
    {
        return operandStack;
    }
}
