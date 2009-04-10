package de.fu_berlin.inf.dpp.invitation;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import de.fu_berlin.inf.dpp.FileList;

/**
 * An incoming invitation process. Is created when we receive an invitation to a
 * shared project on a remote system.
 * 
 * @author rdjemili
 */
public interface IIncomingInvitationProcess extends IInvitationProcess {

    /**
     * Requests the file list of the remotely shared project. This methods
     * blocks until the file list is retrieved.
     * 
     * This method can only be called while in INVITATION_SENT state.
     * 
     * If the local user canceled via the monitor the Invitation process is
     * canceled.
     * 
     * @throws InterruptedException
     *             If the local user canceled waiting for the operation user
     *             isCanceled of the monitor.
     */
    public void requestRemoteFileList(IProgressMonitor monitor)
        throws InterruptedException;

    /**
     * @return the file list of the remotely shared project or <code>null</code>
     *         if it hasn't been requested and retrieved yet. Make a call to
     *         {@link #requestRemoteFileList(IProgressMonitor)} before using
     *         this method.
     */
    public FileList getRemoteFileList();

    /**
     * Accepts the incoming invitation and creates the shared project. This
     * method blocks until the synchronization is done.
     * 
     * This method can be called by the user while in HOST_FILELIST_SENT state.
     * 
     * @param baseProject
     *            the local project that is used as file base for the following
     *            replication. If this is <code>null</code> no project will be
     *            used to start the replication from and a new, empty project
     *            will be created.
     * @param newProjectName
     *            the project name of the new project that is to be generated.
     *            If this is <code>null</code> the <code>baseProject</code> will
     *            be overwritten.
     * @param monitor
     *            a {@link IProgressMonitor} to report progress to and to check
     *            whether the user canceled via
     *            {@link IProgressMonitor#isCanceled()}. Can not be
     *            <code>null</code>.
     * 
     * @throws InterruptedException
     *             if the user canceled the invitation via
     *             {@link IProgressMonitor#isCanceled()}
     */
    public void accept(IProject baseProject, String newProjectName,
        IProgressMonitor monitor) throws InterruptedException;

    public void setInvitationUI(IInvitationUI inviteUI);

}