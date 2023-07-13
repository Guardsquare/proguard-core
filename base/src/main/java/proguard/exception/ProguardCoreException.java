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

package proguard.exception;

public class ProguardCoreException extends RuntimeException
{

    /**
     * A unique id identifying the error (or group of errors).
     */
    private final int componentErrorId;

    /**
     * Information related to the error.
     */
    private final String[] errorParameters;


    /**
     * Overload of {@link ProguardCoreException#ProguardCoreException(int, Throwable, String, String...)}} without throwable.
     */
    public ProguardCoreException(int componentErrorId, String message,  String... errorParameters)
    {
        this(componentErrorId, null, message, errorParameters);
    }

    /**
     * <b>Main constructor, all other constructors need to call this one in order to do common things (formating string for instance).</b>
     * Same as {@link ProguardCoreException#ProguardCoreException(int, String,  String...)}
     * but takes a Throwable argument to initialize the cause.
     */
    public ProguardCoreException(int componentErrorId, Throwable cause, String message, String... errorParameters)
    {
        super(String.format(message, (Object[]) errorParameters), cause);

        this.componentErrorId = componentErrorId;
        this.errorParameters  = errorParameters;
    }



    /**
     * Returns the id for the error (exception).
     */
    public int getComponentErrorId()
    {
        return componentErrorId;
    }


    /**
     * Returns the list of information related to this error.
     */
    public String[] getErrorParameters()
    {
        return errorParameters;
    }
}
