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

package proguard.analysis.cpa.interfaces;

import proguard.classfile.Signature;

/**
 * If an {@link AbstractState} is program location-specific, it should implement {@link ProgramLocationDependent}.
 *
 * @author Dmitry Ivanov
 */
public interface ProgramLocationDependent<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
{

    /**
     * Returns the program location.
     */
    CfaNodeT getProgramLocation();

    /**
     * Sets the program location.
     */
    void setProgramLocation(CfaNodeT programLocation);
}
