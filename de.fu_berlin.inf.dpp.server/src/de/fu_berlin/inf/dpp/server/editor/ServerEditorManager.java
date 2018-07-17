package de.fu_berlin.inf.dpp.server.editor;

import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.activities.TextEditActivity;
import de.fu_berlin.inf.dpp.editor.IEditorManager;
import de.fu_berlin.inf.dpp.editor.ISharedEditorListener;
import de.fu_berlin.inf.dpp.editor.text.LineRange;
import de.fu_berlin.inf.dpp.editor.text.TextSelection;
import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.session.User;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerEditorManager implements IEditorManager {

    private static final Logger LOG = Logger
        .getLogger(ServerEditorManager.class);

    private Map<SPath, Editor> openEditors = Collections.synchronizedMap(new LRUMap(10));
    private List<ISharedEditorListener> listeners = new CopyOnWriteArrayList();

    @Override public void openEditor(SPath path, boolean activate) {
        try {
            getOrCreateEditor(path);
        } catch (IOException e) {
            LOG.warn("Could not open editor for "+path);
        }
    }

    @Override public Set<SPath> getOpenEditors() {
        return openEditors.keySet();
    }

    @Override public String getContent(SPath path) {
        try {
            return getOrCreateEditor(path).getContent();
        } catch (IOException e) {
            return null;
        }
    }

    @Override public void saveEditors(IProject project) {
        // do nothing?
        // we do not keep dirty editors,
        // because the LRUMap might close Editors at any time
    }

    @Override public void closeEditor(SPath path) {
        openEditors.remove(path);
    }

    @Override public void adjustViewport(SPath path, LineRange range,
        TextSelection selection) {
        throw new UnsupportedOperationException();
    }

    @Override public void jumpToUser(User target) {
        throw new UnsupportedOperationException();
    }

    @Override public void addSharedEditorListener(
        ISharedEditorListener listener) {
        listeners.add(listener);
    }

    @Override public void removeSharedEditorListener(
        ISharedEditorListener listener) {
        listeners.remove(listener);
    }

    private Editor getOrCreateEditor(SPath path) throws IOException {
        Editor editor = openEditors.get(path);
        if (editor == null) {
            editor = new Editor(path);
            openEditors.put(path, editor);
        }
        return editor;
    }

    /**
     * Executes a text edit activity on the matching editor.
     *
     * @param activity
     *            the activity describing the text edit to apply
     */
    public void applyTextEdit(TextEditActivity activity) {
        SPath path = activity.getPath();
        try {
            Editor editor = getOrCreateEditor(path);
            editor.applyTextEdit(activity);
            editor.save();
            for (ISharedEditorListener listener : listeners) {
                listener.textEdited(activity);
            }
        } catch (IOException e) {
            LOG.error("Could not read " + path + " to apply text edit", e);
        }
    }

    public void moveFile(SPath oldPath, SPath newPath) {
        Editor oldEditor = this.openEditors.remove(oldPath);
        this.openEditors.put(newPath, oldEditor);
    }

    public void invalidateFile(SPath file) {
        this.openEditors.remove(file);
    }

    public void invalidateFolder(SPath folder) {
        synchronized (this.openEditors) {
            Set<SPath> keys = this.openEditors.keySet();
            Set<SPath> invalidKeys = new HashSet<>();
            for (SPath path : keys) {
                if (folder.getFullPath().isPrefixOf(path.getFullPath())) {
                    invalidKeys.add(path);
                }
            }
            for (SPath path : invalidKeys) {
                this.openEditors.remove(path);
            }
        }
    }
}
