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
public class SetArcReaderCommand implements Command {

    private Arc arc;
    private boolean isReader;
    private String oldType;
    private int oldMult;

    public SetArcReaderCommand(Arc arc, boolean reader) {
        this.arc = arc;
        this.isReader = reader;
    }

    public void execute() {
        oldType = arc.getType();
        if (isReader) {
            arc.setType(Arc.READ);

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
        return "Set arc type to reader arc";
    }
}
