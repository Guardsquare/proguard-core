package proguard.util;

/**
 * A circular buffer over the primitive integer type.
 */
public class CircularIntBuffer
{
    private final int[] buffer;
    private int head;
    private int curSize;

    /**
     * Create a new CircularIntBuffer that can grow to a given maxSize.
     */
    public CircularIntBuffer(int maxSize)
    {
        buffer = new int[maxSize];
        head = 0;
    }

    /**
     * Push a value into the buffer overriding the oldest element when exceeding max size.
     */
    public void push(int value)
    {
        head = (head + 1) % buffer.length;
        buffer[head] = value;
        if (curSize < buffer.length)
        {
            curSize++;
        }
    }

    /**
     * Get the element at an offset of the head.
     */
    public int peek(int offset)
    {
        if (offset >= curSize)
        {
            throw new IndexOutOfBoundsException("Offset ["+offset+"] out of bounds for buffer of size "+curSize+".");
        }
        return buffer[Math.floorMod(head - offset, buffer.length)];
    }

    /**
     * Get the head value.
     */
    public int peek()
    {
        return peek(0);
    }

    /**
     * Get current size of the buffer.
     */
    public int size()
    {
        return curSize;
    }
}
