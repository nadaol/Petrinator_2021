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
    private class PlaceControlListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            if(netWithControlPlaces.isSelected())
            {
                String returnValue="";
                boolean isNumeric;
                int place=0;
                int result=0;
                ControlPlacesString.clear();
                while(result==0)//while(returnValue!= null)
                {
                    /*
                    returnValue = (String) JOptionPane.showInputDialog(null,
                            "Plazas de control: "+ControlPlacesString.toString(),
                            "(Cancelar para finalizar)", JOptionPane.QUESTION_MESSAGE, null,
                            null, // Array of choices
                            null);*/
                    //nuevo formato
                    Object[] options1 = { "Aceptar","Finalizar"};
                    JPanel panel = new JPanel();
                    panel.add(new JLabel("Plazas de control: "+ ControlPlacesString.toString()));
                    JTextField textField = new JTextField(10);
                    textField.setText("");
                    panel.add(textField);
                    panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    result = JOptionPane.showOptionDialog(null, panel, "Agregar plaza de control",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                            null, options1, null);
                    //System.out.println("Resultado : "+result);
                    //termina nuevo formaato
                    try {
                        place=Integer.valueOf(textField.getText());
                        isNumeric = true;
                    } catch (NumberFormatException excepcion) {
                        isNumeric = false;
                    }

                    if(isNumeric && textField.getText()!= "" && !ControlPlacesString.contains("P"+textField.getText()) && place > 0 && place <=root.getDocument().getPetriNet().getSortedPlacesNames().size() && result==0)
                    {
                        ControlPlacesString.add("P"+textField.getText());
                    }
                    else if(textField.getText()!= "" && result==0)
                        JOptionPane.showMessageDialog(root.getParentFrame(),"Place is not numeric, is not in the range or is repeat", "Place number error", JOptionPane.ERROR_MESSAGE);

                }
                if(ControlPlacesString.isEmpty())
                {
                    JOptionPane.showMessageDialog(root.getParentFrame(),"There is not control places added", "Control Places Error", JOptionPane.ERROR_MESSAGE);
                    netWithControlPlaces.setSelected(false);
                }
            }
        }
    }
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


// parte para s3pr luego la sacamoss
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
