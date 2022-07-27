package proguard.dexfile.reader.api.node;

import proguard.dexfile.reader.api.visitors.DexCodeVisitor;
import proguard.dexfile.reader.api.DexLabel;

public class TryCatchNode {

    public final DexLabel start;
    public final DexLabel end;
    public final DexLabel[] handler;
    public final String[] type;

    public TryCatchNode(DexLabel start, DexLabel end, DexLabel[] handler, String[] type) {
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.type = type;
    }

    public void accept(DexCodeVisitor cv) {
        cv.visitTryCatch(start, end, handler, type);
    }
}
