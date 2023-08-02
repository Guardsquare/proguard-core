package proguard.evaluation.util.jsonprinter;

interface JsonSerializable
{
    StringBuilder toJson(StringBuilder builder);
}
