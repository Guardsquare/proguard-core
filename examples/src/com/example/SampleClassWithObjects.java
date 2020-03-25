package com.example;

/**
 * This is a sample class that can be processed.
 */
public class SampleClassWithObjects
{
    public static Number getAnswer(Number answer)
    {
        if (answer == null)
        {
            answer = new Integer(42);
        }

        return answer;
    }
}
