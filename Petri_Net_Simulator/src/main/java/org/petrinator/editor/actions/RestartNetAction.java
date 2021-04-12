package org.petrinator.editor.actions;

import org.petrinator.editor.Root;
import org.petrinator.util.GraphicsTools;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class RestartNetAction extends AbstractAction {

    private Root root;

    public RestartNetAction(Root root){
        this.root = root;
        putValue(NAME, "Restart");
        putValue(SHORT_DESCRIPTION, "Reset Net");
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/invariant16.png"));

    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {

        root.getDocument().getPetriNet().getInitialMarking().resetMarking();

    }
}
