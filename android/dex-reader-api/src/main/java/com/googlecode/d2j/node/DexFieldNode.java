/*
 * dex2jar - Tools to work with android .dex and java .class files
 * Copyright (c) 2009-2013 Panxiaobo
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
package com.googlecode.d2j.node;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.d2j.Field;
import com.googlecode.d2j.Visibility;
import com.googlecode.d2j.visitors.DexAnnotationVisitor;
import com.googlecode.d2j.visitors.DexClassVisitor;
import com.googlecode.d2j.visitors.DexFieldVisitor;

/**
 * @author Bob Pan
 * 
 */
public class DexFieldNode extends DexFieldVisitor {
    public int access;
    public List<DexAnnotationNode> anns;
    public Object cst;
    public Field field;

    public DexFieldNode(DexFieldVisitor visitor, int access, Field field, Object cst) {
        super(visitor);
        this.access = access;
        this.field = field;
        this.cst = cst;
    }

    public DexFieldNode(int access, Field field, Object cst) {
        super();
        this.access = access;
        this.field = field;
        this.cst = cst;
    }

    public void accept(DexClassVisitor dcv) {
        DexFieldVisitor fv = dcv.visitField(access, field, cst);
        if (fv != null) {
            accept(fv);
            fv.visitEnd();
        }
    }

    public void accept(DexFieldVisitor fv) {
        if (anns != null) {
            for (DexAnnotationNode ann : anns) {
                ann.accept(fv);
            }
        }
    }

    @Override
    public DexAnnotationVisitor visitAnnotation(String name, Visibility visibility) {
        if (anns == null) {
            anns = new ArrayList<DexAnnotationNode>(5);
        }
        DexAnnotationNode annotation = new DexAnnotationNode(name, visibility);
        anns.add(annotation);
        return annotation;
    }
}
