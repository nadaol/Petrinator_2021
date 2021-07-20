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

import com.google.gson.Gson;
import org.petrinator.editor.Root;
import org.petrinator.editor.actions.ReloadFileAction;
import org.petrinator.editor.actions.SaveAction;
import org.petrinator.editor.actions.algorithms.reachability.CRTree;
import org.petrinator.editor.filechooser.*;
import org.petrinator.petrinet.*;
import org.petrinator.util.GraphicsTools;
import pipe.gui.widgets.ButtonBar;
import pipe.gui.widgets.EscapableDialog;
import pipe.gui.widgets.FileBrowser;
import pipe.gui.widgets.ResultsHTMLPane;
import pipe.modules.minimalSiphons.MinimalSiphons;
import pipe.utilities.math.Matrix;
import pipe.views.PetriNetView;
import scala.Array;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
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



public class AutomaticPolitics extends AbstractAction
{
    private static final String MODULE_NAME = "Automatics Politics";
    private ResultsHTMLPane results;
    private String sPanel;
    private String S3PRresults;
    private Root root;
    private JDialog guiDialog;
    private ButtonBar FirstAnalizeButton;
    private ButtonBar showPlotButton;
    private ButtonBar HelpButton;
    String[] cost = {"simp", "inv"};
    JComboBox tipoCosto = new JComboBox(cost);
    JTextField firenumbers = new JTextField(0);
    JTextField repeats = new JTextField(0);
    JCheckBox netWithMod = new JCheckBox("NET with Modifications" );
    JCheckBox netWithControlPlaces = new JCheckBox("NET with Control Places");
    //
    ArrayList<String> ControlPlacesString = new ArrayList<String>();
    //
    JCheckBox modifyNetButton = new JCheckBox("Modify NET" );
    InvariantAction accion;
    MatricesAction matrices;
    //test
    ServerSocket server = null;
    Process proceso = null;
    DataOutputStream outw = null;
    DataInputStream inw = null;

    public AutomaticPolitics(Root root)
    {
        this.root = root;
        putValue(NAME, MODULE_NAME);
        putValue(SHORT_DESCRIPTION, MODULE_NAME);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/politicsIconx16.png"));

        guiDialog =  new JDialog(root.getParentFrame(), MODULE_NAME, true);
        guiDialog.setModalityType(Dialog.ModalityType.MODELESS);
        Container contentPane = guiDialog.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        results = new ResultsHTMLPane("");
        sPanel = new String();//new
        S3PRresults = new String();
        contentPane.add(results);
        FirstAnalizeButton = new ButtonBar("Run Politics Analysis", new RunListener(), guiDialog.getRootPane());
        showPlotButton = new ButtonBar("Show Plot", new showPlotListener(), guiDialog.getRootPane());
        HelpButton = new ButtonBar("Help", new HelpListener(), guiDialog.getRootPane());
        //action listener del plazas de control
        //netWithControlPlaces.addActionListener(new PlaceControlListener());
        //
        JPanel checkPanel = new JPanel(new GridLayout(3,3));
        //agrego al nuevo panel
        //String[] cost = {"simp", "inv"};
        //JComboBox tipoCosto = new JComboBox(cost);
        checkPanel.add(new JLabel("Firenumber:"),BorderLayout.PAGE_START);
        checkPanel.add(firenumbers,BorderLayout.PAGE_START);
        checkPanel.add(netWithControlPlaces);
        checkPanel.add(new JLabel("Repeat:"));
        checkPanel.add(repeats);
        checkPanel.add(modifyNetButton);
        checkPanel.add(new JLabel("Cost type:"));
        checkPanel.add(tipoCosto);
        checkPanel.add(netWithMod);
        //termino de agregar al nuevo panel
        contentPane.add(checkPanel, BorderLayout.CENTER);
        JPanel checkPanel2 = new JPanel(new GridLayout(1,2));
        checkPanel2.add(FirstAnalizeButton);
        checkPanel2.add(showPlotButton);
        contentPane.add(checkPanel2, BorderLayout.CENTER);
        contentPane.add(HelpButton);
        //creo un objeto de invariantes
        accion = new InvariantAction(this.root);
        matrices = new MatricesAction(this.root);
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
        if(!root.getDocument().getPetriNet().getRootSubnet().isValid())
        {
            results.setText("Invalid Net");
            JOptionPane.showMessageDialog(null, "Invalid Net!", "Error analysing net", JOptionPane.ERROR_MESSAGE, null);
            guiDialog.setVisible(false);
            //close_socket();
            return;
        }
        results.setText("");

        // Disables the copy and save buttons
        results.setEnabled(false);

        // Enables classify button
        FirstAnalizeButton.setButtonsEnabled(true);
        FirstAnalizeButton.setEnabled(true);//para luego chequear
        showPlotButton.setButtonsEnabled(false);
        ArrayList<String> controlPlaces = root.getDocument().getPetriNet().getControlPlaces();
        if(controlPlaces.isEmpty())
        {
            netWithControlPlaces.setSelected(false);
        }
        else
        {
            netWithControlPlaces.setToolTipText(controlPlaces.toString());
            netWithControlPlaces.setSelected(true);
        }
        //print_arraylistStringonly(controlPlaces,"plazas de control");
        netWithControlPlaces.setEnabled(false);
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
        return 0;
    }
    private String getInputAsString(InputStream is)
    {
        try(java.util.Scanner s = new java.util.Scanner(is))
        {
            return s.useDelimiter("\\A").hasNext() ? s.next() : "";
        }
    }
    public int execPolitics()
    {
        sPanel = "<h2>Run Automatic Politics Analisys</h2>";
        List<String> comandos = new ArrayList<String>();

        try {
            comandos.add("python3");//1°comand
            //Get tesis python path and execute
            String  jar_path = get_Current_JarPath();
            String pathToPythonMain;
            if(jar_path!= null)pathToPythonMain = jar_path +"/Modulos/Automatic-politics/main.py";
            else return 1;
            comandos.add(pathToPythonMain);//2°comand
            comandos.add("./Modulos/Automatic-politics/tmp/jsonmat.json");//3°comand
            try
            {
                if(0 >= Integer.valueOf(firenumbers.getText()) || Integer.valueOf(firenumbers.getText())>100000)
                {
                    JOptionPane.showMessageDialog(null, "Invalid range of firenumbers");
                    return 1;
                }
                comandos.add("-n");//4°comand
                comandos.add(firenumbers.getText());//5°comand
                if(0 >= Integer.valueOf(repeats.getText()) || Integer.valueOf(repeats.getText())>10)
                {
                    JOptionPane.showMessageDialog(null, "Invalid range of repeats");
                    return 1;
                }
                comandos.add("-r");//6°comand
                comandos.add(repeats.getText());//7°comand
                comandos.add("-t");//8°comand
                comandos.add(tipoCosto.getSelectedItem().toString());//9°comand
                if(netWithMod.isSelected())
                {
                    comandos.add("-l");

                }
                if(modifyNetButton.isSelected())
                {
                    comandos.add("-m");
                    comandos.add(root.getCurrentFile().getPath());
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid firenumber or repeats");
                return 1;
            }
            print_arraylistStringonly((ArrayList<String>) comandos,"comando");
            ProcessBuilder pb = new ProcessBuilder(comandos);
            pb.redirectErrorStream(true);
            sPanel+= getInputAsString(pb.start().getInputStream());
            results.setText(sPanel);
            reSaveNet();
            //showPlot();

        } catch (IOException e) {
            results.setText("");
            e.printStackTrace();
            JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error executing python module", JOptionPane.ERROR_MESSAGE);
            return 1;
        }
        return 0;
    }

    //Function to save the current net in a temp.pflow file for later supervision analisys
    public int saveNet()
    {

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

    public void print_arraylistString(ArrayList<String []> list,String Title)
    {
        System.out.println(Title);
        for(String [] string_array : list)
        {
            for(int i=0;i<string_array.length;i++)
            {
                System.out.print(string_array[i]+" ");
            }
            System.out.println("\n");
        }
        System.out.println("\n");
    }
    public void print_arraylistStringonly(ArrayList<String> list,String Title)
    {
        System.out.println(Title);
        for(String string_array : list)
        {
                System.out.print(string_array+" ");
        }
        System.out.println("\n");
    }
    /**
     * hace el analisis de t invariantes
     * @return Array list con T invariantes de a forma [T1,T2,T3]
     */
    public ArrayList<String[]> invariantAnalysis()
    {
        ArrayList<String> TinvariantsLabels_aux=new ArrayList<String>();
        ArrayList<String[]> TinvariantsLabels=new ArrayList<String[]>();
        Matrix TInvariants = accion.findVectors(new Matrix(root.getDocument().getPetriNet().getIncidenceMatrix()));
        ArrayList<String> transNames = root.getDocument().getPetriNet().getSortedTransitionsNames();
        for (int c=0; c < TInvariants.getColumnDimension(); c++)
        {
            //System.out.println("Fila: "+(f+1));
            TinvariantsLabels_aux.add(new String());
            for (int f=0; f < TInvariants.getRowDimension(); f++)
            {
                if(TInvariants.get(f,c)==1)
                {
                    TinvariantsLabels_aux.set(c,TinvariantsLabels_aux.get(c)+transNames.get(f)+" ");
                }
            }
            TinvariantsLabels.add(TinvariantsLabels_aux.get(c).split(" "));
        }
        return TinvariantsLabels;
    }

    /**
     * exporta los json files que utilitizara el agortimo .json (todas las matrices) y cfg.json
     * (costos estaticos y T-Invariantes)
     * @return 1 en caso de error , 0 caso exitoso
     */
    public int exportJsonsFiles()
    {
        Map<String, Object> extraAttributes = new HashMap<>();
        ArrayList<String> controlPlaces = root.getDocument().getPetriNet().getControlPlaces();
        Gson gson = new Gson();
        extraAttributes.put("Costos", root.getDocument().getPetriNet().getCostArray());
        extraAttributes.put("Invariantes",invariantAnalysis());
        if(!controlPlaces.isEmpty())
        {
            extraAttributes.put("ControlPlaces",controlPlaces);
        }
        String json = gson.toJson(extraAttributes);

        String destFNcfg,destFN;
        String jar_path = get_Current_JarPath();
        if(jar_path != null)destFNcfg = jar_path + "/Modulos/Automatic-politics/tmp/jsonmat.cfg.json";
        else return 1;

        try {
            FileWriter writer = new FileWriter(new File(destFNcfg));
            writer.write(json);
            writer.close();
        } catch (Exception var6) {
            System.out.println("Error saving CGF.JSON to file");
        }
        //only matrices
        if(jar_path != null)destFN = jar_path + "/Modulos/Automatic-politics/tmp/jsonmat.json";
        else return 1;
        Map<String, Object> matricesonly = new HashMap<>();

        Gson gsononly = new Gson();

        matricesonly.put("I-", root.getDocument().getPetriNet().getBackwardsIMatrix());
        matricesonly.put("I+", root.getDocument().getPetriNet().getForwardIMatrix());
        matricesonly.put("Incidencia", root.getDocument().getPetriNet().getIncidenceMatrix());
        matricesonly.put("Inhibicion", root.getDocument().getPetriNet().getInhibitionMatrix());
        matricesonly.put("Marcado", root.getDocument().getPetriNet().getInitialMarking().getMarkingAsArray()[Marking.CURRENT]);

        String jsononly = gsononly.toJson(matricesonly);
        try {
            FileWriter writer = new FileWriter(new File(destFN));
            writer.write(jsononly);
            writer.close();
        } catch (Exception var6) {
            System.out.println("Error saving JSON to file");
        }
        return 0;
    }
    private class showPlotListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            /*
             * Create the dialog
             */
            EscapableDialog guiDialog = new EscapableDialog(root.getParentFrame(), "Plot results", false);
            Container contentPane = guiDialog.getContentPane();
            //org.petrinator.auxiliar.ResultsHTMLPane results = new org.petrinator.auxiliar.ResultsHTMLPane("");
            //contentPane.add(results);
            //
            File fichero=new File(get_Current_JarPath() + "/plot.png");
            ImageIcon books = new ImageIcon(fichero.getPath());
            JLabel imgLabel = new JLabel();
            imgLabel.setIcon(books);
            imgLabel.setMaximumSize(new Dimension(books.getIconWidth(), books.getIconHeight()));
            imgLabel.setAutoscrolls(true);
            imgLabel.grabFocus();
            contentPane.add(imgLabel);
            //
            guiDialog.pack();
            guiDialog.setAlwaysOnTop(true);
            guiDialog.setLocationRelativeTo(root.getParentFrame());
            guiDialog.setVisible(true);

            //String s = "<img src='"+get_Current_JarPath()+"/plotonly.png"+"' class='img-responsive' height='1000' width='1200' alt=''>";

            System.out.println(fichero.getPath());
            books.getImage().flush();
        }
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
    private class RunListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent)
        {
            System.out.println("----- Running Politics Analysis -----");
            // Checks if the net is valid
            if(!root.getDocument().getPetriNet().getRootSubnet().isValid())
            {
                results.setText("Invalid Net");
                JOptionPane.showMessageDialog(null, "Invalid Net!", "Error analysing net", JOptionPane.ERROR_MESSAGE, null);
                guiDialog.setVisible(false);
                //close_socket();
                return;
            }
            exportJsonsFiles();
            execPolitics();
            results.setVisible(true);
            showPlotButton.setButtonsEnabled(true);

        }
    };
    private class HelpListener implements ActionListener {
        public void actionPerformed(ActionEvent e)
        {
            /*
             * Create the dialog
             */
            EscapableDialog guiDialog = new EscapableDialog(root.getParentFrame(), "Help: Automatics Politics", false);
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
            InputStream aboutFile = getClass().getResourceAsStream("/AutomaticPoliticsHelp.html");
            Scanner scanner = null;
            scanner = new Scanner(aboutFile, "UTF-8");
            String s = scanner.useDelimiter("\\Z").next();
            scanner.close();

            /*
             * Show the text on dialog
             */
            results.setText(s);
        }
    };
  
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
