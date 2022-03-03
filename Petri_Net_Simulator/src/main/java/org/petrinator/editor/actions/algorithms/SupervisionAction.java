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
import org.petrinator.editor.actions.algorithms.reachability.CRTree;
import org.petrinator.editor.filechooser.*;
import org.petrinator.petrinet.*;
import org.petrinator.util.GraphicsTools;
import org.petrinator.util.S3pr;
import org.petrinator.util.Save;

import pipe.gui.widgets.ButtonBar;
import pipe.gui.widgets.EscapableDialog;
import pipe.gui.widgets.ResultsHTMLPane;
import pipe.modules.minimalSiphons.MinimalSiphons;
import pipe.utilities.math.Matrix;
import pipe.views.PetriNetView;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.net.*;
import java.io.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.event.*;

public class SupervisionAction extends AbstractAction
{
    private static final String MODULE_NAME = "Deadlock Supervisor";
    private ResultsHTMLPane results;
    private String sPanel;
    private String S3PRresults;
    private Root root;
    private JDialog guiDialog;
    private ButtonBar FirstAnalizeButton;
    private ButtonBar SecondAnalizeButton;
    private ButtonBar superviseButton;
    private ButtonBar fixConflictButton;
    private ButtonBar HelpButton;
    InvariantAction accion;
    MatricesAction matrices;
    ReachabilityAction states;

    Process process = null;
    PrintStream outw = null;
    BufferedReader inw = null;

    public SupervisionAction(Root root)
    {
        this.root = root;
        putValue(NAME, MODULE_NAME);
        putValue(SHORT_DESCRIPTION, MODULE_NAME);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/deadlock.png"));

        guiDialog =  new JDialog(root.getParentFrame(), MODULE_NAME, true);
        guiDialog.setModalityType(Dialog.ModalityType.MODELESS);
        Container contentPane = guiDialog.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

        results = new ResultsHTMLPane("");
        sPanel = new String();//new
        S3PRresults = new String();
        contentPane.add(results);
        FirstAnalizeButton = new ButtonBar("First Analysis", new ClassifyListener(), guiDialog.getRootPane());
        SecondAnalizeButton = new ButtonBar("Net Analysis (with supervisors)", new SecondClassifyListener(), guiDialog.getRootPane());
        superviseButton = new ButtonBar("Add Supervisor/s", new AddSupervisorListener(), guiDialog.getRootPane());
        fixConflictButton = new ButtonBar("Fix Conflict/s", new FixConflictListener(), guiDialog.getRootPane());
        HelpButton = new ButtonBar("Help", new HelpListener(), guiDialog.getRootPane());
        // closing window listener
        guiDialog.addWindowListener(new WindowAdapter() 
        {
            public void windowClosed(WindowEvent e)
            {
                closeProc();
            }

            public void windowClosing(WindowEvent e)
            {
                closeProc();
            }
        });

        JPanel checkPanel = new JPanel(new GridLayout(2,2));
        //agrego al nuevo panel
        checkPanel.add(FirstAnalizeButton);
        checkPanel.add(superviseButton);
        checkPanel.add(SecondAnalizeButton);
        checkPanel.add(fixConflictButton);
        //termino de agregar al nuevo panel
        contentPane.add(checkPanel);
        contentPane.add(HelpButton);
        //creo un objeto de invariantes
        accion = new InvariantAction(this.root);
        matrices = new MatricesAction(this.root);
        states = new ReachabilityAction(this.root);

    }
    

    private String readInput()
    {
        String Respuesta = "";
        
        String line;
        try {
            // Blocking read
            Respuesta = inw.readLine();
            while(inw.ready() && (line = inw.readLine()) != null) 
            {
                Respuesta+=inw.readLine();
            }
            //Respuesta = inw.readLine();
        } 
        
        catch(Exception e)
        {
            e.printStackTrace();
            results.setText("");
            JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error reading python module input buffer", JOptionPane.ERROR_MESSAGE); 
            guiDialog.setVisible(false); 
            return null;
        }
        if(Respuesta.contains("Error : "))
        {
/*             try {
                //outw.close();
                //inw.close();
                //server.close();
                //proceso.destroy();
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
                results.setText("");
                JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error in python module", JOptionPane.ERROR_MESSAGE); 
                guiDialog.setVisible(false);
                return null;
            } */

            results.setText("");
            JOptionPane.showMessageDialog(root.getParentFrame(),Respuesta, "Error in python module", JOptionPane.ERROR_MESSAGE); 
            guiDialog.setVisible(false);
            return null;
        }
        return Respuesta;
    }



    public void actionPerformed(ActionEvent e)
    {
        if(!root.getDocument().getPetriNet().getRootSubnet().isValid())
        {
            results.setText("Invalid Net");
            JOptionPane.showMessageDialog(null, "Invalid Net!", "Error analysing net", JOptionPane.ERROR_MESSAGE, null);
            guiDialog.setVisible(false);
            return;
        }
        ArrayList<String> controlPlaces = root.getDocument().getPetriNet().getControlPlaces();
        results.setText("");

        // Disables the copy and save buttons
        results.setEnabled(false);

        // Enables classify button
        if(controlPlaces.isEmpty())
        {
            FirstAnalizeButton.setButtonsEnabled(true);
            SecondAnalizeButton.setButtonsEnabled(false);
            FirstAnalizeButton.setToolTipText("The net doesn't have any supervisors");
            //FirstAnalizeButton.setEnabled(true);//para luego chequear
        }
        else{
            FirstAnalizeButton.setButtonsEnabled(false);
            SecondAnalizeButton.setButtonsEnabled(true);
            SecondAnalizeButton.setToolTipText("The net has supervisors");
        }
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
    private int Runanalysis(String message)
    {
        if( (invariantAnalysis()==1) || (matricesAnalysis()==1)|| (coverabilityAnalysis()==1)|| 
            (sifonnalysis()==1) || (saveNet()==1) || (remoteExecute(message,true)==1) )
            return 1;//Supervision analysis
        return 0;
    }
 
    // Executes tesis.py and get the response using sockets
    private int remoteExecute(String message,boolean read_response)
    {
        int port=0;
        String Respuesta;
        Socket cli = null;

        try {

            //server = new ServerSocket(0);
            //port = server.getLocalPort();

            //Get tesis python path and execute
            String  jar_path = Save.get_Current_JarPath(SupervisionAction.class,root,results);
            String pathToPythonMain ;
            if(jar_path!=null)pathToPythonMain= jar_path +"/Modulos/Deadlock-supervisor/tesis.py";
            else return 1;

            ProcessBuilder pb = new ProcessBuilder("python3", pathToPythonMain, root.getCurrentFile().getPath(),jar_path);
            process = pb.start();
            
            //System.out.println("python3 '" + pathToPythonMain + "' "+ String.valueOf(port) + " '" + root.getCurrentFile().getPath() + "'");


            //Blocking accept executed python client
            //cli = server.accept();
            //Instantiate input and output socket buffers
            //outw = new DataOutputStream(cli.getOutputStream());
            //inw = new DataInputStream(cli.getInputStream());
            
            inw = new BufferedReader(new InputStreamReader(process.getInputStream()));
            outw = new PrintStream(process.getOutputStream());
            BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            outw.println(message); 
            outw.flush();
            
        } catch (IOException e) {
            results.setText("");
            e.printStackTrace();
            JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error executing python module", JOptionPane.ERROR_MESSAGE); 
            return 1;
        }

            //
   

            if(read_response)
            {
                System.out.println("waitning for process response ----------------------");
                Respuesta = readInput();
                if(Respuesta==null)return 1;
                String[] treeInfo = new String[]{
                        Respuesta
                };
                sPanel += "<h2>Net Anylisis Results</h2>";
                sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
                results.setText(sPanel);
                results.setEnabled(true);


                superviseButton.setButtonsEnabled(true);
            }
            
        return 0;
    }

    //Function to save the current net in a temp.pflow file for later supervision analisys
    public int saveNet() {

        FileChooserDialog chooser = new FileChooserDialog();
        chooser.setVisible(false);
        chooser.setAcceptAllFileFilterUsed(false);

        String Temp_net_path;
        String jar_path = Save.get_Current_JarPath(SupervisionAction.class,root,results);
        if(jar_path != null)Temp_net_path = jar_path + "/Modulos/Deadlock-supervisor/tmp/net.pflow";
        else return 1;

        File dir = new File(String.valueOf(jar_path + "/Modulos/Deadlock-supervisor/tmp"));
        if (!dir.exists()) dir.mkdirs();
        File file = new File(Temp_net_path);
        
        FileType chosenFileType = (FileType) new PflowFileType();
        try {
            chosenFileType.save(root.getDocument(), file);
        } catch (FileTypeException ex) {
            results.setText("");
            JOptionPane.showMessageDialog(root.getParentFrame(),ex.getMessage(), "Error saving net", JOptionPane.ERROR_MESSAGE); 
            ex.printStackTrace();
            return 1;
        }
        return 0;
    }
    /*
     Do Siphon's and trap's analisys and saves it in html extension
  */
    public int sifonnalysis()
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

        File dir = new File(Save.get_Current_JarPath(SupervisionAction.class,root,results) + "/tmp");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(Save.get_Current_JarPath(SupervisionAction.class,root,results) + "/tmp/" + "tmp" + "." + "pnml");
        FileType chosenFileType = (FileType) chooser.getFileFilter();
        try {
            chosenFileType.save(root.getDocument(), file);
        } catch (FileTypeException e1) {
            results.setText("");
            JOptionPane.showMessageDialog(root.getParentFrame(),e1.getMessage(), "Error executing siphons analysis", JOptionPane.ERROR_MESSAGE); 
            e1.printStackTrace();
            return 1;
        }
        /*
         * Read tmp file
         */
        PetriNetView sourceDataLayer = new PetriNetView(Save.get_Current_JarPath(SupervisionAction.class,root,results) + "/tmp/tmp.pnml");
        String s = "<h2>Siphons and Traps</h2>";

        if (sourceDataLayer == null) {
            return 1;
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
                return 1;
            } catch (Exception e) {
                e.printStackTrace();
                s = "<br>Error" + e.getMessage();
                results.setText(s);
                return 1;
            }
        }
        results.setText(s);
        if(SaveHTML("sif") == 1)return 1;
        return 0;
    }
    /*
       Do the coverability analisys and saves it in html extension
    */
    public int coverabilityAnalysis()
    {
        // Checks if the net is valid
        if (!root.getDocument().getPetriNet().getRootSubnet().isValid()) {
            results.setText("");
            JOptionPane.showMessageDialog(null, "Invalid Net!", "Error executing coverability analysis", JOptionPane.ERROR_MESSAGE, null);
            return 1;
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
            return 1;
        }

        results.setText(log);
        if(SaveHTML("cov") == 1)return 1;
        return 0;
    }
    /*
        Do the invariant analisys and saves it in html extension
     */
    public int invariantAnalysis()
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
                return 1;
            }
            catch(Exception e)
            {
                e.printStackTrace();
                s = "<br>Error" + e.getMessage();
                results.setText(s);
                return 1;
            }
        }
        results.setText(s);
        if(SaveHTML("inv") == 1)return 1;
        return 0;
    }
    /*
        Get matrices and saves it in html extension
     */
    public int matricesAnalysis()
    {
        // Checks if the net is valid
        if(!root.getDocument().getPetriNet().getRootSubnet().isValid()) {
            results.setText("");
            JOptionPane.showMessageDialog(null, "Invalid Net!", "Error executing matrices analysis", JOptionPane.ERROR_MESSAGE, null);
            return 1;
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
            return 1;
        }
        catch(Exception e)
        {
            s = "<br>Invalid net";
            results.setText(s);
            return 1;
        }

        results.setText(s);

        // Enables the copy and save buttons
        results.setEnabled(false);
        if(SaveHTML("mat") == 1)return 1;
        return 0;
    }

    /*
        SAVE AS HTML
     */
    private int SaveHTML(String name)
    {
        String save_path="";
        try
        {
            String  jar_path = Save.get_Current_JarPath(SupervisionAction.class,root,results);

            File dir = new File(String.valueOf(jar_path + "/Modulos/Deadlock-supervisor/tmp"));
            if (!dir.exists()) dir.mkdirs();

            if(jar_path!= null) save_path= jar_path +"/Modulos/Deadlock-supervisor/tmp/"+ name +".html";
            else return 1;

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
            results.setText("");
            JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(),  "Error saving temporary files", JOptionPane.ERROR_MESSAGE); 
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    public int closeProc()
    {

        outw.println("quit");
        outw.flush();
        String Respuesta = readInput();
        System.out.println("Respuesta: " + Respuesta);
        //outw.close();
        //inw.close();
        //server.close();
        //proceso.destroy();

        //cierro sockets y streams
        return 0;
    }
    /**
     * Listener for analyse button
     */
    private class ClassifyListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent)
        {
            System.out.println("----- Running Supervision Analysis -----");
            // Checks if the net is valid
            if(!root.getDocument().getPetriNet().getRootSubnet().isValid()) {
                results.setText("3");
                JOptionPane.showMessageDialog(null, "Invalid Net!", "Error analysing net", JOptionPane.ERROR_MESSAGE, null);
                guiDialog.setVisible(false);
                //closeProc();
                return;
            }

            FirstAnalizeButton.setButtonsEnabled(false);
            //FirstAnalizeButton.setEnabled(false);//para luego chequear

            sPanel = "<h2>Deadlock and S3PR analysis</h2>";

            try {
                /*
                 * Information for boundedness, safeness and deadlock
                 */
                CRTree statesTree = new CRTree(root, root.getCurrentMarking().getMarkingAsArray()[Marking.CURRENT]);

                boolean S3PR = S3pr.isS3PR(accion,root,S3PRresults);
                boolean Deadlock = statesTree.hasDeadlock();

                if(Deadlock ==false || S3PR==false)
                {
                    sPanel+="The net is not compatible with a deadlock supervision ,the net has to be S3PR and have a deadlock";
                    String[] treeInfo = new String[]{
                            "&nbsp&emsp &emsp&nbsp", "&emsp&emsp&emsp",
                            "S3PR", "" + S3PR,        // ----------------------  ADD S3PR CLASSIFICATION
                            "Deadlock", "" + Deadlock
                    };
                    sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
                    if(!S3PR)
                        sPanel+= S3PRresults;
                    results.setEnabled(true);
                    results.setText(sPanel);
                    return;
                }
                superviseButton.setButtonsEnabled(true);
                fixConflictButton.setButtonsEnabled(true);
                String[] treeInfo = new String[]{
                        "&nbsp&emsp &emsp&nbsp", "&emsp&emsp&emsp",
                        "S3PR", "" + S3PR,        // ----------------------  ADD S3PR CLASSIFICATION
                        "Deadlock", "" + Deadlock
                };
                sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
                if(Runanalysis("1")==1 )
                {
                    guiDialog.setVisible(false);
                    //closeProc();
                    return;
                }
                //closeProc();
                
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

    // Analyse2 button (with supervisors)
    private class SecondClassifyListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent)
        {

            System.out.println("----- Running Supervision Analysis (w/supervisors) -----");
            // Checks if the net is valid
            if(!root.getDocument().getPetriNet().getRootSubnet().isValid()) {
                results.setText("");
                JOptionPane.showMessageDialog(null, "Invalid Net!", "Error analysing net", JOptionPane.ERROR_MESSAGE, null);
                guiDialog.setVisible(false);
                return;
            }

            FirstAnalizeButton.setButtonsEnabled(false);
            FirstAnalizeButton.setToolTipText("The net has places control");


            sPanel = "<h2>Deadlock analysis</h2>";

            try {
                /*
                 * Information for boundedness, safeness and deadlock
                 */
                CRTree statesTree = new CRTree(root, root.getCurrentMarking().getMarkingAsArray()[Marking.CURRENT]);

                boolean S3PR = true;//isS3PR();
                boolean Deadlock = statesTree.hasDeadlock();

                if(Deadlock ==false || S3PR==false)
                {
                    sPanel+="The net is not compatible with a deadlock supervision ,the net has to be S3PR and have a deadlock";
                    String[] treeInfo = new String[]{
                            "&nbsp&emsp &emsp&nbsp", "&emsp&emsp&emsp",
                            //"S3PR", "" + S3PR,        // ----------------------  ADD S3PR CLASSIFICATION
                            "Deadlock", "" + Deadlock
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
                        //"S3PR", "" + S3PR,        // ----------------------  ADD S3PR CLASSIFICATION
                        "Deadlock", "" + Deadlock
                };
                sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);

                if(Runanalysis("2")==1)
                {
                    guiDialog.setVisible(false);
                    //closeProc();
                    return;
                }
                //closeProc();
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
                guiDialog.setVisible(false);
                //closeProc();
            }
            catch (StackOverflowError e){
                results.setText("An error has occurred, the net might have too many states...");
                guiDialog.setVisible(false);
                //closeProc();
            }
            catch(Exception e)
            {
                e.printStackTrace();
                sPanel = "<br>Error" + e.getMessage();
                results.setText(sPanel);
                guiDialog.setVisible(false);
                //closeProc();
            }

            results.setText(sPanel);
        }
    };


    //Listener for add supervisor button
    private class AddSupervisorListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent) {
            //Runanalysis("2");
            System.out.println("----- Running Add Supervisor Analysis ------\n");
/*             if(execServer()==1)
            {
                guiDialog.setVisible(false);
                return;
            } */
            remoteExecute("S",false);
            String[] choices;

            String Respuesta;

                //writeout("S");
                //outw.flush();
                Respuesta = readInput();
                if(Respuesta==null)return;
                //PIDO ID
                System.out.println("received problematic siphons " + Respuesta + " \n");
                choices = Respuesta.split(" ");
                String id;
                do{

                    id = (String) JOptionPane.showInputDialog(null, "Choose now...",
                            "Indicar ID", JOptionPane.QUESTION_MESSAGE, null,
                            choices, // Array of choices
                            choices[0]); // Initial choice
                    if (id == null)
                        break;

                    if(results.getText().contains("id="+id+"<br>Marcado_del_supervisor:0"))
                    {
                        JOptionPane.showMessageDialog(null, "The supervisor : " +id +" is invalid",
                                "Warning: Help for more information!", JOptionPane.ERROR_MESSAGE);
                    }
                    else
                        break;

                }while(results.getText().contains("id="+id+"<br>Marcado_del_supervisor:0"));

                if (id == null)//chequeo si se toco cancelar o cerrar
                {
                    //closeProc();
                    return;
                }

                outw.println(id);
                outw.flush();
                
                Respuesta = readInput();
                if(Respuesta==null)return;
                //hasta aca
                Save.reSaveNet(root);
                String[] treeInfo = new String[]{
                        Respuesta
                };
                sPanel = "<h2>Added supervisors , run analysis for more details</h2>";
                sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
                results.setText(sPanel);
                results.setEnabled(true);
                //closeProc();
                superviseButton.setButtonsEnabled(false);
                fixConflictButton.setButtonsEnabled(false);
                SecondAnalizeButton.setButtonsEnabled(true);

            return;
        }

    };
    //Listener for fix conflict button
    private class FixConflictListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent)
        {
            System.out.println("----- Running Fix Conflict Analysis ------\n");
/*             if(execServer()==1)
            {
                guiDialog.setVisible(false);
                return;
            } */
            remoteExecute("3",false);
            String Respuesta;
                //writeout("3");
                Respuesta = readInput();
                String[] treeInfo = new String[]{
                        Respuesta
                };
                sPanel = "<h2>Conflicts Results, run analysis for more details</h2>";
                sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
                results.setText(sPanel);
                results.setEnabled(true);
                Save.reSaveNet(root);
                //closeProc();
                fixConflictButton.setButtonsEnabled(false);
                superviseButton.setButtonsEnabled(false);
                SecondAnalizeButton.setButtonsEnabled(true);

            return;
        }

    };
    //Function to save and reload net when the supervisor is added

    private class HelpListener implements ActionListener {
        public void actionPerformed(ActionEvent e)
        {
            /*
             * Create the dialog
             */
            EscapableDialog guiDialog = new EscapableDialog(root.getParentFrame(), "Help: Deadlock Supervisor", false);
            Container contentPane = guiDialog.getContentPane();
            org.petrinator.auxiliar.ResultsHTMLPane results = new org.petrinator.auxiliar.ResultsHTMLPane("");
            contentPane.add(results);
            guiDialog.pack();
            guiDialog.setAlwaysOnTop(true);
            guiDialog.setLocationRelativeTo(root.getParentFrame());
            guiDialog.setVisible(true);

            /*
             * Read the about.html file
             */
            InputStream aboutFile = getClass().getResourceAsStream("/DeadlockSupervisorHelp.html");
            Scanner scanner = null;
            scanner = new Scanner(aboutFile, "UTF-8");
            String s = scanner.useDelimiter("\\Z").next();
            scanner.close();

            results.setText(s);
        }
    };



}
