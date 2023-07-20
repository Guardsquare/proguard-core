package proguard.evaluation.stateTrackers.machinePrinter;

import java.util.ArrayList;
import java.util.List;

/**
 * Track the state of a partial evaluator instance
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
}
