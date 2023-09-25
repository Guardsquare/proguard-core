/*
 * Copyright (c) 2009-2012 Panxiaobo
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
package proguard.dexfile.reader;

/**
 * a light weight version of org.objectweb.asm.Label
 *
 * @author Panxiaobo
 * @version $Rev$
 */
public class DexLabel {
    private final String displayName;

    /**
     * @param offset the offset of the label
     */
    public DexLabel(int offset) {
        this.displayName = "L" + offset;
    }

    public DexLabel() {
        this.displayName = "L" + this.hashCode();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
