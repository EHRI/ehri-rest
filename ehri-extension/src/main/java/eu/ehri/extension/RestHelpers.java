package eu.ehri.extension;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;

/**
 * Static helpers for dealing with REST conversions.
 * 
 * @author Mike Bryant (http://github.com/mikesname)
 * 
 */
public class RestHelpers {
    /**
     * Produce json formatted ErrorMessage
     * 
     * @param e
     *            The exception
     * @return The json string
     */
    static String produceErrorMessageJson(final Throwable e) {
        // NOTE only put in a stacktrace when debugging??
        // or no stacktraces, only by logging!
        String message = "{errormessage: \"  " + e.getMessage() + "\""
                + ", stacktrace:  \"  " + getStackTrace(e) + "\"" + "}";

        return message;
    }

    /**
     * Wrap an exception in a StreamingOutput.
     * 
     * @param e
     * @return
     */
    static StreamingOutput streamingException(final Throwable e) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream arg0) throws IOException,
                    WebApplicationException {
                arg0.write((produceErrorMessageJson(e)).getBytes());
            }
        };
    }

    // Use for testing
    // see http://www.javapractices.com/topic/TopicAction.do?Id=78
    // for even nicer trace tool
    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }
}
