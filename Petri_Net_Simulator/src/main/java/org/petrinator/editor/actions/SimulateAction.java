package org.petrinator.editor.actions;

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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.petrinator.editor.Root;
import org.petrinator.editor.filechooser.FileChooserDialog;
import org.petrinator.editor.filechooser.FileType;
import org.petrinator.editor.filechooser.FileTypeException;
import org.petrinator.petrinet.*;
import org.petrinator.util.GraphicsTools;
import org.petrinator.editor.commands.FireTransitionCommand;
import org.petrinator.auxiliar.*;

import java.awt.event.*;
import java.util.*;

import org.unc.lac.javapetriconcurrencymonitor.errors.DuplicatedNameError;
import org.unc.lac.javapetriconcurrencymonitor.errors.IllegalTransitionFiringError;
import org.unc.lac.javapetriconcurrencymonitor.exceptions.PetriNetException;
import org.unc.lac.javapetriconcurrencymonitor.monitor.PetriMonitor;
import org.unc.lac.javapetriconcurrencymonitor.monitor.policies.FirstInLinePolicy;
import org.unc.lac.javapetriconcurrencymonitor.monitor.policies.TransitionsPolicy;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.CudaPetriNet;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.RootPetriNet;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.factory.PetriNetFactory;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.factory.PetriNetFactory.petriNetType;

import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * @author Joaquin Felici <joaquinfelici at gmail.com>
 * @brief Does N firings, one every Y seconds.
 * @detail Creates a monitor, subscribes to all transitions and creates
 * a thread for each one of them. Every thread will try to persistently fire
 * it's associated transition, until N firings have been executed.
 * Once's it's finished, a new thread is created, in charge of graphically
 * executing all these firings, one every Y seconds.
 */
public class SimulateAction extends AbstractAction
{
    private Root root;
    private List<FileType> fileTypes;
    protected static boolean stop = false;
    ActionEvent e;
    public static List<Double> instants = new ArrayList<Double>();
    private boolean running = false;
    private String serverIP = "";
    private String defaultIP = "localhost";
    private String defaultPort = "8080";

    public SimulateAction(Root root, List<FileType> fileTypes) {
        this.root = root;
        this.fileTypes = fileTypes;
        String name = "Simulate";
        putValue(NAME, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/play16.png"));
        putValue(SHORT_DESCRIPTION, name);
    }

    public void actionPerformed(ActionEvent e)
    {
        stop = false;

        if(!root.getDocument().getPetriNet().getRootSubnet().isValid()){
            JOptionPane.showMessageDialog(null, "Invalid Net!", "Error", JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        /*
         * Create tmp.pnml file
         */
        FileChooserDialog chooser = new FileChooserDialog();

        if (root.getCurrentFile() != null)
        {
            chooser.setSelectedFile(root.getCurrentFile());
        }

        for (FileType fileType : fileTypes)
        {
            chooser.addChoosableFileFilter(fileType);
        }
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setCurrentDirectory(root.getCurrentDirectory());
        chooser.setDialogTitle("Save as...");

        File file = new File("tmp/" + "tmp" + "." + "pnml");
        FileType chosenFileType = (FileType) chooser.getFileFilter();
        try
        {
            chosenFileType.save(root.getDocument(), file);
        }
        catch (FileTypeException e1)
        {
            e1.printStackTrace();
        }

        /*
         * Ask user to insert times
         */
        int numberOfTransitions = 1;
        int timeBetweenTransitions = 10;
        boolean skipGraphicalFire = false;
        boolean cudaServer = false;

        JTextField numberTF = new JTextField(8);
        JTextField timeTF = new JTextField(8);
        JCheckBox skipCheck = new JCheckBox();
        JCheckBox serverCheck = new JCheckBox();
        JTextField ipTF = new JTextField(16);
        JTextField portTF = new JTextField(6);

        JPanel myPanel = new JPanel();
        myPanel.setLayout(new MigLayout());

        myPanel.add(new JLabel("Number of transitions:  "));
        myPanel.add(new JLabel ("    "));
        myPanel.add(numberTF,"wrap");

        if(!root.getDocument().petriNet.getRootSubnet().anyStochastic())
        {
            myPanel.add(new JLabel("Time between transition [ms]:  "));
            myPanel.add(new JLabel ("    "));
            myPanel.add(timeTF,"wrap");
        }

        myPanel.add(new JLabel("\n"), "wrap");

        myPanel.add(new JLabel("Skip graphic simulation: "));
        myPanel.add(new JLabel ("    "));
        myPanel.add(skipCheck, "wrap");

        myPanel.add(new JLabel("\n"), "wrap");

        myPanel.add(new JLabel("\nExcecute simulation in a remote server: "));
        myPanel.add(new JLabel ("    "));
        myPanel.add(serverCheck, "wrap");

        JLabel ipLabel = new JLabel("Server IP: ");
        JLabel portLabel = new JLabel("Port: ");

        myPanel.add(ipLabel);
        myPanel.add(new JLabel ("    "));
        myPanel.add(ipTF,"wrap");
        myPanel.add(portLabel);
        myPanel.add(new JLabel ("    "));
        myPanel.add(portTF, "wrap");

        ipTF.setVisible(false);
        ipLabel.setVisible(false);
        portTF.setVisible(false);
        portLabel.setVisible(false);

        timeTF.setText("1000");
        numberTF.setText("10");
        ipTF.setText(defaultIP);
        portTF.setText(defaultPort);

        serverCheck.addActionListener(actionEvent -> {
            if(serverCheck.isSelected()){
                ipTF.setVisible(true);
                ipLabel.setVisible(true);
                portTF.setVisible(true);
                portLabel.setVisible(true);
            }
            else {
                ipTF.setVisible(false);
                ipLabel.setVisible(false);
                portTF.setVisible(false);
                portLabel.setVisible(false);
            }
        });

        int result = JOptionPane.CANCEL_OPTION;

        result = JOptionPane.showConfirmDialog(root.getParentFrame(), myPanel, "Simulation time", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, GraphicsTools.getIcon("pneditor/play32.png"));
        if (result == JOptionPane.OK_OPTION)
        {

            try
            {
                int _transitions = Integer.parseInt(numberTF.getText());
                int _time = Integer.parseInt(timeTF.getText());

                skipGraphicalFire = skipCheck.isSelected();
                cudaServer = serverCheck.isSelected();

                if(cudaServer){
                    serverIP = String.format("http://%s:%s", ipTF.getText(), portTF.getText());
                    defaultIP = ipTF.getText();
                    defaultPort=portTF.getText();
                }

                if(_transitions < numberOfTransitions || _time < timeBetweenTransitions){
                    throw new NumberFormatException();
                }
                else {
                    numberOfTransitions = _transitions;
                    timeBetweenTransitions = _time;

                }
            }
            catch(NumberFormatException e1)
            {
                String title = "Invalid Input!";
                String message = String.format("Number of transitions must be at least: %d\n Time between transitions must be at least: %d ms  \n", numberOfTransitions, timeBetweenTransitions);
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE, null);
                return; // Don't execute further code
            }
        }
        else {
            return;
        }


        root.disableWhileSimulating();
        root.getDocument().getPetriNet().getInitialMarking().updateInitialMarking();

        /*
         * Run a single thread to fire the transitions graphically
         */
        final boolean skip = skipGraphicalFire;
        final boolean cuda = cudaServer;
        final int number = numberOfTransitions;
        final int time = timeBetweenTransitions;

        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                runInMonitor(number, time, skip, cuda);
                root.enableAfterStop();
            }
        });
        t.start();

    }

    /*
     * @brief Creates monitor, threads for transitions, observer, and runs all threads.
     * @detail After getting all the firings the user set, it creates a thread that
     * will "fire" the transitions within our editor every x millis.
     */
    private void runInMonitor(int numberOfTransitions, int timeBetweenTransitions, boolean skipGraphicalFire, boolean cudaServer)
    {
        /*
         * Create monitor, petri net, and all related variables.
         */
        PetriNetFactory factory = new PetriNetFactory("tmp/tmp.pnml");
        RootPetriNet petri;

        try
        {  // The exception tell us if there's two places or transitions with the same name

            if(cudaServer) {
                petri = factory.makePetriNet(petriNetType.CUDA);
                boolean init = ((CudaPetriNet) petri).initializeCuda(serverIP);
                if(!init){
                    return;
                }

            }
            else{
                petri = factory.makePetriNet(petriNetType.PLACE_TRANSITION);
            }

        } catch (DuplicatedNameError e)
        {
            JOptionPane.showMessageDialog(null, "Two places or transitions cannot have the same label");
            stop = false;
            setEnabled(true);
            return; // Don't execute further code
        }

        TransitionsPolicy policy = new FirstInLinePolicy();
        PetriMonitor monitor = new PetriMonitor(petri, policy, numberOfTransitions);
        PetriMonitor.simulationRunning = true;

        petri.initializePetriNet();

		 /*
		  * Create one thread per transition, start them all to try and fire them.
		  */
        List<Thread> threads = new ArrayList<Thread>();
        for(int i = 0; i < petri.getTransitions().length; i++)
        {
            if(!(root.getDocument().petriNet.getRootSubnet().getTransition(petri.getTransitions()[i].getId()).isAutomatic()))
            {
                Thread t = createThread(monitor, petri.getTransitions()[i].getName());
                threads.add(t);
                t.start();
            }
        }

        System.out.println("Simulation");
        System.out.println(" > Started firing");

        ProgressBarDialog dialog = new ProgressBarDialog(root, "Simulating...");
        dialog.show(true);

        boolean blocked = false;
        long simTime = -1;
		 /*
		  * Wait for the number of events to occur
		  */
        while(true)
        {

            if(!PetriMonitor.simulationRunning)
                break;
            else
            {
                try
                {
                    Thread.sleep(10);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

                if(petri.isBlockedPetriNet() && !petri.anyWaiting())   // We need to check if the net is blocked and no more transitions can be fored
                {
                    blocked = true;
                    simTime = monitor.getTimeElapsed();

                    if(monitor.getListOfEvents().size() == 0)
                        JOptionPane.showMessageDialog(root.getParentFrame(), "The net is blocked.\n\nMake sure that at least one fired\n transition comes before the automatic ones.\n\n" + monitor.getListOfEvents().size() + " transitions were fired.");
                    else
                        JOptionPane.showMessageDialog(root.getParentFrame(), "The net is blocked, " + monitor.getListOfEvents().size() + " transitions were fired.");

                    System.out.println(" > Monitor blocked");
                    System.out.printf("Transiciones disparadas antes de bloquearse: %d\n", monitor.getListOfEvents().size());

                    break;
                }
            }
        }

        //monitor.simulationRunning = false;
        System.out.println(" > Simulation started");
        dialog.show(false);

         /*
          * Stop all threads from firing
          */
        for(Thread t: threads)
        {
            t.stop();
        }

        /*
         * We simulate to press the EditTokens/EditTransition button so the enabled transitions
         * will be shown in green.
         */
        new TokenSelectToolAction(root).actionPerformed(e);

        /*
        *   Display simulation time
        * */
        if(!blocked){
            simTime = monitor.getSimulationTime();
        }

        if(simTime != -1)
            JOptionPane.showMessageDialog(root.getParentFrame(), "Tiempo de simulacion: " + simTime + " ms");

        /*
         * We fire the net graphically
         */
        running = true;
        instants.clear();
        for(Place place : root.getDocument().petriNet.getRootSubnet().getPlaces())
        {
            place.clearValues();
        }
        analyzePlaces(timeBetweenTransitions);
        fireGraphically(monitor.getListOfEvents(), timeBetweenTransitions, numberOfTransitions, skipGraphicalFire);
        new SelectionSelectToolAction(root).actionPerformed(e);

        running = false;
        System.out.println(" > Simulation ended");
        //root.enableAfterStop();
    }

    /*
     * @brief Creates a thread that tries to fire one given transition
     * @param m the monitor that holds our petri net
     * @param id the id of the transition this thread will try to fire
     * @return t the created tread
     */
    Thread createThread(PetriMonitor m, String id)
    {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run()
            {
                while(true)
                {
                    try
                    {
                        Thread.sleep(1);
                        m.fireTransition(id);
                    } catch (IllegalTransitionFiringError | IllegalArgumentException | PetriNetException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        return t;
    }

    /*
     * @brief Takes the list of events, and performs one per one, every x millis (set by user)
     * @param list list of events
     * @param timeBetweenTransitions milliseconds to wait between events performed
     * @return
     */
    void fireGraphically(ArrayList<String[]> listOfEvents, int timeBetweenTransitions, int numberOfTransitions, boolean skipGraphicalFire){

        int i = 0;
        for(String[] event : listOfEvents)
        {
            /*
             * Check if stop button has been pressed
             */
            if(stop)
            {
                stop = false;
                setEnabled(true);
                listOfEvents.clear();
                System.out.println(" > Simulation stopped by user");
                return;
            }

            System.out.println(Arrays.toString(event));

            double time = 0;
            try
            {
                time = Double.parseDouble(event[PetriMonitor.TIME]) * 1000;
            }
            catch (ArrayIndexOutOfBoundsException e) {} // The transition is not timed, so no time to retrieve. No biggy.


            Transition transition = root.getDocument().petriNet.getRootSubnet().getTransition(event[PetriMonitor.TID]);
            Marking marking = root.getDocument().petriNet.getInitialMarking();

            if(!skipGraphicalFire)
                root.getEventList().addEvent((transition.getLabel() + " was fired!"));

            if(transition.isTimed())
            {
                transition.setTime((int) time);
                transition.setWaiting(true);

                if(!skipGraphicalFire) {
                    countDown(transition);

                    try {
                        System.out.println("Sleeping " + (int) time);
                        Thread.sleep((int) time);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                transition.setWaiting(false);
            }

            FireTransitionCommand fire = new FireTransitionCommand(transition, marking);
            fire.execute();

            if(!skipGraphicalFire)
                root.refreshAll();

            /*
             * Maybe, if several threads executed multiple transitions concurrently,
             * there are more events than "numberOfTransitions" specified.
             * Let's make sure we won't fire more than "numberOfTransitions"
             */
            if(++i >= numberOfTransitions)
            {
                setEnabled(true);
                return;
            }

            if(!skipGraphicalFire){
                if(!root.getDocument().petriNet.getRootSubnet().anyStochastic())
                {
                    try
                    {
                        Thread.sleep(timeBetweenTransitions);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                else
                {
                    try
                    {
                        Thread.sleep(50);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        if(skipGraphicalFire){
            root.refreshAll();
        }

    }

    public void countDown(Transition t)
    {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                long begin = System.currentTimeMillis();

                while(t.getTime()>1)
                {
                    try
                    {
                        root.repaintCanvas();
                        Thread.sleep(5);
                        t.setTime((int) (t.getTime()-(System.currentTimeMillis()-begin)));
                        begin = System.currentTimeMillis();
                    } catch (IllegalTransitionFiringError | IllegalArgumentException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public void analyzePlaces(int timeBetweenTransitions)
    {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                double time = 0;
                int timeToSleep = 0;

                if(root.getDocument().petriNet.getRootSubnet().anyStochastic())
                {
                    timeToSleep = 500;
                }
                else
                {
                    timeToSleep = timeBetweenTransitions;
                }

                while(running)
                {
                    Marking marking = root.getDocument().petriNet.getInitialMarking();
                    Set<Place> places = root.getDocument().petriNet.getRootSubnet().getPlaces();

                    for(Place place : places)
                    {
                        place.addValue(marking.getTokens(place));
                    }

                    instants.add(time);

                    time += (double) timeToSleep / 1000;
                    time = roundDouble(time);

                    try
                    {
                        Thread.sleep(timeToSleep);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public static double roundDouble(double value)
    {
        long factor = (long) Math.pow(10, 2);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
