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

package proguard.util.kotlin.asserter.constraint;

import java.util.List;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinFileFacadeKindMetadata;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.util.ClassUtil;
import proguard.resources.file.ResourceFile;
import proguard.resources.file.visitor.ResourceFileVisitor;
import proguard.resources.kotlinmodule.KotlinModule;
import proguard.resources.kotlinmodule.visitor.KotlinModuleVisitor;
import proguard.util.kotlin.asserter.AssertUtil;
import proguard.util.kotlin.asserter.Reporter;

/**
 * @author James Hamilton
 */
public class KotlinModuleIntegrity
implements   KotlinAsserterConstraint,
             KotlinModuleVisitor,
             ResourceFileVisitor
{
    private Reporter reporter;

    @Override
    public void check(Reporter       reporter,
                      ClassPool      programClassPool,
                      ClassPool      libraryClassPool,
                      Clazz          clazz,
                      KotlinMetadata kotlinMetadata) { }

    @Override
    public void check(Reporter reporter, KotlinModule kotlinModule)
    {
        this.reporter = reporter;
        kotlinModule.accept(this);
    }


    @Override
    public void visitKotlinModule(KotlinModule kotlinModule)
    {
        AssertUtil util = new AssertUtil("Kotlin module", reporter, new ClassPool(), new ClassPool());

        util.reportIfNull("Module name", kotlinModule.name);

        if (!kotlinModule.fileName.startsWith("META-INF/"))
        {
            reporter.report("Module should be in the META-INF folder");
        }

        if (!kotlinModule.fileName.endsWith(".kotlin_module"))
        {
            reporter.report("Module file name extension should be .kotlin_module");
        }

        if (!kotlinModule.fileName.equals("META-INF/" + kotlinModule.name + ".kotlin_module"))
        {
            reporter.report("Module name does not match filename: \"" + kotlinModule.fileName + "\" != \"META-INF/" + kotlinModule.name + ".kotlin_module\"");
        }

        kotlinModule.modulePackagesAccept((_kotlinModule, kotlinModulePart) -> {
            util.setParentElement("Kotlin module part '" + ClassUtil.externalClassName(kotlinModulePart.fqName) + "'");

            util.reportIfNull("fqName", kotlinModulePart.fqName);

            if (kotlinModulePart.fileFacadeNames.size() != kotlinModulePart.referencedFileFacades.size())
            {
                reporter.report("Mismatch between file facade names and references: " + kotlinModulePart.fileFacadeNames.size() + " != " + kotlinModulePart.referencedFileFacades.size());
            }

            if (kotlinModulePart.multiFileClassParts.size() != kotlinModulePart.referencedMultiFileParts.size())
            {
                reporter.report("Mismatch between multi-file class parts and references: " + kotlinModulePart.multiFileClassParts.size() + " != " + kotlinModulePart.referencedMultiFileParts.size());
            }

            List<KotlinFileFacadeKindMetadata> referencedFileFacades = kotlinModulePart.referencedFileFacades;
            for (int i = 0; i < referencedFileFacades.size(); i++)
            {
                KotlinFileFacadeKindMetadata ff = referencedFileFacades.get(i);
                String ffName;
                try
                {
                    ffName = ClassUtil.externalClassName(kotlinModulePart.fileFacadeNames.get(i));
                }
                catch (IndexOutOfBoundsException ignored)
                {
                    ffName = "unknown file facade";
                }

                util.reportIfNull("referenced file facade for '" + ffName + "'", ff);

                if (ff != null)
                {
                    util.reportIfNull("referenced file facade owner reference for '" + ffName + "'", ff.ownerReferencedClass);
                }
            }

            kotlinModulePart.referencedMultiFileParts
                .forEach((mfpName, mfp) -> {
                    util.reportIfNull("referenced multi-file part for '" + mfpName + "'", mfp);
                    if (mfp != null)
                    {
                        util.reportIfNull("referenced multi-file part for '" + mfpName + "'", mfp.ownerReferencedClass);
                    }
                });
        });
    }

    @Override
    public void visitResourceFile(ResourceFile resourceFile) {}

}
