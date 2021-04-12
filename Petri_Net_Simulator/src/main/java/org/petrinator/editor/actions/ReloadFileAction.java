package org.petrinator.editor.actions;

import org.petrinator.editor.Root;
import org.petrinator.editor.filechooser.FileChooserDialog;
import org.petrinator.editor.filechooser.FileType;
import org.petrinator.editor.filechooser.FileTypeException;
import org.petrinator.petrinet.Document;
import org.petrinator.util.GraphicsTools;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

public class ReloadFileAction extends AbstractAction {

    private Root root;
    private List<FileType> fileTypes;

    public ReloadFileAction(Root root, List<FileType> fileTypes) {
        this.root = root;
        this.fileTypes = fileTypes;
        String name = "Reload file";
        putValue(NAME, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/Open16.gif"));
        putValue(SHORT_DESCRIPTION, name);
        putValue(MNEMONIC_KEY, KeyEvent.VK_O);
    }

    public void actionPerformed(ActionEvent e) {
        if (!root.isModified() || JOptionPane.showOptionDialog(
                root.getParentFrame(),
                "Any unsaved changes will be lost. Continue?",
                "Reload file...",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new String[]{"Reload...", "Cancel"},
                "Cancel") == JOptionPane.YES_OPTION) {

            File file = root.getCurrentFile();
            if (file != null) {
                FileType fileType = FileType.getAcceptingFileType(file, fileTypes);

                try {
                    root.getEventList().resetEvents();
                    Document document = fileType.load(file);
                    root.setDocument(document);
                    root.setCurrentFile(file);
                    root.setModified(false);
                } catch (FileTypeException ex) {
                    JOptionPane.showMessageDialog(root.getParentFrame(), ex.getMessage());
                }
            }

        }
    }
}
