package org.petrinator.util;

import java.util.LinkedList;

import javax.swing.JOptionPane;
import org.petrinator.editor.Root;
import org.petrinator.editor.actions.ReloadFileAction;
import org.petrinator.editor.filechooser.FileType;
import org.petrinator.editor.filechooser.PflowFileType;

import pipe.gui.widgets.ResultsHTMLPane;

import java.net.URLDecoder;

public class Save {

    public static void reSaveNet(Root root) {
        FileType chosenFileType = (FileType) new PflowFileType();
        LinkedList<FileType> fileTypes = new LinkedList<>();
        fileTypes.add(chosenFileType);
        ReloadFileAction reload = new ReloadFileAction(root, fileTypes);
        reload.actionPerformed(null);
    }

    public static String getOsName() {
        return System.getProperty("os.name");
    }

    //Get actual absolute executed .jar path
    public static String get_Current_JarPath(Class c,Root root,ResultsHTMLPane results)
    {
        String pathNet = c.getProtectionDomain().getCodeSource().getLocation().getPath();
        pathNet = pathNet.substring(0, pathNet.lastIndexOf("/"));
        if (getOsName().startsWith("Windows") && pathNet.startsWith("/"))
            pathNet = pathNet.substring(1, pathNet.length());
        String decodedPath = null;
        try {
            decodedPath = URLDecoder.decode(pathNet, "UTF-8");
        } catch (Exception e) {
            results.setText("");
            JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error obtaining absolute jar path", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
        //System.out.println("Jar path : " + decodedPath);
        return decodedPath;
    }
}
