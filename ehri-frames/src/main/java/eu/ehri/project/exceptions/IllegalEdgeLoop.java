package eu.ehri.project.exceptions;

/**
 * Error thrown when creating a relationship would
 * result in dangerous/nonsensical behaviour.
 *
 * @author Mike Bryant (http://github.com/mikesname)
 */
public class IllegalEdgeLoop extends Exception {
    public IllegalEdgeLoop() {
    }

    public IllegalEdgeLoop(String message) {
        super(message);
    }
}
