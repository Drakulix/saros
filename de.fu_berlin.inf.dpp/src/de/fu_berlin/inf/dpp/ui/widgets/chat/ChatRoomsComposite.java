package de.fu_berlin.inf.dpp.ui.widgets.chat;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.jivesoftware.smack.XMPPException;
import org.picocontainer.annotations.Inject;

import de.fu_berlin.inf.dpp.SarosPluginContext;
import de.fu_berlin.inf.dpp.communication.chat.IChat;
import de.fu_berlin.inf.dpp.communication.chat.IChatServiceListener;
import de.fu_berlin.inf.dpp.communication.chat.muc.MultiUserChat;
import de.fu_berlin.inf.dpp.communication.chat.muc.MultiUserChatPreferences;
import de.fu_berlin.inf.dpp.communication.chat.muc.MultiUserChatService;
import de.fu_berlin.inf.dpp.communication.chat.muc.negotiation.MUCNegotiationManager;
import de.fu_berlin.inf.dpp.communication.chat.single.SingleUserChatService;
import de.fu_berlin.inf.dpp.communication.connection.ConnectionHandler;
import de.fu_berlin.inf.dpp.communication.connection.IConnectionStateListener;
import de.fu_berlin.inf.dpp.editor.AbstractSharedEditorListener;
import de.fu_berlin.inf.dpp.editor.EditorManager;
import de.fu_berlin.inf.dpp.net.ConnectionState;
import de.fu_berlin.inf.dpp.net.util.XMPPUtils;
import de.fu_berlin.inf.dpp.net.xmpp.JID;
import de.fu_berlin.inf.dpp.net.xmpp.roster.AbstractRosterListener;
import de.fu_berlin.inf.dpp.net.xmpp.roster.IRosterListener;
import de.fu_berlin.inf.dpp.net.xmpp.roster.RosterTracker;
import de.fu_berlin.inf.dpp.preferences.PreferenceConstants;
import de.fu_berlin.inf.dpp.session.ISarosSession;
import de.fu_berlin.inf.dpp.session.ISarosSessionListener;
import de.fu_berlin.inf.dpp.session.ISarosSessionManager;
import de.fu_berlin.inf.dpp.session.NullSarosSessionListener;
import de.fu_berlin.inf.dpp.ui.ImageManager;
import de.fu_berlin.inf.dpp.ui.Messages;
import de.fu_berlin.inf.dpp.ui.util.SWTUtils;
import de.fu_berlin.inf.dpp.ui.views.SarosView;
import de.fu_berlin.inf.dpp.ui.widgets.ListExplanationComposite.ListExplanation;
import de.fu_berlin.inf.dpp.ui.widgets.ListExplanatoryComposite;
import de.fu_berlin.inf.dpp.ui.widgets.activitylog.ActivityLogControl;
import de.fu_berlin.inf.dpp.util.Pair;
import de.fu_berlin.inf.dpp.util.ThreadUtils;

/**
 * This component shows chat he right side of the {@link SarosView}
 * 
 * @author patbit
 */

public class ChatRoomsComposite extends ListExplanatoryComposite {

    private static final Logger LOG = Logger
        .getLogger(ChatRoomsComposite.class);

    static final Color WHITE = Display.getDefault().getSystemColor(
        SWT.COLOR_WHITE);

    /**
     * Default image for ChatView.
     */
    public static final Image chatViewImage = ImageManager
        .getImage("icons/view16/chat_misc.png");

    /**
     * Image while composing a message.
     */
    public static final Image composingImage = ImageManager
        .getImage("icons/view16/cmpsg_misc.png");

    /**
     * Default image for Activity Log.
     */
    public static final Image activityLogImage = ImageManager
        .getImage("icons/view16/activitylog/activitylog_misc.png");

    private ListExplanation connectFirst = new ListExplanation(
        SWT.ICON_INFORMATION, "To share projects you must connect first.");

    private ListExplanation howToShareProjects = new ListExplanation(
        SWT.ICON_INFORMATION, "To share projects you can either:",
        "Right-click on a project", "Right-click on a contact",
        "Use the Saros menu in the Eclipse menu bar");

    protected boolean isSessionRunning;

    protected boolean isSessionHost;

    protected RosterTracker rosterTracker;

    protected IChat sessionChat;

    protected CTabItem sessionChatErrorTab;

    protected Object mucCreationLock = new Object();

    @Inject
    private ConnectionHandler connectionHandler;

    @Inject
    protected EditorManager editorManager;

    @Inject
    protected ISarosSessionManager sessionManager;

    protected CTabFolder chatRooms;

    @Inject
    protected MultiUserChatService multiUserChatService;

    @Inject
    protected SingleUserChatService singleUserChatService;

    @Inject
    private MUCNegotiationManager mucNegotiationManager;

    @Inject
    private IPreferenceStore preferenceStore;

    private final IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {

        @Override
        public void propertyChange(final PropertyChangeEvent event) {

            SWTUtils.runSafeSWTAsync(LOG, new Runnable() {
                @Override
                public void run() {
                    if (isDisposed())
                        return;

                    executePropertyChangeEvent(event);
                }
            });
        }

        private void executePropertyChangeEvent(final PropertyChangeEvent event) {
            // check if activity log tab shall be enabled or disabled
            if (PreferenceConstants.ACTIVITY_LOG_ENABLED.equals(event
                .getProperty())) {
                final boolean activityLogEnabled = preferenceStore
                    .getBoolean(PreferenceConstants.ACTIVITY_LOG_ENABLED);

                if (activityLogEnabled) {
                    CTabItem activityLogTab = getActivityLogTab();
                    if (activityLogTab == null || activityLogTab.isDisposed()) {
                        createActivityLogTab();
                        openActivityLog();
                    }
                } else {
                    closeActivityLog();
                }
            }

            if (!PreferenceConstants.USE_IRC_STYLE_CHAT_LAYOUT.equals(event
                .getProperty()))
                return;

            final List<Pair<Integer, IChat>> currentData = new ArrayList<Pair<Integer, IChat>>();

            final int activeIdx = chatRooms.getSelectionIndex();

            int idx = 0;

            for (CTabItem tab : chatRooms.getItems()) {
                final Object data = tab.getData();

                if (data instanceof IChat)
                    currentData
                        .add(new Pair<Integer, IChat>(idx, (IChat) data));

                idx++;
            }

            for (Pair<Integer, IChat> chatData : currentData)
                closeChatTab(chatData.v);

            for (Pair<Integer, IChat> chatData : currentData)
                openChat(chatData.v, chatData.p == activeIdx);
        }
    };

    /**
     * This RosterListener closure is added to the RosterTracker to get
     * notifications when the roster changes.
     */
    protected IRosterListener rosterListener = new AbstractRosterListener() {

        @Override
        public void entriesUpdated(final Collection<String> addresses) {
            SWTUtils.runSafeSWTAsync(LOG, new Runnable() {
                @Override
                public void run() {

                    if (ChatRoomsComposite.this.isDisposed())
                        return;

                    LOG.trace("roster entries changed, refreshing chat tabs");

                    Collection<JID> jids = new ArrayList<JID>();

                    for (String address : addresses)
                        jids.add(new JID(address));

                    updateChatTabs(jids);
                }
            });
        }
    };

    private final IConnectionStateListener connectionStateListener = new IConnectionStateListener() {

        @Override
        public void connectionStateChanged(ConnectionState state,
            Exception error) {
            SWTUtils.runSafeSWTAsync(LOG, new Runnable() {

                @Override
                public void run() {
                    if (ChatRoomsComposite.this.isDisposed())
                        return;

                    updateExplanation();
                }
            });
        }
    };

    protected AbstractSharedEditorListener sharedEditorListener = new AbstractSharedEditorListener() {
        @Override
        public void colorChanged() {
            SWTUtils.runSafeSWTAsync(LOG, new Runnable() {
                @Override
                public void run() {
                    for (CTabItem tab : chatRooms.getItems()) {
                        Control control = tab.getControl();
                        if (control instanceof ChatControl) {
                            ((ChatControl) control).updateColors();
                        } else if (control instanceof ActivityLogControl) {
                            ((ActivityLogControl) control).updateColors();
                        }
                    }
                }
            });
        }
    };

    protected ISarosSessionListener sessionListener = new NullSarosSessionListener() {

        @Override
        public void sessionStarting(final ISarosSession session) {

            // create activity log tab if enabled
            SWTUtils.runSafeSWTSync(LOG, new Runnable() {
                @Override
                public void run() {
                    if (isDisposed())
                        return;

                    final boolean activityLogEnabled = preferenceStore
                        .getBoolean(PreferenceConstants.ACTIVITY_LOG_ENABLED);
                    if (activityLogEnabled) {
                        createActivityLogTab();
                        openActivityLog();
                    }
                }
            });

            SWTUtils.runSafeSWTSync(LOG, new Runnable() {
                @Override
                public void run() {
                    isSessionRunning = true;
                    isSessionHost = session.isHost();
                }
            });

            final CountDownLatch mucCreationStart = new CountDownLatch(1);

            /*
             * FIXME: the clients should be notified after the chat has been
             * created to join that chat. For slow connections it is possible
             * that ALICE invites BOB, but BOB creates the chat room first. If
             * now CARL joins the session and BOB leaves afterwards, ALICE and
             * CARLs MUC is broken !
             */
            ThreadUtils.runSafeAsync("dpp-muc-join", LOG, new Runnable() {
                @Override
                public void run() {
                    synchronized (mucCreationLock) {
                        mucCreationStart.countDown();
                        /*
                         * ignore return value, we will be notified by the
                         * listener before this call even returns
                         */
                        createChat(session);
                    }
                }
            });

            try {
                mucCreationStart.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void sessionEnded(final ISarosSession session) {

            final CountDownLatch mucDestroyed = new CountDownLatch(1);

            SWTUtils.runSafeSWTAsync(LOG, new Runnable() {
                @Override
                public void run() {

                    if (sessionChat != null)
                        multiUserChatService.destroyChat(sessionChat);

                    mucDestroyed.countDown();

                    isSessionRunning = false;
                    isSessionHost = false;
                    sessionChat = null;

                    if (isDisposed())
                        return;

                    if (sessionChatErrorTab != null
                        && !sessionChatErrorTab.isDisposed())
                        sessionChatErrorTab.dispose();

                    closeActivityLog();
                }
            });

            /*
             * It is possible that the session was stopped by disconnecting from
             * the server. As we run async. it is possible that destroyChat will
             * wait for an acknowledge packet but this will never be received
             * because the connection is closed in the meantime. This will
             * produce a GUI freeze (currently 30 seconds, see Smack Packet
             * Timeout). So we wait here to ensure that the connection is not
             * closed until the chat is destroyed. See also SarosSessionManager
             * class.
             */
            try {
                mucDestroyed.await(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    };

    protected DisposeListener disposeListener = new DisposeListener() {

        @Override
        public void widgetDisposed(DisposeEvent e) {
            CTabItem source = (CTabItem) e.getSource();
            source.getControl().dispose();

            updateExplanation();
        }
    };

    protected IChatServiceListener chatServiceListener = new IChatServiceListener() {

        @Override
        public void chatCreated(final IChat chat, boolean createdLocally) {
            SWTUtils.runSafeSWTAsync(LOG, new Runnable() {
                @Override
                // FIXME: disposed ?!
                public void run() {

                    boolean isSessionChat = false;

                    if (chat instanceof MultiUserChat) {

                        /*
                         * creation of the session chat is done async., so it is
                         * possible to receive multiple requests from different
                         * Saros sessions
                         */
                        if (sessionChat != null)
                            multiUserChatService.destroyChat(sessionChat);

                        sessionChat = chat;
                        isSessionChat = true;
                    }

                    /*
                     * creation of the session chat is done async., so the
                     * session may have already ended
                     */
                    if (!isSessionRunning && isSessionChat) {
                        multiUserChatService.destroyChat(sessionChat);
                        sessionChat = null;
                    } else {
                        openChat(chat, false);
                    }
                }
            });
        }

        @Override
        public void chatDestroyed(final IChat chat) {
            SWTUtils.runSafeSWTAsync(LOG, new Runnable() {

                @Override
                public void run() {
                    if (isDisposed())
                        return;

                    closeChatTab(chat);
                }
            });
        }

        @Override
        public void chatAborted(IChat chat, XMPPException exception) {

            if (!(chat instanceof MultiUserChat))
                return;

            MultiUserChatPreferences preferences = ((MultiUserChat) chat)
                .getPreferences();

            String mucService = preferences.getService();

            final String errorMessage;

            if (mucService == null) {
                errorMessage = isSessionHost ? Messages.ChatRoomsComposite_muc_error_host_no_service_found
                    : Messages.ChatRoomsComposite_muc_error_client_no_service_found;
            } else {
                errorMessage = MessageFormat
                    .format(
                        Messages.ChatRoomsComposite_muc_error_connecting_failed,
                        mucService,
                        exception == null ? Messages.ChatRoomsComposite_muc_error_connecting_failed_unknown_error
                            : exception.getMessage());
            }

            SWTUtils.runSafeSWTAsync(LOG, new Runnable() {

                @Override
                public void run() {
                    if (isDisposed())
                        return;

                    setErrorMessage(errorMessage);
                }
            });
        }
    };

    public ChatRoomsComposite(Composite parent, int style,
        final RosterTracker rosterTracker) {
        super(parent, style);

        this.rosterTracker = rosterTracker;
        rosterTracker.addRosterListener(rosterListener);

        SarosPluginContext.initComponent(this);

        sessionManager.addSarosSessionListener(sessionListener);
        editorManager.addSharedEditorListener(sharedEditorListener);
        singleUserChatService.addChatServiceListener(chatServiceListener);
        multiUserChatService.addChatServiceListener(chatServiceListener);
        connectionHandler.addConnectionStateListener(connectionStateListener);
        preferenceStore.addPropertyChangeListener(propertyChangeListener);

        setLayout(new FillLayout());

        chatRooms = new CTabFolder(this, SWT.BOTTOM);
        setContentControl(chatRooms);

        chatRooms.setSimple(true);
        chatRooms.setBorderVisible(true);

        /*
         * TODO: The user can open and close Views as he wishes. This means that
         * the live cycle of this ChatView is completely independent of the
         * global MultiUserChat. Therefore we need to correctly validate the
         * MultiUserChat's state when this ChatView is reopened.
         */

        ISarosSession session = sessionManager.getSarosSession();

        isSessionRunning = sessionManager.getSarosSession() != null;
        isSessionHost = session != null && session.isHost();

        updateExplanation();

        addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(DisposeEvent e) {
                sessionManager.removeSarosSessionListener(sessionListener);

                editorManager.removeSharedEditorListener(sharedEditorListener);

                preferenceStore
                    .removePropertyChangeListener(propertyChangeListener);

                singleUserChatService
                    .removeChatServiceListener(chatServiceListener);

                multiUserChatService
                    .removeChatServiceListener(chatServiceListener);

                connectionHandler
                    .removeConnectionStateListener(connectionStateListener);
                /**
                 * This must be called before finalization otherwise you will
                 * get NPE on RosterTracker.
                 */
                rosterTracker.removeRosterListener(rosterListener);
            }
        });

    }

    /**
     * Create a new single user chat with the given JID and open it.
     * 
     * @param jid
     * @param activateAfterCreation
     *            see {@link ChatRoomsComposite#openChat(IChat, boolean)} *
     */
    public void openChat(JID jid, boolean activateAfterCreation) {
        openChat(singleUserChatService.createChat(jid), activateAfterCreation);
    }

    /**
     * Open the tab for a given chat.
     * 
     * If the the corresponding tab already exists, it will be activated,
     * otherwise a new tab will be created.
     * 
     * @param chat
     *            The chat that should be displayed. If no corresponding chat
     *            tab exists, a new one will be created.
     * @param activateAfterCreation
     *            If a new tab is created, setting this parameter
     *            <code>false</code> will open the tab in background,
     *            <code>true</code> will activate it. If the newly created chat
     *            tab is the only one, it will of course be active anyway. If
     *            the chat tab already exists, this parameter has no effect: the
     *            tab will be activated anyway.
     */
    public void openChat(IChat chat, boolean activateAfterCreation) {
        if (selectExistentTab(chat)) {
            return;
        }

        hideExplanation();

        CTabItem chatTab = createChatTab(chat);
        if (activateAfterCreation || chatRooms.getItemCount() == 1) {
            chatRooms.setSelection(chatTab);
        }
    }

    private CTabItem createChatTab(IChat chat) {
        ChatControl control = new ChatControl(this, chat, chatRooms,
            SWT.BORDER, WHITE, 2);

        CTabItem chatTab;

        if (chat == sessionChat)
            chatTab = new CTabItem(chatRooms, SWT.NONE, 0);
        else
            chatTab = new CTabItem(chatRooms, SWT.CLOSE);

        chatTab.setText(getChatTabName(chat));
        chatTab.setImage(chatViewImage);
        chatTab.setData(chat);
        chatTab.setControl(control);
        chatTab.addDisposeListener(disposeListener);

        return chatTab;
    }

    private CTabItem createActivityLogTab() {
        ActivityLogControl activityControl = new ActivityLogControl(this,
            chatRooms, SWT.BORDER, WHITE);

        CTabItem activityLogTab = new CTabItem(chatRooms, SWT.NONE, 0);
        activityLogTab.setText(Messages.ActivityLog_tab_title);
        activityLogTab.setImage(activityLogImage);
        activityLogTab.setControl(activityControl);
        activityLogTab.addDisposeListener(disposeListener);

        return activityLogTab;
    }

    private void openActivityLog() {
        CTabItem activityLogItem = getActivityLogTab();
        chatRooms.setSelection(activityLogItem);
    }

    private void closeActivityLog() {
        CTabItem activityLogItem = getActivityLogTab();
        if (activityLogItem != null && !activityLogItem.isDisposed()) {
            activityLogItem.dispose();
            updateExplanation();
        }
    }

    public CTabItem getActivityLogTab() {
        for (CTabItem tab : chatRooms.getItems()) {

            if (tab.getControl() instanceof ActivityLogControl)
                return tab;
        }

        return null;
    }

    /*
     * TODO make this protected / private and use a listener to update the chat
     * tabs text
     */
    public CTabItem getChatTab(IChat chat) {
        for (CTabItem tab : chatRooms.getItems()) {

            // do the equal check this way to allow null values in the tab data
            if (chat.equals(tab.getData()))
                return tab;
        }

        return null;
    }

    private boolean closeChatTab(IChat chat) {
        CTabItem tab = getChatTab(chat);
        if (tab != null && !tab.isDisposed()) {
            tab.dispose();

            updateExplanation();
            return true;
        }

        return false;
    }

    /**
     * Update title and history for chats. A chat is updated if its participants
     * contains any of the given {@link JID}s.
     * 
     * @param jids
     *            JIDs whom the chats should be updated for
     */
    private void updateChatTabs(Collection<JID> jids) {
        for (CTabItem tab : chatRooms.getItems()) {

            if (!(tab.getControl() instanceof ChatControl))
                continue;

            ChatControl control = (ChatControl) tab.getControl();
            IChat chat = (IChat) tab.getData();

            if (!Collections.disjoint(jids, chat.getParticipants())) {
                control.updateDisplayNames();
                tab.setText(getChatTabName(chat));
            }
        }
    }

    private boolean selectExistentTab(IChat chat) {
        for (CTabItem item : chatRooms.getItems()) {
            // do the equal check this way to allow null values in the tab data
            if (chat.equals(item.getData())) {
                chatRooms.setSelection(item);
                return true;
            }
        }

        return false;
    }

    private void updateExplanation() {
        if (chatRooms.getItemCount() != 0)
            return;

        if (!connectionHandler.isConnected())
            showExplanation(connectFirst);
        else
            showExplanation(howToShareProjects);
    }

    /**
     * Returns the {@linkplain ChatControl control} for the current selected
     * chat (tab) or <code>null</code> if no tab or a tab is selected which not
     * represents a chat.
     * 
     * @return the control for the current selected chat or <code>null</code>
     */
    public ChatControl getSelectedChatControl() {
        return !isChatExistent() ? null : (ChatControl) chatRooms
            .getSelection().getControl();
    }

    public boolean isChatExistent() {
        return chatRooms.getSelection() != null
            && (chatRooms.getSelection().getControl() instanceof ChatControl);
    }

    private void setErrorMessage(String message) {

        if (!isSessionRunning || message == null)
            return;

        if (sessionChatErrorTab != null && !sessionChatErrorTab.isDisposed())
            sessionChatErrorTab.dispose();

        ListExplanatoryComposite errorComposite = new ListExplanatoryComposite(
            chatRooms, SWT.NONE);

        ListExplanation explanation = new ListExplanation(SWT.ICON_ERROR,
            message);

        errorComposite.showExplanation(explanation);

        sessionChatErrorTab = new CTabItem(chatRooms, SWT.CLOSE, 0);

        sessionChatErrorTab
            .setText(Messages.ChatRoomsComposite_muc_error_tab_text);

        // TODO add an icon

        sessionChatErrorTab.setData(null);
        sessionChatErrorTab.setControl(errorComposite);
        sessionChatErrorTab.addDisposeListener(disposeListener);
        chatRooms.setSelection(sessionChatErrorTab);

        hideExplanation();

    }

    /**
     * Connects to the session's {@link MultiUserChat}. Automatically (if
     * necessary) created and joins the {@link MultiUserChat}.
     * 
     * @param session
     *            session the multi user chat should belong to
     * @return multi user chat of the session
     */
    private IChat createChat(ISarosSession session) {
        MultiUserChatPreferences preferences = session.isHost() ? mucNegotiationManager
            .getOwnPreferences() : mucNegotiationManager
            .getSessionPreferences();

        return multiUserChatService.createChat(preferences);
    }

    /**
     * Returns the name for the given chat that would be used as caption for a
     * chat tab.
     * 
     * @param chat
     * @return
     */
    public String getChatTabName(IChat chat) {

        if (chat instanceof MultiUserChat)
            return Messages.ChatRoomsComposite_roundtable;
        else {
            JID jid = chat.getParticipants().iterator().next();
            String nickname = XMPPUtils.getNickname(null, jid);
            return nickname == null ? jid.getBase() : nickname;
        }
    }
}
