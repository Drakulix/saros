package de.fu_berlin.inf.dpp.ui;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.picocontainer.annotations.Nullable;

import de.fu_berlin.inf.dpp.editor.internal.EditorAPI;
import de.fu_berlin.inf.dpp.util.Util;

/**
 * Eclipse Dialog to show Exception Messages.
 * 
 * @author rdjemili
 * @author chjacob
 * 
 */
public class ErrorMessageDialog {

    private static final Logger log = Logger.getLogger(ErrorMessageDialog.class
        .getName());

    /**
     * show error message dialog.
     * 
     * @param exception
     */
    public static void showErrorMessage(final Exception exception) {
        Util.runSafeSWTSync(log, new Runnable() {
            public void run() {
                MessageDialog.openError(EditorAPI.getShell(), exception
                    .toString(), exception.getMessage());
            }
        });
    }

    /**
     * Opens a modal dialog which displays the given error message to the user.
     * 
     * @param exceptionMessage
     *            The message to show the user or null (in which case
     *            "An unspecified error occurred" is printed, which is not very
     *            desirable)
     */
    public static void showErrorMessage(@Nullable String exceptionMessage) {

        if ((exceptionMessage == null) || exceptionMessage.trim().length() == 0) {
            exceptionMessage = "An unspecified error occurred.";
        }
        final String error = exceptionMessage;

        Util.runSafeSWTSync(log, new Runnable() {
            public void run() {
                MessageDialog.openError(EditorAPI.getShell(),
                    "Error in Saros-Plugin", error);
            }
        });
    }
}
