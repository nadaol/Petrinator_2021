/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.petrinator.editor.commands;

import org.petrinator.petrinet.Arc;
import org.petrinator.util.Command;

/**
 *
 * @author jan.tancibok
 */
public class SetArcResetCommand implements Command {

    private Arc arc;
    private boolean isReset;
    private String oldType;

    public SetArcResetCommand(Arc arc, boolean reset) {
        this.arc = arc;
        this.isReset = reset;
    }

    public void execute() {
        oldType = arc.getType();
        if (isReset) {
            arc.setType(Arc.RESET);
        }
        else {
            arc.setType(Arc.REGULAR);
        }
    }

    public void undo() {
        arc.setType(oldType);
    }

    public void redo() {
        execute();
    }

    @Override
    public String toString() {
        return "Set arc type to reset arc";
    }
}
