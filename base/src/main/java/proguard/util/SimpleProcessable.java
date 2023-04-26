/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
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
package proguard.util;

import java.util.Arrays;

/**
 * This class provides a straightforward implementation of the Processable
 * interface.
 *
 * @author Eric Lafortune
 */
public class SimpleProcessable
implements   Processable
{
    public int    processingFlags;
    public Object processingInfo;


    /**
     * Creates an uninitialized SimpleProcessable.
     */
    public SimpleProcessable() {}


    /**
     * Creates an initialized SimpleProcessable.
     */
    public SimpleProcessable(int    processingFlags,
                             Object processingInfo)
    {
        this.processingFlags = processingFlags;
        this.processingInfo  = processingInfo;
    }

    
    // Implementations for Processable.

    @Override
    public int getProcessingFlags()
    {
        return processingFlags;
    }


    @Override
    public void setProcessingFlags(int processingFlags)
    {
        this.processingFlags = processingFlags;
    }


    @Override
    public Object getProcessingInfo()
    {
        return processingInfo;
    }


    @Override
    public void setProcessingInfo(Object processingInfo) {
        if (System.getProperty("proguard.processinginfo.check_overriding") != null)
        {
            if (this.processingInfo != null && processingInfo != null && !this.processingInfo.getClass().getName().equals(processingInfo.getClass().getName()))
            {
                // get the second (top down) element of the stack trace (the second is setProcessingInfo call)
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                StackTraceElement stackTraceElement = stackTrace.length > 2 ? stackTrace[2] : null;

                System.out.printf("Overriding processingInfo. Old:%s, New:%s. Stacktrace entry before last is:\n\t%s%n",
                        this.processingInfo.getClass().getName(),
                        processingInfo.getClass().getName(),
                        stackTraceElement
                );
            }
        }
        this.processingInfo = processingInfo;
    }
}
