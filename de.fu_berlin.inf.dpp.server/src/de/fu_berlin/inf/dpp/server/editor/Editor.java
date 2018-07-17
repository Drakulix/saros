package de.fu_berlin.inf.dpp.server.editor;

import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.activities.TextEditActivity;
import de.fu_berlin.inf.dpp.filesystem.IFile;
import de.fu_berlin.inf.dpp.filesystem.IResource;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;

public class Editor {

    private SPath filePath;
    private GapBuffer content;

    public Editor(SPath filePath) throws IOException {
        this.filePath = filePath;

        try (InputStream input = getFile().getContents()) {
            content = new GapBuffer(IOUtils.toString(input));
        }
    }

    /**
     * Returns the path of the text file this editor is associated with.
     *
     * @return associated file
     */
    public SPath getFilePath() {
        return filePath;
    }

    /**
     * Returns the editor's text content. Depending on whether any text edits
     * have been applied, the content may differ from that of the associated
     * file on disk.
     *
     * @return editor's content
     */
    public String getContent() {
        return content.toString();
    }

    /**
     * Applies an editing operation to the editor's content. For performance
     * reasons, the change is not automatically saved to disk; this allows
     * multiple edits to be collected and then written in one go (by calling
     * {@link #save}).
     *
     * @param edit
     *            the text edit operation to apply
     */
    public void applyTextEdit(TextEditActivity edit) {
        content.delete(edit.getOffset(), edit.getReplacedText().length());
        content.insert(edit.getOffset(), edit.getText());
    }

    /**
     * Writes the editor's current content to the associated file on disk. This
     * operation is guaranteed to be atomic - it either succeeds completely or
     * doesn't change the workspace at all (in case an exception is thrown).
     *
     * @throws IOException
     *             if writing the file fails
     */
    public void save() throws IOException {
        getFile().setContents(IOUtils.toInputStream(content.toString()), true, true);
    }

    private IFile getFile() throws IOException {
        IResource resource = filePath.getResource();
        if (resource == null) {
            throw new NoSuchFileException(filePath.toString());
        }

        IFile file = (IFile) resource.getAdapter(IFile.class);
        if (file == null) {
            throw new IOException("Not a file: " + filePath);
        }

        return file;
    }
}
