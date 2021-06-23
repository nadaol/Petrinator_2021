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
import scala.Array;

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
import org.apache.commons.io.FileUtils;


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



    public String catch_error()
    {
        String Respuesta = "";
        try {
            Respuesta = inw.readUTF();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            results.setText("");
            JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error reading python module input buffer", JOptionPane.ERROR_MESSAGE); 
            guiDialog.setVisible(false); 
            return null;
        }

        //System.out.println ("Respuesta:" + Respuesta);
        if(Respuesta.startsWith("Error"))
        {
            try {
                outw.close();
                inw.close();
                server.close();
                //proceso.destroy();
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
                results.setText("");
                JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error in python module", JOptionPane.ERROR_MESSAGE); 
                guiDialog.setVisible(false);
                return null;
            }

            results.setText("");
            JOptionPane.showMessageDialog(root.getParentFrame(),Respuesta, "Error in python module", JOptionPane.ERROR_MESSAGE); 
            guiDialog.setVisible(false);
            return null;
        }
        return Respuesta;
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
    public int Runanalysis(String message)
    {
        //JOptionPane.showMessageDialog(null, "llego al run alanisis", "Error", JOptionPane.ERROR_MESSAGE, null);
        if( (invariantAnalysis()==1) || (matricesAnalysis()==1)|| (coverabilityAnalysis()==1)|| 
            (sifonnalysis()==1) || (saveNet()==1) || (socketServer(message)==1) )
            return 1;//Supervision analysis
        return 0;
    }
    public int execServer()
    {
        int port=0;
        Socket cli = null;

        try {
            server = new ServerSocket(0);
            port = server.getLocalPort();

            //Get tesis python path and execute
            String  jar_path = get_Current_JarPath();
            String pathToPythonMain;
            if(jar_path!= null)pathToPythonMain = jar_path +"/Modulos/Deadlock-supervisor/tesis.py";
            else return 1;

            ProcessBuilder pb = new ProcessBuilder("python3", pathToPythonMain,String.valueOf(port), root.getCurrentFile().getPath(),jar_path);
            pb.start();
            //System.out.println("python3 '" + pathToPythonMain + "' "+ String.valueOf(port) + " '" + root.getCurrentFile().getPath() + "'");

            //Blocking accept executed python client
            cli = server.accept();
            //Instantiate input and output socket buffers
            outw = new DataOutputStream(cli.getOutputStream());
            inw = new DataInputStream(cli.getInputStream());

        } catch (IOException e) {
            results.setText("");
            e.printStackTrace();
            JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error executing python module", JOptionPane.ERROR_MESSAGE); 
            return 1;
        }
        return 0;
    }
    // Executes tesis.py and get the response using sockets
    public int socketServer(String message)
    {
        int port=0;
        String Respuesta;
        Socket cli = null;

        try {

            server = new ServerSocket(0);
            port = server.getLocalPort();

            //Get tesis python path and execute
            String  jar_path = get_Current_JarPath();
            String pathToPythonMain ;
            if(jar_path!=null)pathToPythonMain= jar_path +"/Modulos/Deadlock-supervisor/tesis.py";
            else return 1;

            ProcessBuilder pb = new ProcessBuilder("python3", pathToPythonMain,String.valueOf(port), root.getCurrentFile().getPath(),jar_path);
            pb.start();
            //System.out.println("python3 '" + pathToPythonMain + "' "+ String.valueOf(port) + " '" + root.getCurrentFile().getPath() + "'");

            //Blocking accept executed python client
            cli = server.accept();
            //Instantiate input and output socket buffers
            outw = new DataOutputStream(cli.getOutputStream());
            inw = new DataInputStream(cli.getInputStream());

        } catch (IOException e) {
            results.setText("");
            e.printStackTrace();
            JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error executing python module", JOptionPane.ERROR_MESSAGE); 
            return 1;
        }

        try {
            outw.writeUTF(message);
            outw.flush();
            Respuesta = catch_error();
            if(Respuesta==null)return 1;
            String[] treeInfo = new String[]{
                    Respuesta
            };
            sPanel += "<h2>Net Anylisis Results</h2>";
            sPanel += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
            results.setText(sPanel);
            results.setEnabled(true);

        } catch (IOException e) {
            results.setText("");
            e.printStackTrace();
            JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error in python module communication", JOptionPane.ERROR_MESSAGE); 
            return 1;
        }
        superviseButton.setButtonsEnabled(true);
        return 0;
    }

    //Function to save the current net in a temp.pflow file for later supervision analisys
    public int saveNet() {

        FileChooserDialog chooser = new FileChooserDialog();
        chooser.setVisible(false);
        chooser.setAcceptAllFileFilterUsed(false);

        
        String Temp_net_path;
        String jar_path = get_Current_JarPath();
        if(jar_path != null)Temp_net_path = jar_path + "/Modulos/Deadlock-supervisor/tmp/net.pflow";
        else return 1;

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

        File file = new File(get_Current_JarPath() + "/tmp/" + "tmp" + "." + "pnml");
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
        PetriNetView sourceDataLayer = new PetriNetView(get_Current_JarPath() + "/tmp/tmp.pnml");
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
    public int SaveHTML(String name)
    {
        String save_path="";
        try
        {
            String  jar_path = get_Current_JarPath();
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

    public int close_socket()
    {
        try {
            outw.writeUTF("quit");
            outw.flush();
            String Respuesta =inw.readUTF();
            System.out.println ("Respuesta: " + Respuesta);
            outw.close();
            inw.close();
            server.close();
            //proceso.destroy();
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
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
                close_socket();
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

                boolean S3PR = isS3PR();
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
                //results.setEnabled(false);
                if(Runanalysis("1")==1 )
                {
                    guiDialog.setVisible(false);
                    close_socket();
                    return;
                }
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
                    close_socket();
                    return;
                }
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
                guiDialog.setVisible(false);
                close_socket();
            }
            catch (StackOverflowError e){
                results.setText("An error has occurred, the net might have too many states...");
                guiDialog.setVisible(false);
                close_socket();
            }
            catch(Exception e)
            {
                e.printStackTrace();
                sPanel = "<br>Error" + e.getMessage();
                results.setText(sPanel);
                guiDialog.setVisible(false);
                close_socket();
            }

            results.setText(sPanel);
        }
    };


    //Listener for add supervisor button
    private class AddSupervisorListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent) {
            //Runanalysis("2");
            System.out.println("----- Running Add Supervisor Analysis ------\n");
            if(execServer()==1)
            {
                guiDialog.setVisible(false);
                return;
            }
            String[] choices;
            //sPanel = "<h2>Add Supervisors</h2>";
            //results.setText(sPanel);
            String Respuesta;
            try {
                outw.writeUTF("S");
                outw.flush();
                Respuesta = catch_error();
                if(Respuesta==null)return;
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
                Respuesta = catch_error();
                if(Respuesta==null)return;
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
                results.setText("");
                e.printStackTrace();
                JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error during net supervision", JOptionPane.ERROR_MESSAGE); 
                guiDialog.setVisible(false);
                close_socket();
                return;
            }
            return;
        }

    };
    //Listener for fix conflict button
    private class FixConflictListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent)
        {
            //Runanalysis("2");
            System.out.println("----- Running Fix Conflict Analysis ------\n");
            if(execServer()==1)
            {
                guiDialog.setVisible(false);
                return;
            }
            String Respuesta;
            try {
                outw.writeUTF("3");
                outw.flush();
                Respuesta =inw.readUTF();
                //System.out.println ("Respuesta:");
                //System.out.println (Respuesta);
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
                results.setText("");
                e.printStackTrace();
                JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error resolving net conflicts", JOptionPane.ERROR_MESSAGE); 
                guiDialog.setVisible(false);
                close_socket();
                return;
            }
            return;
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
            results.setText("");
            JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error obtaining absolute jar path", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
        //System.out.println("Jar path : " + decodedPath);
        return decodedPath;
    }

    //S3PR classification
    public boolean isS3PR()
    {
        System.out.println("----- Running S3PR Analysis -----\n");
        int[][] IncidenceMatrix = root.getDocument().getPetriNet().getIncidenceMatrix();
        Matrix TInvariants = accion.findVectors(new Matrix(root.getDocument().getPetriNet().getIncidenceMatrix()));
        TInvariants.transpose();
        System.out.println("There are "+ TInvariants.getColumnDimension()+ " T-invariants\n");

        //1°Checheo que haya mas de 1 T-invariante
        if(!check_num_Tinvariants(TInvariants))return false;
        System.out.println("1° CHECK: there is more than one T-Invariants");
        //creo un hashmap con las transiciones de los tinvariantes y las plazas de los t invariantes
        Map<String,ArrayList<Integer>> Tinvariants_trans = new LinkedHashMap<String,ArrayList<Integer>>();
        Map<String,ArrayList<Integer>> Tinvariants_places = new LinkedHashMap<String,ArrayList<Integer>>();
        Map<String,ArrayList<Integer>> Tinvariants_SM_places = new LinkedHashMap<String,ArrayList<Integer>>();
        Map<String,ArrayList<Integer>> Tinvariants_SM_trans = new LinkedHashMap<String,ArrayList<Integer>>();
        Map<String,ArrayList<Integer>> Tinvariants_resources = new LinkedHashMap<String,ArrayList<Integer>>();
        Map<String,ArrayList<Integer>> Tinvariants_shared_resoruces = new LinkedHashMap<String,ArrayList<Integer>>();

        get_tinv_trans_and_places(IncidenceMatrix,TInvariants,Tinvariants_trans, Tinvariants_places);

        ArrayList<int[][]> Tinv_incidence_matrices = get_tinvariants_incidences_matrices(IncidenceMatrix, Tinvariants_trans, Tinvariants_places);

        print_hashmap(Tinvariants_trans,"T-Invariants Transitions");
        print_hashmap(Tinvariants_places,"T-Invariants Places");

        //print_matrix(Tinv_incidence_matrices.get(0),"Incidence matrix of Tinv 1");
        //print_matrix(Tinv_incidence_matrices.get(1),"Incidence matrix of Tinv 2");

        if(!check_closed_Tinvariants(Tinv_incidence_matrices,Tinvariants_trans,Tinvariants_places,Tinvariants_SM_places,Tinvariants_SM_trans))return false;
        System.out.println("2° CHECK: All T-Invariants are closed paths, potential State Machines");
        print_hashmap(Tinvariants_SM_places,"T-Invariants loop Places");
        print_hashmap(Tinvariants_SM_trans,"T-invariant loop Transitions");

        if(!check_Tinvariants_SM(Tinv_incidence_matrices,Tinvariants_places,Tinvariants_SM_places,Tinvariants_trans,Tinvariants_SM_trans,Tinvariants_resources))return false;
        System.out.println("3° CHECK: All T-Invariants are State Machines");
        System.out.println("4° CHECK: All Resources have marking greater than or equal to one");
        print_hashmap(Tinvariants_resources,"T-invariant resources");

        if(!get_shared_places(Tinvariants_resources,Tinvariants_shared_resoruces))return false;
        System.out.println("5° CHECK: All T-Invariants have Shared Resources");
        print_hashmap(Tinvariants_shared_resoruces,"T-Invariants Shared Resources");
        //print_arraylist(getEnabledTransitions(),"Enabled transitions");
        //aca verifica el marcado de las plazas del state machine y las plazas IDLE
        if(!check_SM_places_and_pidle_marking(Tinvariants_SM_places))return false;
        System.out.println("6° CHECK: All State Machine places have marking zero");
        System.out.println("7° CHECK: All Idles Places have marking greater than or equal to one");
        return true;
    }

// ----------  S3PR CLASSIFICATION FUNCTIONS  ----------

public boolean check_SM_places_and_pidle_marking(Map<String,ArrayList<Integer>> Tinvariants_SM_places)
{
    int Initial_marking[] = get_initial_marking();
    int Tinv_number = 1;
    int numPlaces;
    for (ArrayList<Integer> tinv : Tinvariants_SM_places.values())
    {
        numPlaces=0;
        for (Integer places : tinv)
        {
            if(Initial_marking[places-1]>0 && !(numPlaces==tinv.size()-1))
            {
                System.out.println("The place "+ places + " of de Tinv " + Tinv_number+" have marking and isn´t the idle");
                S3PRresults="<br>The net isn't S3PR because: The place "+ places + " of de Tinv " + Tinv_number+" is marked but isn´t the idle place";
                return false;
            }
            if( numPlaces==tinv.size()-1 && Initial_marking[places-1]==0)
            {
                System.out.println("The place idle "+ places + " of de Tinv " + Tinv_number+" must have marked");
                S3PRresults="<br>The net isn't S3PR because: The place idle"+ places + " of de Tinv " + Tinv_number+" needs to be marked";
                return false;
            }
            numPlaces++;
        }
        Tinv_number++;
    }

    return true;
}
public boolean check_closed_Tinvariants(ArrayList<int[][]> Tinv_incidence_matrices,Map<String,ArrayList<Integer>> Tinvariants_trans,Map<String,ArrayList<Integer>> Tinvariants_places,
Map<String,ArrayList<Integer>> Tinvariants_SM_place,Map<String,ArrayList<Integer>> Tinvariants_SM_trans)
{
    System.out.println("----- Running T-Invariants SM Analysis -----\n");
    int cont = 1;
    ArrayList<Integer> Trans_Auxiliar = new ArrayList<Integer>();
    ArrayList<Integer> Places_Auxiliar = new ArrayList<Integer>();

    for(int[][] matrices : Tinv_incidence_matrices)//recorremos las matrices de incidencia de los Tinvariantes
    {
        int[][] Incidence_Auxiliar = new int [matrices.length][matrices[0].length];
        Trans_Auxiliar.clear();
        Places_Auxiliar.clear();

        for(int row=0; row<matrices.length;row++)
        {
            for (int column = 0; column < matrices[row].length; column++)
            {
                Incidence_Auxiliar[row][column] = matrices[row][column];
            }
        }

        //print_matrix(Incidence_Auxiliar,"Incidence matrix of Tinv " + cont);
        int t,p,pAnterior;
        t=find_first_Tinvariants_enable_transition(Tinvariants_trans.get(String.format("TInv%d (T)",cont)));//aca iria la primer T sencibilizada sino 0;
        if(t==-1)
        {
            System.out.println("T-Invariant " + cont + " doesn't have an idle place or isn't marked");
            S3PRresults="<br>The net isn't S3PR because: T inv " + cont + " has not idle place or isn't marked";
            return false;
        }
        p=0;
        pAnterior=0;
        System.out.println("---------- Analyzing Tinv "+cont+" ----------");
        while (true) //p,t = f,c -> recorro Transiciones
        {
            p=0;
            while (Incidence_Auxiliar[p][t]!=1) //recorro las plazas de 1 transicion
            {
                p++;
                if(p==Incidence_Auxiliar.length)
                {
                    System.out.println("The T-invariant "+ cont + " dosn't have a closed loop\n");
                    S3PRresults="<br>The net isn't S3PR because: The T Invariant "+cont+ " isn't closed";
                    return false;
                }
            }
            if(!Trans_Auxiliar.contains(Tinvariants_trans.get(String.format("TInv%d (T)",cont)).get(t)))
            {
                Trans_Auxiliar.add(Tinvariants_trans.get(String.format("TInv%d (T)",cont)).get(t));// guardo la transicion que ya se recorrio
                //System.out.println("Agrego al TInv "+cont +" La transicion: "+ Tinvariants_trans.get(String.format("TInv%d (T)",cont)).get(t));
                System.out.println("Transition "+Tinvariants_trans.get(String.format("TInv%d (T)",cont)).get(t) +" added to T-invariant "+ cont);
            }
            else
            {
                if((Trans_Auxiliar.get(0) == Tinvariants_trans.get(String.format("TInv%d (T)",cont)).get(t)) && (Trans_Auxiliar.containsAll(Tinvariants_trans.get(String.format("TInv%d (T)",cont)))))//si el bucle contiene todas las transiciones de t invariante
                {
                    Tinvariants_SM_place.put(String.format("TInv%d (P-Loop)",(cont)), new ArrayList<Integer>(Places_Auxiliar));
                    Tinvariants_SM_trans.put(String.format("TInv%d (T-Loop)",(cont)), new ArrayList<Integer>(Trans_Auxiliar));
                    //System.out.println("El TInv "+ cont +" posee un ciclo cerrado\n");
                    System.out.println("The T-invariant "+ cont +" contains a closed loop\n");
                    break;//ya se cumplio las condiciones
                }
                else
                {
                    if(!(Trans_Auxiliar.get(0) == Tinvariants_trans.get(String.format("TInv%d (T)",cont)).get(t)))
                        System.out.println("The transition that was repeated was not the first");
                    if(!(Trans_Auxiliar.containsAll(Tinvariants_trans.get(String.format("TInv%d (T)",cont)))))
                        System.out.println("Encountered a loop that doesn't contain all T-invariant transitions");
                    delete_place_arcs(Incidence_Auxiliar,pAnterior);//elimino los arcos de la plaza q me hizo el ciclo
                    t=find_first_Tinvariants_enable_transition(Tinvariants_trans.get(String.format("TInv%d (T)",cont)));//para q comience desde la t sencibilizada, sino 0
                    p=0;
                    pAnterior=0;
                    Trans_Auxiliar.clear();
                    Places_Auxiliar.clear();
                    //System.out.println("Se encontro un recurso, Todavia no se determina si el TInv "+cont+" es un SM\n");
                    System.out.println("A resource of T-Invariant " + cont + " has been found\n");
                    continue;
                }
            }
                t=0;
            while (Incidence_Auxiliar[p][t]!=-1) //recorro las transiciones de plazas
            {
                t++;
                if(t==Incidence_Auxiliar[0].length)
                {
                    //System.out.println("el T invariante"+ String.format("TInv%d (T)",cont) + "Tiene una P que no tiene salida");
                    System.out.println("The T-Invariant "+ cont + "has a place that doesn't have an exit arc");
                    S3PRresults="<br>The net isn't S3PR because: The T Invariant "+cont+ " isn't closed";
                    return false;
                }
            }
            if(!Places_Auxiliar.contains(Tinvariants_places.get(String.format("TInv%d (P)",cont)).get(p)))
            {
                Places_Auxiliar.add(Tinvariants_places.get(String.format("TInv%d (P)",cont)).get(p));//guardo la plaza que ya se recorrio
                //System.out.println("Agrego al TInv "+cont +" La plaza: "+Tinvariants_places.get(String.format("TInv%d (P)",cont)).get(p));
                System.out.println("Place "+Tinvariants_places.get(String.format("TInv%d (P)",cont)).get(p) +" Added to T-Invariant "+ cont);
            }
            pAnterior=p;
        }
        cont++;
    }
    return true;
}

// Chequea los recursos del tinvarante (en contra del flujo), si existe una plaza que no cumple esto y no esta incluida en el bucle principal , la misma no es una SM.
public boolean check_Tinvariants_SM(ArrayList<int[][]> Tinv_incidence_matrices,Map<String,ArrayList<Integer>> Tinvariants_places,
Map<String,ArrayList<Integer>>  Tinvariants_SM_places,Map<String,ArrayList<Integer>>  Tinvariants_trans,Map<String,ArrayList<Integer>>  Tinvariants_SM_trans,Map<String,ArrayList<Integer>> Tinvariants_Shared_resources)
{
    // Recorro los tinvariantes
    int Ntinv =1;
    int Initial_marking[] = get_initial_marking();

    for (Map.Entry<String, ArrayList<Integer>> TinvSM_trans : Tinvariants_SM_trans.entrySet())
    {
        ArrayList<Integer> Places_leftOvers = new ArrayList<Integer>();
        //Restar las plazas totales del tinvariante con las plazas del bucle principal
        get_leftOvers(Tinvariants_places.get(String.format("TInv%d (P)",Ntinv)), Tinvariants_SM_places.get(String.format("TInv%d (P-Loop)",(Ntinv))),Places_leftOvers);
        //print_arraylist(Places_leftOvers, String.format("Tinv %d places left overs",Ntinv));
        print_arraylist(Places_leftOvers, String.format("T-Invariant %d not operational places",Ntinv));

        //Checkear que esas plazas esten en contra del flujo del bucle principal, de lo contrario no es una SM
        if(!check_Tinvariant_resources(Tinv_incidence_matrices.get(Ntinv-1),Tinvariants_places.get(String.format("TInv%d (P)",Ntinv)),Tinvariants_SM_places.get(String.format("TInv%d (P-Loop)",(Ntinv)))
        ,Tinvariants_trans.get(String.format("TInv%d (T)",Ntinv)),Tinvariants_SM_trans.get(String.format("TInv%d (T-Loop)",Ntinv)),Places_leftOvers,Ntinv,Initial_marking))return false;
        Tinvariants_Shared_resources.put(String.format("TInv%d (R)",Ntinv),Places_leftOvers);
        Ntinv ++ ;

    }
    return true;
}

//Checkear que esas plazas esten en contra del flujo del bucle principal, de lo contrario no es una SM
public boolean check_Tinvariant_resources(int[][] Tinv_incidence_matrices,ArrayList<Integer> Tinvariants_places,
ArrayList<Integer>  Tinvariants_SM_place,ArrayList<Integer> Tinvariants_trans,ArrayList<Integer>  Tinvariants_SM_trans,ArrayList<Integer> Places_leftOvers,int Ntinv,int[] Initial_marking)
{
    int place_row_index;
    for (Integer leftOver_place : Places_leftOvers)
    {
        //Ir a la fila de la plaza 'leftOver_place'
        place_row_index = Tinvariants_places.indexOf(leftOver_place);
        //System.out.println(String.format("Plaza leftover %d (indice (%d) ) del T-invariant %d",leftOver_place,place_row_index,Ntinv));
        System.out.println(String.format("The not operational place %d is a resource in T-invariant %d",leftOver_place,Ntinv));
        //get_index(Tinvariants_places,leftOver_place);
        int trans_entrada=0;
        int trans_salida=0;

        for(int column=0;column<Tinv_incidence_matrices[0].length;column++)
        {
           if(Tinv_incidence_matrices[place_row_index][column]==1)
           {
             trans_entrada = Tinvariants_trans.get(column);
           }

           if(Tinv_incidence_matrices[place_row_index][column]==-1)
           {
             trans_salida = Tinvariants_trans.get(column);
           }

        }


        if(Tinvariants_SM_trans.indexOf(trans_entrada) <= Tinvariants_SM_trans.indexOf(trans_salida))
        {
            /*
            System.out.println(String.format("Plaza leftover %d  del T-invariant %d tiene de transicion de salida %d (indice %d) y entrada %d (indice %d)"
            ,leftOver_place,Ntinv,trans_salida,Tinvariants_SM_trans.indexOf(trans_salida),trans_entrada,Tinvariants_SM_trans.indexOf(trans_entrada)));
            System.out.println(String.format("The place %d (index %d) isn't a resource of T-invariant %d",leftOver_place,place_row_index,Ntinv));
            */
            System.out.println(String.format("Not operational place %d of T-Invariant %d has an output transition %d and input %d"
                    ,leftOver_place,Ntinv,trans_salida,trans_entrada));
            System.out.println(String.format("The place %d isn't a resource of T-invariant %d",leftOver_place,Ntinv));
            S3PRresults=String.format("<br>The net isn't S3PR because: The place %d isn't a resource of T-invariant %d",leftOver_place,Ntinv);
            //print_matrix(Tinv_incidence_matrices, "Matriz incidencia del tinvariante");
            return false;
        }

        // Checkear que el marcado de la plaza sea uno

        if(Initial_marking[leftOver_place-1] == 0)
        {
            System.out.println(String.format("The place %d of T-invariant %d is a resource but the marking is zero",leftOver_place,Ntinv));
            return false;
        }

    }

 return true;
}

public int get_index(ArrayList<Integer>  Tinvariants_original_nodes,int node_label)
{
    int index = 0;
    for(Integer node : Tinvariants_original_nodes)
    {
        if(node == node_label)
            return index;
        index ++;
    }
    return index;
}

//find the index of the first enabled transition of a Tinvariant
public int find_first_Tinvariants_enable_transition(ArrayList<Integer> Tinvariant_trans)
{
    int index = 0;
    for(Integer trans : Tinvariant_trans)
    {
        if(getEnabledTransitions().contains(trans))
        {
            return index;
        }
        else
            index++;
    }
    if(index==Tinvariant_trans.size())
        return -1;
    else
        return index;
}
// Verifies that there is more than one Closed Tinvariant, else return false (falta chequear q sean cerrados)
public boolean check_num_Tinvariants(Matrix TInvariants)
{
    if(TInvariants.getColumnDimension()<=1)
    {
        System.out.println("The net can't be S3PR because there is only one T-Invariant \n");
        S3PRresults="<br>The net isn't S3PR because there is only one T Invariant<br>";
        return false;//existe un solo t invariante
    }
    else return true;
}

// Returns a hashmap of shared places between Tinvariants. (String Tinv -> arraylist of shared places )
public boolean get_shared_places(Map<String,ArrayList<Integer>> Tinvariants_resources,Map<String,ArrayList<Integer>> Tinvariants_shared_resources)
{
    int Tinv_number = 1;
    boolean paso =false;
    for (ArrayList<Integer> places : Tinvariants_resources.values())
    {
        if(places.isEmpty())
        {
            //System.out.println(String.format("Tinv %d doesn't have any resources\n",Tinv_number));
            System.out.println(String.format("The T-Invariant %d can't be S3PR beacause it doesn't have any resources\n",Tinv_number));
            S3PRresults="<br>The net isn't S3PR because: The T Invariant "+Tinv_number+ "doesn't have any resources";
            return false;
        }

        Tinvariants_shared_resources.put(String.format("TInv%d (SR)",(Tinv_number)), new ArrayList<Integer>());
        paso = false;
        for (ArrayList<Integer> places_others : Tinvariants_resources.values())
        {
            if(places.equals(places_others)&&!paso)
            {
                paso=true;
                continue;
            }

            add_intersection(places,places_others,Tinvariants_shared_resources.get(String.format("TInv%d (SR)",(Tinv_number))));
            if(places.isEmpty())
            {
                //System.out.println(String.format("Tinv %d doesn't share any resources\n",Tinv_number));
                System.out.println(String.format("The T-Invariant %d can't be S3PR because it doesn't share any resources\n",Tinv_number));
                S3PRresults="<br>The net isn't S3PR because: The T Invariant "+Tinv_number+ "doesn't have any resources";
                return false;
            }
        }
        Tinv_number ++ ;
    }
    return true;
}

// Adds the intersection elements between list1 and list2 to list_dest
public void add_intersection(ArrayList<Integer> list1,ArrayList<Integer> list2,ArrayList<Integer> list_dest)
{
    for (Integer element : list1)
        if ( (list2.contains(element)) && (!list_dest.contains(element)))
            list_dest.add(element);
}

// Get the not shared elements between list1 and list2 to list_dest
public void get_leftOvers(ArrayList<Integer> list1_original,ArrayList<Integer> list2,ArrayList<Integer> list_dest)
{
    list_dest.clear();
    for (Integer element : list1_original)
        if ( !(list2.contains(element)) )
            list_dest.add(element);
}

// Other S3PR3 associated functions


// Obtains Tinvariants transition numbers (TinvN (T) -> [2,3,4,5]) and Tinvariants places numbers (TinvN (P) -> [1,2,5,7]) including shared and own places .
public void get_tinv_trans_and_places(int [][]IncidenceMatrix,Matrix TInvariants,Map<String,ArrayList<Integer>> Tinvariants_trans,Map<String,ArrayList<Integer>> Tinvariants_places)
{
    //1° agrego a los hashmap la cant de array list segun la cant de t invariantes
    for(int i=0;i<TInvariants.getColumnDimension();i++)
    {
        Tinvariants_places.put(String.format("TInv%d (P)",(i+1)), new ArrayList<Integer>());
        Tinvariants_trans.put(String.format("TInv%d (T)",(i+1)), new ArrayList<Integer>());
    }

     // ----- Obtención de las transiciones que componen los Tinvariantes
    for (int c=0; c < TInvariants.getColumnDimension(); c++)
    {
        for (int f=0; f < TInvariants.getRowDimension(); f++)
        {
            if(TInvariants.get(f,c)==1)
            {
                Tinvariants_trans.get(String.format("TInv%d (T)",(c+1))).add((Integer)(f+1));
            }
        }
    }

    // ----- Obtención de las plazas de los Tinvariantes

    int suma,numarcos,Tinv_number=1;
    //1 recorro los arraylist de las transiciones por tivariante
    for (Map.Entry<String, ArrayList<Integer>> Tinv_trans : Tinvariants_trans.entrySet())
    {
        //2 recorro las plazas de la matriz de incidencia
        for (int f=0; f < IncidenceMatrix.length; f++)//plazas
        {
            suma=0;
            numarcos=0;
            for(int trans : Tinv_trans.getValue()) //itera por columna
            {
                //verifico que sea un -1 o 1 para tener las plazas del t invariante
                if(IncidenceMatrix[f][trans-1] == 1 || IncidenceMatrix[f][trans-1] == -1)
                    numarcos++;
                suma += IncidenceMatrix[f][trans-1];
            }
            //aca verificas que tenga 2 o mas arcos opuestos para saber q sea una plaza del t invariante
            if((numarcos>=2) && (suma ==0))
                Tinvariants_places.get(String.format("TInv%d (P)",(Tinv_number))).add((Integer)(f+1));
        }
        Tinv_number++;
    }
}

public ArrayList<int[][]> get_tinvariants_incidences_matrices(int [][]IncidenceMatrix,Map<String,ArrayList<Integer>> Tinvariants_trans,Map<String,ArrayList<Integer>> Tinvariants_places)
{
        ArrayList<int[][]> Tinv_incidences = new ArrayList<int[][]>();
        for (int Tinv =0;Tinv<Tinvariants_trans.size();Tinv++)
        {
            // Get places and transitions of Tinvariant i .
            ArrayList<Integer> Tinv_trans = Tinvariants_trans.get(String.format("TInv%d (T)",(Tinv+1)));
            ArrayList<Integer> Tinv_places = Tinvariants_places.get(String.format("TInv%d (P)",(Tinv+1)));

            int [][] Tinv_incidence = new int[Tinv_places.size()][Tinv_trans.size()];

            for(int place_index=0;place_index<Tinv_places.size();place_index++)
            {
                int place = Tinv_places.get(place_index);

                for(int trans_index=0;trans_index<Tinv_trans.size();trans_index++)
                {
                    int transition = Tinv_trans.get(trans_index);
                    Tinv_incidence[place_index][trans_index] = IncidenceMatrix[place-1][transition-1];
                }
            }
            Tinv_incidences.add(Tinv_incidence);
        }
        return Tinv_incidences;

}

public int[] get_initial_marking()
{
    Marking mark = root.getDocument().getPetriNet().getInitialMarking();
    return mark.getMarkingAsArray()[1];
}

public ArrayList<Integer> getEnabledTransitions()
{
    //root.getDocument().getPetriNet().getInitialMarking().resetMarking();// da error si no se guarda el marcado <--
    //root.getDocument().getPetriNet().setInitialMarking(get_initial_marking());
    ArrayList<Transition> enabledArray = new ArrayList<Transition>(root.getDocument().getPetriNet().getInitialMarking().getAllEnabledTransitions());
    ArrayList<Integer> enabledNames= new ArrayList<Integer>();

    for (Transition transition : enabledArray)
    {
        enabledNames.add(Integer.valueOf(transition.getLabel().substring(1)));
    }
    return enabledNames;
}
public void delete_place_arcs(int[][] matrix,Integer row)
{
     for (int c=0; c < matrix[0].length; c++)
    {
        matrix[row][c]=0;
    }

}
// ----- Print funcitons -----

public void print_matrix(int[][] matrix,String Title)
{
    System.out.println(Title);
    for (int f=0; f < matrix.length; f++)
    {
        for (int c=0; c < matrix[f].length; c++)
        {
            System.out.print(Integer.toString(matrix[f][c])+" ");
        }
        System.out.println("\n");
    }
}


public void print_arraylist(ArrayList<Integer> list,String Title)
{
    System.out.println(Title);
    for(int list_element : list)
    {
        System.out.print(list_element+" ");
    }
    System.out.println("\n");
}

public void print_hashmap(Map<String,ArrayList<Integer>> hashmap,String Title)
{
    System.out.println(Title);
    //iterate over the linked hashmap
    for (Map.Entry<String, ArrayList<Integer>> entry : hashmap.entrySet())
    {
        //Print key
        System.out.print(entry.getKey()+" : ");
        //Print value (arraylist)
        for(int list_element : entry.getValue()){
            System.out.print(list_element+" ");
        }
        System.out.println();
    }
    System.out.println();
}


}
