package de.fu_berlin.inf.dpp.project;

/**
 * Abstract ISessionListener that does nothing in all the methods.
 * 
 * Clients can override just the methods they want to act upon.
 */
public abstract class AbstractSessionListener implements ISessionListener {

    public void sessionEnded(ISarosSession oldSarosSession) {
        // do nothing
    }

    public void sessionStarted(ISarosSession newSarosSession) {
        // do nothing
    }
}
