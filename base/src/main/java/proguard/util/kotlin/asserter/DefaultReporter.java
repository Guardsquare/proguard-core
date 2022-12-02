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

package proguard.util.kotlin.asserter;

import proguard.classfile.util.WarningPrinter;

/**
 * @author James Hamilton
 */
class DefaultReporter implements Reporter
{
    private final WarningPrinter warningPrinter;
    private       int            count;
    private       String         errorMessage = "";
    private       String         contextName  = "";


    DefaultReporter(WarningPrinter warningPrinter)
    {
        this.warningPrinter = warningPrinter;
        count = 0;
    }


    @Override
    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }


    @Override
    public void report(String error)
    {
        if (count == 0)
        {
            warningPrinter.print(this.contextName, String.format(this.errorMessage, this.contextName));
        }
        count++;
        warningPrinter.print(this.contextName, "  " + error);
    }


    @Override
    public void resetCounter(String contextName)
    {
        this.contextName = contextName;
        this.count       = 0;
    }


    @Override
    public int getCount()
    {
        return count;
    }


    @Override
    public void print(String className, String s)
    {
        this.warningPrinter.print(className, s);
    }
}
