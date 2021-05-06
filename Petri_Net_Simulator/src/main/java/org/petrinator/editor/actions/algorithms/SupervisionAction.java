/*
 * Copyright (C) 2008-2010 Martin Riesz <riesz.martin at gmail.com>
 * Copyright (C) 2016-2017 Joaquin Rodriguez Felici <joaquinfelici at gmail.com>
 * Copyright (C) 2016-2017 Leandro Asson <leoasson at gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


package org.petrinator.editor.actions.algorithms;

import org.petrinator.editor.Root;
import org.petrinator.editor.actions.ReloadFileAction;
import org.petrinator.editor.actions.SaveAction;
import org.petrinator.editor.actions.algorithms.reachability.CRTree;
import org.petrinator.editor.filechooser.*;
import org.petrinator.petrinet.*;
import org.petrinator.util.GraphicsTools;
import pipe.gui.widgets.ButtonBar;
import pipe.gui.widgets.ResultsHTMLPane;
import pipe.modules.minimalSiphons.MinimalSiphons;
import pipe.utilities.math.Matrix;
import pipe.views.PetriNetView;


import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.net.*;
import java.lang.*;
import java.io.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class SupervisionAction extends AbstractAction
{
    private static final String MODULE_NAME = "Deadlock Supervisor";
    private ResultsHTMLPane results;
    private String sPanel;
    private Root root;
    private JDialog guiDialog;
    private ButtonBar FirstAnalizeButton;
    private ButtonBar SecondAnalizeButton;
    private ButtonBar superviseButton;
    private ButtonBar fixConflictButton;
    InvariantAction accion;
    MatricesAction matrices;
    ReachabilityAction states;
    //SiphonsAction sifon;
    //test
    ServerSocket server = null;
    Process proceso = null;
    DataOutputStream outw = null;
    DataInputStream inw = null;

    public SupervisionAction(Root root)
    {

        this.root = root;
        putValue(NAME, MODULE_NAME);
        putValue(SHORT_DESCRIPTION, MODULE_NAME);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/deadlock.png"));

        guiDialog =  new JDialog(root.getParentFrame(), MODULE_NAME, true);
        Container contentPane = guiDialog.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

        results = new ResultsHTMLPane("");
        sPanel = new String();//new
        contentPane.add(results);
        FirstAnalizeButton = new ButtonBar("First Analysis", new ClassifyListener(), guiDialog.getRootPane());
        SecondAnalizeButton = new ButtonBar("Net Analysis (with supervisors)", new SecondClassifyListener(), guiDialog.getRootPane());
        superviseButton = new ButtonBar("Add Supervisor/s", new AddSupervisorListener(), guiDialog.getRootPane());
        fixConflictButton = new ButtonBar("Fix Conflict/s", new FixConflictListener(), guiDialog.getRootPane());
        contentPane.add(FirstAnalizeButton);
        contentPane.add(SecondAnalizeButton);
        contentPane.add(superviseButton);
        contentPane.add(fixConflictButton);
        //creo un objeto de invariantes
        accion = new InvariantAction(this.root);
        matrices = new MatricesAction(this.root);
        states = new ReachabilityAction(this.root);
        //sifon = new SiphonsAction(this.root);

    }

    public void actionPerformed(ActionEvent e)
    {

        results.setText("");

        // Disables the copy and save buttons
        results.setEnabled(false);

        // Enables classify button
        FirstAnalizeButton.setButtonsEnabled(true);
        FirstAnalizeButton.setEnabled(true);//para luego chequear
        SecondAnalizeButton.setButtonsEnabled(true);
        superviseButton.setButtonsEnabled(false);
        fixConflictButton.setButtonsEnabled(false);

        // Shows initial pane
        guiDialog.pack();
        guiDialog.setLocationRelativeTo(root.getParentFrame());
        guiDialog.setVisible(true);
    }

    /*
        Exports all html analysis and pflow net for suprevision analysis

     */
    public void Runanalysis(String message)
    {
        //JOptionPane.showMessageDialog(null, "llego al run alanisis", "Error", JOptionPane.ERROR_MESSAGE, null);
        invariantAnalysis();
        matricesAnalysis();
        coverabilityAnalysis();
        sifonnalysis();
        saveNet();
        socketServer(message);//Supervision analysis
    }
    public void execServer()
    {
        int port=0;
        Socket cli = null;

        try {
            server = new ServerSocket(0);
            port = server.getLocalPort();

            //Get tesis python path and execute
            String pathToPythonMain = get_Current_JarPath() +"/Modulos/Deadlock-supervisor/tesis.py";
            String pathPythonExec = "python3 "+pathToPythonMain + " "+ port + " " + root.getCurrentFile().getPath();
            System.out.println(pathPythonExec);
            proceso=Runtime.getRuntime().exec(pathPythonExec);
            //Blocking accept executed python client
            cli = server.accept();
            //Instantiate input and output socket buffers
            outw = new DataOutputStream(cli.getOutputStream());
            inw = new DataInputStream(cli.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Executes tesis.py and get the response using sockets
    public void socketServer(String message)
    {
        /*
        ServerSocket server = null;
        Process proceso = null;
        DataOutputStream outw = null;
        DataInputStream inw = null;
        */
        int port=0;
        String Respuesta;
        Socket cli = null;


        try {

            server = new ServerSocket(0);
            port = server.getLocalPort();

            //Get tesis python path and execute
            String pathToPythonMain = get_Current_JarPath() +"/Modulos/Deadlock-supervisor/tesis.py";
            String pathPythonExec = "python3 "+pathToPythonMain + " "+ port + " " + root.getCurrentFile().getPath();
            System.out.println(pathPythonExec);
            proceso=Runtime.getRuntime().exec(pathPythonExec);

            //Blocking accept executed python client
            cli = server.accept();
            //Instantiate input and output socket buffers
            outw = new DataOutputStream(cli.getOutputStream());
            inw = new DataInputStream(cli.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            outw.writeUTF(message);
            outw.flush();
            Respuesta =inw.readUTF();
            System.out.println ("Respuesta:");
            System.out.println (Respuesta);
            String[] treeInfo = new String[]{
                    Respuesta
            };
            sPanel += "<h2>Net Anylisis Results</h2>";
            sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
            results.setText(sPanel);
            results.setEnabled(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
        superviseButton.setButtonsEnabled(true);
    }

    //Function to save the current net in a temp.pflow file for later supervision analisys
    public void saveNet() {

        FileChooserDialog chooser = new FileChooserDialog();
        chooser.setVisible(false);
        chooser.setAcceptAllFileFilterUsed(false);

        String Temp_net_path = get_Current_JarPath() + "/Modulos/Deadlock-supervisor/tmp/net.pflow";

        File file = new File(Temp_net_path);
        FileType chosenFileType = (FileType) new PflowFileType();
        try {
            chosenFileType.save(root.getDocument(), file);
        } catch (FileTypeException ex) {
            JOptionPane.showMessageDialog(root.getParentFrame(), ex.getMessage());
        }
    }
    /*
     Do Siphon's and trap's analisys and saves it in html extension
  */
    public void sifonnalysis()
    {
        /*
         * Create tmp.pnml file
         */
        FileChooserDialog chooser = new FileChooserDialog();

        if (root.getCurrentFile() != null) {
            chooser.setSelectedFile(root.getCurrentFile());
        }

        chooser.addChoosableFileFilter(new PipePnmlFileType());
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setCurrentDirectory(root.getCurrentDirectory());
        chooser.setDialogTitle("Save as...");

        File file = new File("tmp/" + "tmp" + "." + "pnml");
        FileType chosenFileType = (FileType) chooser.getFileFilter();
        try {
            chosenFileType.save(root.getDocument(), file);
        } catch (FileTypeException e1) {
            e1.printStackTrace();
        }
        /*
         * Read tmp file
         */
        PetriNetView sourceDataLayer = new PetriNetView("tmp/tmp.pnml");
        String s = "<h2>Siphons and Traps</h2>";

        if (sourceDataLayer == null) {
            return;
        }
        if(!root.getDocument().getPetriNet().getRootSubnet().hasPlaces() || !root.getDocument().getPetriNet().getRootSubnet().hasTransitions())
        {
            s += "Invalid net!";
        } else {
            try {
                MinimalSiphons siphonsAlgorithm = new MinimalSiphons();
                s += siphonsAlgorithm.analyse(sourceDataLayer);
                results.setEnabled(false);
            } catch (OutOfMemoryError oome) {
                System.gc();
                results.setText("");
                s = "Memory error: " + oome.getMessage();

                s += "<br>Not enough memory. Please use a larger heap size."
                        + "<br>"
                        + "<br>Note:"
                        + "<br>The Java heap size can be specified with the -Xmx option."
                        + "<br>E.g., to use 512MB as heap size, the command line looks like this:"
                        + "<br>java -Xmx512m -classpath ...\n";
                results.setText(s);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                s = "<br>Error" + e.getMessage();
                results.setText(s);
                return;
            }
        }
        results.setText(s);
        SaveHTML("sif");
    }
    /*
       Do the coverability analisys and saves it in html extension
    */
    public void coverabilityAnalysis()
    {
        // Checks if the net is valid
        if (!root.getDocument().getPetriNet().getRootSubnet().isValid()) {
            JOptionPane.showMessageDialog(null, "Invalid Net!", "Error", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        // Disables the calculate button

        String log = "<p></p><h2>Reachability/Coverability Graph Information</h2>";

        log += "<h3> Number of places: "+root.getDocument().getPetriNet().getSortedPlaces().size() +"</h3>";
        log += "<h3> Number of transitions: "+root.getDocument().getPetriNet().getSortedTransitions().size() +"</h3>";

        //TODO check tree size
        try {
            CRTree statesTree = new CRTree(root, root.getCurrentMarking().getMarkingAsArray()[Marking.CURRENT]);
            log += statesTree.getTreeLog();
            states.reachMatrix = statesTree.getReachabilityMatrix();
            // Enables the copy and save buttons
            results.setEnabled(false);
        } catch (StackOverflowError e) {
            log = "An error has occurred, the net might have too many states...";
        }

        results.setText(log);
        SaveHTML("cov");

    }
    /*
        Do the invariant analisys and saves it in html extension
     */
    public void invariantAnalysis()
    {
        //PetriNetView sourceDataLayer = new PetriNetView("tmp/tmp.pnml");
        accion._incidenceMatrix = new Matrix(root.getDocument().getPetriNet().getIncidenceMatrix());;
        String s = "<h2>Petri Net Invariant Analysis</h2>";

        if(!root.getDocument().getPetriNet().getRootSubnet().hasPlaces() || !root.getDocument().getPetriNet().getRootSubnet().hasTransitions())
        {
            s += "Invalid net!";
        }
        else
        {
            try
            {

                //PNMLWriter.saveTemporaryFile(sourceDataLayer,this.getClass().getName());
                s += accion.analyse();
                results.setEnabled(false);
            }
            catch(OutOfMemoryError oome)
            {
                System.gc();
                results.setText("");
                s = "Memory error: " + oome.getMessage();

                s += "<br>Not enough memory. Please use a larger heap size." + "<br>" + "<br>Note:" + "<br>The Java heap size can be specified with the -Xmx option." + "<br>E.g., to use 512MB as heap size, the command line looks like this:" + "<br>java -Xmx512m -classpath ...\n";
                results.setText(s);
                return;
            }
            catch(Exception e)
            {
                e.printStackTrace();
                s = "<br>Error" + e.getMessage();
                results.setText(s);
                return;
            }
        }
        results.setText(s);
        SaveHTML("inv");
    }
    /*
        Get matrices and saves it in html extension
     */
    public void matricesAnalysis()
    {
        // Checks if the net is valid
        if(!root.getDocument().getPetriNet().getRootSubnet().isValid()) {
            JOptionPane.showMessageDialog(null, "Invalid Net!", "Error", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        /* Create HTML file with data */
        String s = "<h2>Petri Net Matrices</h2>";

        ArrayList<String> pnames = root.getDocument().getPetriNet().getSortedPlacesNames();
        ArrayList<String> tnames = root.getDocument().getPetriNet().getSortedTransitionsNames();

        try
        {
            s += ResultsHTMLPane.makeTable(new String[]{
                    "Forwards incidence matrix <i>I<sup>+</sup></i>",
                    matrices.renderMatrix(pnames,tnames,root.getDocument().getPetriNet().getForwardIMatrix())
            }, 1, false, false, true, false);
            s += ResultsHTMLPane.makeTable(new String[]{
                    "Backwards incidence matrix <i>I<sup>-</sup></i>",
                    matrices.renderMatrix(pnames,tnames,root.getDocument().getPetriNet().getBackwardsIMatrix())
            }, 1, false, false, true, false);
            s += ResultsHTMLPane.makeTable(new String[]{
                    "Combined incidence matrix <i>I</i>",
                    matrices.renderMatrix(pnames,tnames,root.getDocument().getPetriNet().getIncidenceMatrix())
            }, 1, false, false, true, false);
            s += ResultsHTMLPane.makeTable(new String[]{
                    "Inhibition matrix <i>H</i>",
                    matrices.renderMatrix(pnames,tnames,root.getDocument().getPetriNet().getInhibitionMatrix())
            }, 1, false, false, true, false);
            s += ResultsHTMLPane.makeTable(new String[]{
                    "Reset matrix <i>H</i>",
                    matrices.renderMatrix(pnames,tnames,root.getDocument().getPetriNet().getResetMatrix())
            }, 1, false, false, true, false);
            s += ResultsHTMLPane.makeTable(new String[]{
                    "Reader matrix <i>H</i>",
                    matrices.renderMatrix(pnames,tnames,root.getDocument().getPetriNet().getReaderMatrix())
            }, 1, false, false, true, false);
            s += ResultsHTMLPane.makeTable(new String[]{
                    "Marking",
                    matrices.renderMarkingMatrices(pnames, root.getDocument())
            }, 1, false, false, true, false);
            s += ResultsHTMLPane.makeTable(new String[]{
                    "Enabled transitions",
                    matrices.renderTransitionStates(tnames, root.getDocument())
            }, 1, false, false, true, false);
        }
        catch(OutOfMemoryError e)
        {
            System.gc();
            results.setText("");
            s = "Memory error: " + e.getMessage();

            s += "<br>Not enough memory. Please use a larger heap size." + "<br>" + "<br>Note:" + "<br>The Java heap size can be specified with the -Xmx option." + "<br>E.g., to use 512MB as heap size, the command line looks like this:" + "<br>java -Xmx512m -classpath ...\n";
            results.setText(s);
            return;
        }
        catch(Exception e)
        {
            s = "<br>Invalid net";
            results.setText(s);
            return;
        }

        results.setText(s);

        // Enables the copy and save buttons
        results.setEnabled(false);
        SaveHTML("mat");
    }

    /*
        SAVE AS HTML
     */
    public void SaveHTML(String name)
    {
        String save_path="";
        try
        {
            /*
            File defaultPath = new File(".");
            FileBrowser fileBrowser = new FileBrowser("HTML file", "html", defaultPath.getPath());
            String destFN = fileBrowser.saveFile();
            if(!destFN.toLowerCase().endsWith(".html"))
            {
                destFN += ".html";
            }*/
            save_path= get_Current_JarPath() +"/Modulos/Deadlock-supervisor/tmp/"+ name +".html";

            FileWriter writer = new FileWriter(new File(save_path));
            String output = "<html><head><style type=\"text/css\">" +
                    "body{font-family:Arial,Helvetica,sans-serif;" +
                    "text-align:center;background:#ffffff}" +
                    "td.colhead{font-weight:bold;text-align:center;" +
                    "background:#ffffff}" +
                    "td.rowhead{font-weight:bold;background:#ffffff}" +
                    "td.cell{text-align:center;padding:5px,0}" +
                    "tr.even{background:#a0a0d0}" +
                    "tr.odd{background:#c0c0f0}" +
                    "td.empty{background:#ffffff}" +
                    "</style>" + results.getText();
            writer.write(output);
            writer.close();
        }
        catch(Exception e)
        {
            System.out.println("Error saving HTML "+save_path);
        }
    }

    public void close_socket()
    {
        try {
            outw.writeUTF("quit");
            outw.flush();
            String Respuesta =inw.readUTF();
            System.out.println ("Respuesta:");
            System.out.println (Respuesta);
            outw.close();
            inw.close();
            server.close();
            //proceso.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //cierro sockets y streams
    }
    /**
     * Listener for analyse button
     */
    private class ClassifyListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent)
        {

            // Checks if the net is valid
            if(!root.getDocument().getPetriNet().getRootSubnet().isValid()) {
                JOptionPane.showMessageDialog(null, "Invalid Net!", "Error", JOptionPane.ERROR_MESSAGE, null);
                return;
            }

            FirstAnalizeButton.setButtonsEnabled(false);
            FirstAnalizeButton.setEnabled(false);//para luego chequear

            sPanel = "<h2>Deadlock and S3PR analysis</h2>";

            try {
                /*
                 * Information for boundedness, safeness and deadlock
                 */
                CRTree statesTree = new CRTree(root, root.getCurrentMarking().getMarkingAsArray()[Marking.CURRENT]);

                boolean S3PR = statesTree.hasDeadlock();
                boolean Deadlock = statesTree.hasDeadlock();

                if(Deadlock ==false || S3PR==false)
                {
                    sPanel+="The net is not compatible with a deadlock supervision ,the net has to be S3PR and have a deadlock";
                    String[] treeInfo = new String[]{
                            "&nbsp&emsp &emsp&nbsp", "&emsp&emsp&emsp",
                            "S3PR", "" + Deadlock,        // ----------------------  ADD S3PR CLASSIFICATION
                            "Deadlock", "" + S3PR
                    };
                    sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
                    results.setEnabled(true);
                    results.setText(sPanel);
                    System.out.println("Soy falso");
                    return;
                }
                superviseButton.setButtonsEnabled(true);
                fixConflictButton.setButtonsEnabled(true);
                String[] treeInfo = new String[]{
                        "&nbsp&emsp &emsp&nbsp", "&emsp&emsp&emsp",
                        "S3PR", "" + Deadlock,        // ----------------------  ADD S3PR CLASSIFICATION
                        "Deadlock", "" + S3PR
                };
                sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
                //results.setEnabled(false);
                Runanalysis("1");
                close_socket();
            }
            catch(OutOfMemoryError e)
            {
                System.gc();
                results.setText("");
                sPanel = "Memory error: " + e.getMessage();

                sPanel += "<br>Not enough memory. Please use a larger heap size." +
                        "<br>" + "<br>Note:" +
                        "<br>The Java heap size can be specified with the -Xmx option." +
                        "<br>E.g., to use 512MB as heap size, the command line looks like this:" +
                        "<br>java -Xmx512m -classpath ...\n";
                results.setText(sPanel);
            }
            catch (StackOverflowError e){
                results.setText("An error has occurred, the net might have too many states...");
            }
            catch(Exception e)
            {
                e.printStackTrace();
                sPanel = "<br>Error" + e.getMessage();
                results.setText(sPanel);
            }

            results.setText(sPanel);

        }
    };
    private class SecondClassifyListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent)
        {

            // Checks if the net is valid
            if(!root.getDocument().getPetriNet().getRootSubnet().isValid()) {
                JOptionPane.showMessageDialog(null, "Invalid Net!", "Error", JOptionPane.ERROR_MESSAGE, null);
                return;
            }
            /*
            //chequeo que no se aprete primero este boton que el first analysis
            if (FirstAnalizeButton.isEnabled())
            {
                int resp = JOptionPane.showConfirmDialog(null, "En caso de ser la red original, Hacer primero 'first analysis'",//<- EL MENSAJE
                        "Alerta!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (resp==JOptionPane.OK_OPTION)
                {
                    return;
                }
            }*/
            //sigo
            FirstAnalizeButton.setButtonsEnabled(false);


            sPanel = "<h2>Deadlock and S3PR analysis</h2>";

            try {
                /*
                 * Information for boundedness, safeness and deadlock
                 */
                CRTree statesTree = new CRTree(root, root.getCurrentMarking().getMarkingAsArray()[Marking.CURRENT]);

                boolean S3PR = statesTree.hasDeadlock();
                boolean Deadlock = statesTree.hasDeadlock();

                if(Deadlock ==false || S3PR==false)
                {
                    sPanel+="The net is not compatible with a deadlock supervision ,the net has to be S3PR and have a deadlock";
                    String[] treeInfo = new String[]{
                            "&nbsp&emsp &emsp&nbsp", "&emsp&emsp&emsp",
                            "S3PR", "" + Deadlock,        // ----------------------  ADD S3PR CLASSIFICATION
                            "Deadlock", "" + S3PR
                    };
                    sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
                    results.setEnabled(true);
                    results.setText(sPanel);
                    superviseButton.setButtonsEnabled(false);
                    fixConflictButton.setButtonsEnabled(false);
                    return;
                }
                superviseButton.setButtonsEnabled(true);
                fixConflictButton.setButtonsEnabled(true);
                SecondAnalizeButton.setButtonsEnabled(false);
                String[] treeInfo = new String[]{
                        "&nbsp&emsp &emsp&nbsp", "&emsp&emsp&emsp",
                        "S3PR", "" + Deadlock,        // ----------------------  ADD S3PR CLASSIFICATION
                        "Deadlock", "" + S3PR
                };
                sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
                //results.setEnabled(false);
                Runanalysis("2");
                close_socket();
            }
            catch(OutOfMemoryError e)
            {
                System.gc();
                results.setText("");
                sPanel = "Memory error: " + e.getMessage();

                sPanel += "<br>Not enough memory. Please use a larger heap size." +
                        "<br>" + "<br>Note:" +
                        "<br>The Java heap size can be specified with the -Xmx option." +
                        "<br>E.g., to use 512MB as heap size, the command line looks like this:" +
                        "<br>java -Xmx512m -classpath ...\n";
                results.setText(sPanel);
            }
            catch (StackOverflowError e){
                results.setText("An error has occurred, the net might have too many states...");
            }
            catch(Exception e)
            {
                e.printStackTrace();
                sPanel = "<br>Error" + e.getMessage();
                results.setText(sPanel);
            }

            results.setText(sPanel);
        }
    };
    //Added supervisors end
    public void EndSupervision()
    {
        superviseButton.setButtonsEnabled(false);
        sPanel = "<h2>Added Supervisors</h2>";

        String[] treeInfo = new String[]{
                "&nbsp&emsp &emsp&nbsp", "&emsp&emsp&emsp",       // ----------------------  ADD S3PR CLASSIFICATION
                "Deadlock", "false"
        };
        sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
        results.setEnabled(true);
        results.setText(sPanel);
        //System.out.println(root.getCurrentFile().getPath());
        String Respuesta="";

            try {
                outw.writeUTF("quit");
                outw.flush();
                Respuesta =inw.readUTF();
                System.out.println ("Respuesta:");
                System.out.println (Respuesta);
                outw.close();
                inw.close();
                server.close();
                //proceso.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }

    }


    //Listener boton add supervisor
    private class AddSupervisorListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent) {
            //Runanalysis("2");
            execServer();
            String[] choices;
            //sPanel = "<h2>Add Supervisors</h2>";
            //results.setText(sPanel);
            String Respuesta;
            try {
                outw.writeUTF("S");
                outw.flush();
                Respuesta = inw.readUTF();
                System.out.println("Respuesta:");
                System.out.println(Respuesta);
                //PIDO ID
                //String id = JOptionPane.showInputDialog("Indicar ID");
                choices = Respuesta.split(" ");
                String id = (String) JOptionPane.showInputDialog(null, "Choose now...",
                        "Indicar ID", JOptionPane.QUESTION_MESSAGE, null,
                        choices, // Array of choices
                        choices[0]); // Initial choice
                if (id == null)//chequeo si se toco cancelar o cerrar
                {
                    close_socket();
                    return;
                }
                outw.writeUTF(id);
                outw.flush();
                Respuesta = inw.readUTF();
                System.out.println("Respuesta:");
                System.out.println(Respuesta);
                //hasta aca
                reSaveNet();
                String[] treeInfo = new String[]{
                        Respuesta
                };
                sPanel = "<h2>Added supervisors , run analysis for more details</h2>";
                sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
                results.setText(sPanel);
                results.setEnabled(true);
                close_socket();
                superviseButton.setButtonsEnabled(false);
                fixConflictButton.setButtonsEnabled(false);
                SecondAnalizeButton.setButtonsEnabled(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    };
    //Listener boton add supervisor
    private class FixConflictListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent)
        {
            //Runanalysis("2");
            execServer();
            String Respuesta;
            try {
                outw.writeUTF("3");
                outw.flush();
                Respuesta =inw.readUTF();
                System.out.println ("Respuesta:");
                System.out.println (Respuesta);
                String[] treeInfo = new String[]{
                        Respuesta
                };
                sPanel = "<h2>Conflicts Results, run analysis for more details</h2>";
                sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
                results.setText(sPanel);
                results.setEnabled(true);
                reSaveNet();
                close_socket();
                fixConflictButton.setButtonsEnabled(false);
                superviseButton.setButtonsEnabled(false);
                SecondAnalizeButton.setButtonsEnabled(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    };
        //Function to save and reload net when the supervisor is added
        public void reSaveNet() {

            FileType chosenFileType = (FileType) new PflowFileType();
            List<FileType> fileTypes = new LinkedList<>();
            fileTypes.add(chosenFileType);
            SaveAction guardar = new SaveAction(this.root, fileTypes);
            //guardar.actionPerformed(null);
            ReloadFileAction reload = new ReloadFileAction(this.root, fileTypes);
            reload.actionPerformed(null);

        }

        public String getOsName() {
            return System.getProperty("os.name");
        }

        //Get actual absolute executed .jar path
        public String get_Current_JarPath()
        {
            String pathNet = SupervisionAction.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            pathNet = pathNet.substring(0, pathNet.lastIndexOf("/"));
            if (getOsName().startsWith("Windows") && pathNet.startsWith("/"))
                pathNet = pathNet.substring(1, pathNet.length());
            String decodedPath = null;
            try {
                decodedPath = URLDecoder.decode(pathNet, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            //System.out.println("Jar path : " + decodedPath);
            return decodedPath;
        }

}
