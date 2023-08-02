/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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

package proguard.evaluation.util.jsonprinter;

import java.util.ArrayList;
import java.util.List;

/**
 * Track the state of a partial evaluator instance.
 */
class StateTracker
{
    private final List<CodeAttributeRecord> codeAttributes = new ArrayList<>();

    public CodeAttributeRecord getLastCodeAttribute()
    {
        if (codeAttributes.isEmpty())
        {
            return null;
        }
        return codeAttributes.get(codeAttributes.size() - 1);
    }

    public List<CodeAttributeRecord> getCodeAttributes()
    {
        return codeAttributes;
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        return JsonPrinter.listToJson("codeAttributes", codeAttributes, builder).append("}").toString();
    }
}
