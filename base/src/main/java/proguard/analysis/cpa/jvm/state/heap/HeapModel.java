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

package proguard.analysis.cpa.jvm.state.heap;

/**
 * An enumeration of supported heap models.
 *
 * @author Dmitry Ivanov
 */
public enum HeapModel
{
    /**
     * a singleton heap model
      */
    FORGETFUL,
    /**
     * a heap model representing objects as reference graphs
      */
    TREE,
    /**
     * A shallow heap models objects as atomic abstract states.
     */
    SHALLOW,
    /**
     * a taint tree heap model allowing to taint whole objects
      */
    TAINT_TREE
}
