package dexir.ts;

import dexir.IrMethod;

public abstract class StatedTransformer implements Transformer {
    public abstract boolean transformReportChanged(IrMethod method);

    @Override
    public void transform(IrMethod method) {
        transformReportChanged(method);
    }
}
