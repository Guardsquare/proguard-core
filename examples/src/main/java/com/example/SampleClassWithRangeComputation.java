package com.example;

/**
 * This is a sample class that can be processed.
 */
public class SampleClassWithRangeComputation
{
    public static int getAnswer()
    {
        int answer = 0;
        for (int counter = 0; counter < 3 && Math.random() < 0.5f; counter++)
        {
            answer += 14;
        }

        return answer;
    }
}
