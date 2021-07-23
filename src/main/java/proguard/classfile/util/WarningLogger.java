/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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
package proguard.classfile.util;

import org.apache.logging.log4j.Logger;

import java.util.List;

public class WarningLogger extends WarningPrinter
{

    private final Logger logger;
    private int warningCount = 0;

    public WarningLogger(Logger logger)
    {
        super(null);
        this.logger = logger;
    }

    public WarningLogger(Logger logger, List<String> classFilter)
    {
        super(null, classFilter);
        this.logger = logger;
    }

    @Override
    public int getWarningCount()
    {
        return warningCount;
    }

    @Override
    public void print(String className, String message)
    {
        if (accepts(className))
        {
            logger.warn(message);
            warningCount++;
        }
    }

    @Override
    public void print(String className, String className2, String message)
    {
        if (accepts(className, className2))
        {
            logger.warn(message);
            warningCount++;
        }
    }

    @Override
    public void note(String className, String message)
    {
        if (accepts(className))
        {
            logger.info(message);
        }
    }

    @Override
    public void note(String className, String className2, String message)
    {
        if (accepts(className, className2))
        {
            logger.info(message);
        }
    }

}
