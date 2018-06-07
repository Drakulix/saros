package de.fu_berlin.inf.dpp.server.session;

import org.apache.log4j.Logger;
import de.fu_berlin.inf.dpp.negotiation.SessionNegotiation;
import de.fu_berlin.inf.dpp.negotiation.OutgoingSessionNegotiation;
import de.fu_berlin.inf.dpp.negotiation.IncomingSessionNegotiation;
import de.fu_berlin.inf.dpp.negotiation.ProjectNegotiation;
import de.fu_berlin.inf.dpp.negotiation.OutgoingProjectNegotiation;
import de.fu_berlin.inf.dpp.negotiation.IncomingProjectNegotiation;
import de.fu_berlin.inf.dpp.negotiation.NegotiationTools;
import de.fu_berlin.inf.dpp.server.progress.ConsoleProgressIndicator;
import de.fu_berlin.inf.dpp.session.INegotiationHandler;
import de.fu_berlin.inf.dpp.session.ISarosSessionManager;

public class NegotiationHandler implements INegotiationHandler {

    private static final Logger LOG = Logger
        .getLogger(NegotiationHandler.class);

    private final ISarosSessionManager sessionManager;

    public NegotiationHandler(ISarosSessionManager sessionManager) {
        sessionManager.setNegotiationHandler(this);
        this.sessionManager = sessionManager;
    }

    @Override
    public void handleOutgoingSessionNegotiation(
        OutgoingSessionNegotiation negotiation) {
        SessionNegotiation.Status status = negotiation
            .start(new ConsoleProgressIndicator(System.out));

        switch (status) {
        case OK:
            sessionManager.startSharingProjects(negotiation.getPeer());
            break;
        case CANCEL:
        case REMOTE_CANCEL:
            break;
        case ERROR:
        case REMOTE_ERROR:
            LOG.error(negotiation.getErrorMessage());
            break;
        }
    }

    @Override
    public void handleIncomingSessionNegotiation(
        IncomingSessionNegotiation negotiation) {
        negotiation.localCancel("Server does not accept session invites",
            NegotiationTools.CancelOption.NOTIFY_PEER);
    }

    @Override
    public void handleOutgoingProjectNegotiation(
        OutgoingProjectNegotiation negotiation) {
        ProjectNegotiation.Status status = negotiation
            .run(new ConsoleProgressIndicator(System.out));

        switch (status) {
        case OK:
            break;
        case CANCEL:
        case REMOTE_CANCEL:
            break;
        case ERROR:
        case REMOTE_ERROR:
            LOG.error(negotiation.getErrorMessage());
            break;
        }
    }

    @Override
    public void handleIncomingProjectNegotiation(
        IncomingProjectNegotiation negotiation) {
        negotiation.localCancel("Server does not accept incoming projects",
            NegotiationTools.CancelOption.NOTIFY_PEER);
    }

}
