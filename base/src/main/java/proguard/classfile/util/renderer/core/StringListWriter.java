package proguard.classfile.util.renderer.core;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class StringListWriter extends Writer
{
    private StringBuilder      currentLine;
    private final List<String> output;
    public StringListWriter(List<String> output)
    {
        this.output = output;
        currentLine = new StringBuilder();
    }

    @Override
    public void write(@NotNull char[] cbuf, int off, int len) throws IOException
    {
        if ((off < 0) || (off > cbuf.length) || (len < 0) ||
            ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        currentLine.append(cbuf, off, len);
    }

    @Override
    public void write(String str) throws IOException {
        if (System.lineSeparator().equals(str))
        {
            newLine();
        }
        else
        {
            String[] segments = str.split(System.lineSeparator());
            for (String segment: segments)
            {
                write(segment, 0, segment.length());
                newLine();
            }
        }
    }

    private void newLine()
    {
        output.add(currentLine.toString());
        currentLine = new StringBuilder();
    }

    public List<String> getOutput()
    {
        return output;
    }

    public void flush() throws IOException
    {
        newLine();
    }

    public void close() throws IOException
    {
        flush();
    }
}
