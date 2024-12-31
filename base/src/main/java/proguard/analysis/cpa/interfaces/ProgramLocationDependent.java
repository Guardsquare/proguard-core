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

import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;

/**
 * If an {@link AbstractState} is program location-specific (i.e., is associated to a specific node
 * from the {@link proguard.analysis.cpa.defaults.Cfa}), it should implement {@link
 * ProgramLocationDependent}.
 */
public interface ProgramLocationDependent {

  /** Returns the program location. */
  JvmCfaNode getProgramLocation();

  /** Sets the program location. */
  void setProgramLocation(JvmCfaNode programLocation);
}
