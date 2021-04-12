package org.petrinator.editor.actions;

import org.petrinator.editor.Root;
import org.petrinator.util.GraphicsTools;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SaveMarkingAction extends AbstractAction {

    private Root root;

    public SaveMarkingAction(Root root){
        this.root = root;
        putValue(NAME, "Save Marking");
        putValue(SHORT_DESCRIPTION, "Save current marking");
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/savemarking.gif"));

    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {

        root.getDocument().getPetriNet().getInitialMarking().updateInitialMarking();

    }
}
