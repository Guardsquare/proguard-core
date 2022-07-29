package proguard.dexfile.reader.node;

import proguard.dexfile.reader.visitors.DexCodeVisitor;
import proguard.dexfile.reader.DexLabel;

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
