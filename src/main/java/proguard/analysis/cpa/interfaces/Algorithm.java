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

/**
 * This interface wraps the main CPA algorithm parametrized by the {@link ReachedSet} and the {@link Waitlist}.
 *
 * @author Dmitry Ivanov
 */
public interface Algorithm
{

    /**
     * Launches the algorithm updating the {@code reachedSet} and the {@code waitlist}.
     * A proper selection of parameters allows resuming the algorithm from a saved state.
     */
    void run(ReachedSet reachedSet, Waitlist waitlist);
}
