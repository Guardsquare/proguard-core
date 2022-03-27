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

package proguard.analysis.cpa.domain.arg;

import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;

/**
 * This CPA wraps a given CPA with abstract reachability graph (ARG) generation.
 * The ARG tracks the way {@link AbstractState}s
 * are computed. Its nodes represented by {@link ArgAbstractState}s wrap the traced analysis
 * states and references their parents, i.e., {@link AbstractState}
 * which may have influenced their value. ARG is constructed by introducing wrapper CPA operators
 * handling the parent lists while being transparent for the traced analysis through
 * interface delegation. Thus, the traced analysis can be ARG-unaware if it preserves
 * some degree of transparency, i.e., all its relevant behavior is defined through its CPA operators.
 *
 * @author Dmitry Ivanov
 */
public class ArgCpa
    extends SimpleCpa
{

    /**
     * Create an ARG CPA for the given inner CPA.
     *
     * @param wrappedCpa              a CPA to be wrapped
     * @param argAbstractStateFactory an ARG node factory
     */
    public ArgCpa(ConfigurableProgramAnalysis wrappedCpa, ArgAbstractStateFactory argAbstractStateFactory)
    {
        this(new ArgAbstractDomain(wrappedCpa.getAbstractDomain(), argAbstractStateFactory),
             new ArgTransferRelation(wrappedCpa.getTransferRelation(), argAbstractStateFactory),
             new ArgMergeOperator(wrappedCpa.getMergeOperator(), argAbstractStateFactory),
             new ArgStopOperator(wrappedCpa.getStopOperator()),
             new ArgPrecisionAdjustment(wrappedCpa.getPrecisionAdjustment(), argAbstractStateFactory));
    }

    /**
     * Create an ARG CPA for the given CPA components.
     *
     * @param argAbstractDomain      an ARG abstract domain
     * @param argTransferRelation    an ARG transfer relation
     * @param argMergeOperator       an ARG merge operator
     * @param argStopOperator        an ARG stop operator
     * @param argPrecisionAdjustment an ARG precision adjustment
     */
    public ArgCpa(ArgAbstractDomain argAbstractDomain,
                  ArgTransferRelation argTransferRelation,
                  ArgMergeOperator argMergeOperator,
                  ArgStopOperator argStopOperator,
                  ArgPrecisionAdjustment argPrecisionAdjustment)
    {
        super(argAbstractDomain,
              argTransferRelation,
              argMergeOperator,
              argStopOperator,
              argPrecisionAdjustment);
    }
}
