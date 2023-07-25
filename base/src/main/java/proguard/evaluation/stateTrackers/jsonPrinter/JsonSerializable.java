package proguard.evaluation.stateTrackers.jsonPrinter;

interface JsonSerializable
{
    StringBuilder toJson(StringBuilder builder);
}
