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
package org.petrinator.editor;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.event.*;

import org.petrinator.auxiliar.EventList;
import org.petrinator.editor.actions.*;
import org.petrinator.editor.actions.algorithms.*;
import org.petrinator.editor.canvas.*;
import org.petrinator.editor.filechooser.EpsFileType;
import org.petrinator.editor.filechooser.FileType;
import org.petrinator.editor.filechooser.FileTypeException;
import org.petrinator.editor.filechooser.PflowFileType;
import org.petrinator.editor.filechooser.PngFileType;
import org.petrinator.editor.filechooser.ViptoolPnmlFileType;
import org.petrinator.editor.filechooser.TinaPnmlFileType;
import org.petrinator.editor.filechooser.PipePnmlFileType;
import org.petrinator.petrinet.Arc;
import org.petrinator.petrinet.Document;
import org.petrinator.petrinet.Element;
import org.petrinator.petrinet.Marking;
import org.petrinator.petrinet.PlaceNode;
import org.petrinator.petrinet.ReferencePlace;
import org.petrinator.petrinet.Role;
import org.petrinator.petrinet.Subnet;
import org.petrinator.petrinet.Transition;
import org.petrinator.petrinet.TransitionNode;
import org.petrinator.util.CollectionTools;
import org.petrinator.util.GraphicsTools;
import org.petrinator.util.ListEditor;



/**
 * This class is the main point of the application.
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class Root implements WindowListener, ListSelectionListener, SelectionChangedListener {

    private static final String APP_NAME = "Petrinator";
    private static final String APP_VERSION = "1.0.0";

    /*
    * Used to disable all actions while simulating
    */
    private boolean simulating = false;

    /*
     * Added event list to show which transitions were fired
     */
    private JSplitPane splitPane;
    private EventList events = new EventList();

    public Root(String[] args)
    {
        PNEditor.setRoot(this);

        loadPreferences();
        selection.setSelectionChangedListener(this);

        roleEditor = new ListEditor<Role>("Roles", document.roles, getParentFrame());
        roleEditor.addButton.setIcon(GraphicsTools.getIcon("pneditor/addrole.gif"));
        roleEditor.deleteButton.setIcon(GraphicsTools.getIcon("pneditor/deleterole.gif"));
        roleEditor.addButton.setToolTipText("Add role");
        roleEditor.editButton.setToolTipText("Edit role properties");
        roleEditor.deleteButton.setToolTipText("Delete role");
        roleEditor.addListSelectionListener(this);

        setupMainFrame();
        mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setupFrameIcons();

        /*
         * Create tmp directory if it doesn't exist
         */
        File directory = new File("tmp");
        if (!directory.exists())
        {
            directory.mkdir();
        }

        if (args.length == 1)
        {
            String filename = args[0];
            File file = new File(filename);
            FileType fileType = FileType.getAcceptingFileType(file, FileType.getAllFileTypes());
            try
            {
                Document document = fileType.load(file);
                this.setDocument(document);
                this.setCurrentFile(file);
                this.setModified(false);
                this.setCurrentDirectory(file.getParentFile());
                canvas.repaint();
            } catch (FileTypeException ex) {
                Logger.getLogger(Root.class.getName()).log(Level.INFO, null, ex);
            }
        }
    }

    private static final String CURRENT_DIRECTORY = "current_directory";

    private void loadPreferences() {
        Preferences preferences = Preferences.userNodeForPackage(this.getClass());
        setCurrentDirectory(new File(preferences.get(CURRENT_DIRECTORY, System.getProperty("user.home"))));
    }

    private void savePreferences() {
        Preferences preferences = Preferences.userNodeForPackage(this.getClass());
        preferences.put(CURRENT_DIRECTORY, getCurrentDirectory().toString());
    }

    // Undo manager - per tab
    protected UndoAction undo = new UndoAction(this);
    protected RedoAction redo = new RedoAction(this);
    private UndoManager undoManager = new UndoManager(this, undo, redo);

    public UndoManager getUndoManager() {
        return undoManager;
    }

    // Current directory - per application
    private File currentDirectory;

    public File getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(File currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    // Main frame - per application
    private MainFrame mainFrame = new MainFrame(getNewWindowTitle());

    public Frame getParentFrame() {
        return mainFrame;
    }

    // Document - per tab
    protected Document document = new Document();

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
        getDocument().petriNet.resetView();
        getRoleEditor().setModel(getDocument().roles);
        getUndoManager().eraseAll();
        refreshAll();
    }

    // Clicked element - per tab
    private Element clickedElement = null;

    public Element getClickedElement() {
        return clickedElement;
    }

    public void setClickedElement(Element clickedElement) {
        this.clickedElement = clickedElement;
        enableOnlyPossibleActions();
    }

    // Selection - per tab
    private Selection selection = new Selection();

    public Selection getSelection() {
        return selection;
    }

    @Override
    public void selectionChanged() {
        enableOnlyPossibleActions();
    }

    // Selection + clicked element
    public Set<Element> getSelectedElementsWithClickedElement() {
        Set<Element> selectedElements = new HashSet<Element>();
        selectedElements.addAll(getSelection().getElements());
        selectedElements.add(getClickedElement());
        return selectedElements;
    }

    // List editor - per tab
    private ListEditor<Role> roleEditor; //TODO

    @Override
    public void valueChanged(ListSelectionEvent e) {
        enableOnlyPossibleActions();
        repaintCanvas();
    }

    //per tab
    public void selectTool_Select() {
        select.setSelected(true);
        canvas.activeCursor = Cursor.getDefaultCursor();
        canvas.setCursor(canvas.activeCursor);
        repaintCanvas();
    }

    public boolean isSelectedTool_Select() {
        return select.isSelected();
    }

    public void selectTool_Place() {
        place.setSelected(true);
        canvas.activeCursor = GraphicsTools.getCursor("pneditor/canvas/place.gif", new Point(16, 16));
        canvas.setCursor(canvas.activeCursor);
        repaintCanvas();
    }

    public boolean isSelectedTool_Place() {
        return place.isSelected();
    }

    public void selectTool_Transition() {
        transition.setSelected(true);
        canvas.activeCursor = GraphicsTools.getCursor("pneditor/canvas/transition.gif", new Point(16, 16));
        canvas.setCursor(canvas.activeCursor);
        repaintCanvas();
    }

    public boolean isSelectedTool_Transition() {
        return transition.isSelected();
    }

    public void selectTool_Arc() {
        arc.setSelected(true);
        canvas.activeCursor = GraphicsTools.getCursor("pneditor/canvas/arc.gif", new Point(0, 0));
        canvas.setCursor(canvas.activeCursor);
        repaintCanvas();
    }

    public boolean isSelectedTool_Arc() {
        return arc.isSelected();
    }

    public void selectTool_Token() {
        token.setSelected(true);
        canvas.activeCursor = GraphicsTools.getCursor("pneditor/canvas/token_or_fire.gif", new Point(16, 0));
        canvas.setCursor(canvas.activeCursor);
        repaintCanvas();
    }

    public boolean isSelectedTool_Token() {
        return token.isSelected();
    }

    public ListEditor<Role> getRoleEditor() {
        return roleEditor;
    }

    public JPopupMenu getPlacePopup() {
        return placePopup;
    }

    public JPopupMenu getTransitionPopup() {
        return transitionPopup;
    }

    public JPopupMenu getArcEdgePopup() {
        return arcEdgePopup;
    }

    public JPopupMenu getSubnetPopup() {
        return subnetPopup;
    }

    public JPopupMenu getCanvasPopup() {
        return canvasPopup;
    }

    //per tab
    protected Canvas canvas = new Canvas(this);
    private DrawingBoard drawingBoard = new DrawingBoard(canvas);

    private JPopupMenu placePopup;
    private JPopupMenu transitionPopup;
    private JPopupMenu arcEdgePopup;
    private JPopupMenu subnetPopup;
    private JPopupMenu canvasPopup;

    private ArrayList<Action> actionList = new ArrayList<>();

    //per application
    private JToggleButton select, place, transition, arc, token;
    private Action setLabel, setBehavior, setTokens, setArcMultiplicity, setArcInhibitory, setArcReset, setArcReader, delete;
    private Action setPlaceStatic;
    private Action addSelectedTransitionsToSelectedRoles;
    private Action removeSelectedTransitionsFromSelectedRoles;
    private Action convertTransitionToSubnet;
    private Action replaceSubnet;
    private Action saveSubnetAs;
    private Action cutAction, copyAction, pasteAction, selectAllAction, graphPlaceAction, graphMultiplePlacesAction;
    private Action matrixAction;

    private Action stopSimulation;
    private Action restartNet;
    private Action saveMarking;
    private Action reloadFile;

    //per application
    private Action openSubnet;
    private Action closeSubnet;


    public void openSubnet() {
        openSubnet.actionPerformed(null);
    }

    public void closeSubnet() {
        closeSubnet.actionPerformed(null);
    }

    public void refreshAll() {
        canvas.repaint();
        enableOnlyPossibleActions();
        getRoleEditor().refreshSelected();

        /*
         * We update splitspane
         */
        splitPane.remove(events.getScrollPane());
        splitPane.setLeftComponent(events.getScrollPane());
        splitPane.setDividerLocation(splitPane.getDividerLocation()); // Super extremely important line
    }

    public void repaintCanvas() {
        canvas.repaint();
    }

    private void enableOnlyPossibleActions() {
        boolean isDeletable = clickedElement != null
                && !(clickedElement instanceof ReferencePlace)
                || !selection.isEmpty()
                && !CollectionTools.containsOnlyInstancesOf(selection.getElements(), ReferencePlace.class);
        boolean isCutable = isDeletable;
        boolean isCopyable = isCutable;
        boolean isPastable = !clipboard.isEmpty();
        boolean isPlaceNode = clickedElement instanceof PlaceNode;
        boolean isArc = clickedElement instanceof Arc;
        boolean isTransitionNode = clickedElement instanceof TransitionNode;
        boolean isTransition = clickedElement instanceof Transition;
        boolean isSubnet = clickedElement instanceof Subnet;
        boolean areSubnets = !selection.getSubnets().isEmpty();
        boolean areTransitionNodes = !selection.getTransitionNodes().isEmpty();
        boolean areTransitions = !selection.getTransitions().isEmpty();
        boolean roleSelected = !roleEditor.getSelectedElements().isEmpty();
        boolean isParent = !document.petriNet.isCurrentSubnetRoot();
        boolean isEmpty = !document.petriNet.getRootSubnet().getPlaces().isEmpty() && !document.petriNet.getRootSubnet().getTransitions().isEmpty();
        boolean isPtoT = false;

        if (isArc) {
            Arc test;
            test = (Arc) clickedElement;
            isPtoT = test.isPlaceToTransition();
        }

        cutAction.setEnabled(isCutable);
        copyAction.setEnabled(isCopyable);
        pasteAction.setEnabled(isPastable);
        selectAllAction.setEnabled(true);
        delete.setEnabled(isDeletable);
        setArcMultiplicity.setEnabled(isArc);
        setArcInhibitory.setEnabled(isPtoT);
        setArcReset.setEnabled(isPtoT);
        setArcReader.setEnabled(isPtoT);
        //setArcReset.setEnabled(false);
        setTokens.setEnabled(isPlaceNode);
        setLabel.setEnabled(isPlaceNode || isTransitionNode);
        setBehavior.setEnabled(isPlaceNode || isTransitionNode);
        addSelectedTransitionsToSelectedRoles.setEnabled((isTransitionNode || areTransitionNodes) && roleSelected);
        removeSelectedTransitionsFromSelectedRoles.setEnabled((isTransitionNode || areTransitionNodes) && roleSelected);
        //convertTransitionToSubnet.setEnabled(isTransition || areTransitions || isSubnet || areSubnets);
        convertTransitionToSubnet.setEnabled(false);
        replaceSubnet.setEnabled(isSubnet || areSubnets);
        saveSubnetAs.setEnabled(isSubnet);
        openSubnet.setEnabled(isSubnet);
        closeSubnet.setEnabled(isParent);
        undo.setEnabled(getUndoManager().isUndoable());
        redo.setEnabled(getUndoManager().isRedoable());
        //setPlaceStatic.setEnabled(isPlaceNode);
        setPlaceStatic.setEnabled(false);
        
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        quitApplication();
    }

    /**
     * Terminates the application
     */
    public void quitApplication() {
        if (!this.isModified()) {
            quitNow();
        }
        mainFrame.setState(Frame.NORMAL);
        mainFrame.setVisible(true);
        int answer = JOptionPane.showOptionDialog(
        		this.getParentFrame(),
        		"Are you sure you want to exit?",
        		"Quit",
        		JOptionPane.DEFAULT_OPTION,
        		JOptionPane.WARNING_MESSAGE,
        		null,
        		new String[]{"Quit", "Cancel"},
        		"Cancel");
        if (answer == JOptionPane.YES_OPTION) {
        	quitNow();
        }
    }

    private void quitNow() {
        savePreferences();
        System.exit(0);
    }

    private JToolBar toolBar = new JToolBar();
    private JMenuBar menuBar = new JMenuBar();
    private JButton stopButton = new JButton();
    private JButton restartButton = new JButton();
    private JButton saveMarkingButton = new JButton();
    private JButton reloadFileButton = new JButton();

    private void setupFrameIcons() {
        List<Image> icons = new LinkedList<Image>();
        icons.add(GraphicsTools.getBufferedImage("icon16.png"));
        icons.add(GraphicsTools.getBufferedImage("icon32.png"));
        icons.add(GraphicsTools.getBufferedImage("icon48.png"));
        mainFrame.setIconImages(icons);
    }

    private void setupMainFrame() {

        List<FileType> openSaveFiletypes = new LinkedList<FileType>();
        openSaveFiletypes.add(new PflowFileType());
        List<FileType> importFiletypes = new LinkedList<FileType>();
        importFiletypes.add(new PipePnmlFileType());
        importFiletypes.add(new ViptoolPnmlFileType());
        List<FileType> exportFiletypes = new LinkedList<FileType>();
        exportFiletypes.add(new PipePnmlFileType());
        exportFiletypes.add(new TinaPnmlFileType());
        exportFiletypes.add(new ViptoolPnmlFileType());
        exportFiletypes.add(new EpsFileType());
        exportFiletypes.add(new PngFileType());
        List<FileType> simulateFileTypes = new LinkedList<FileType>();
        simulateFileTypes.add(new TinaPnmlFileType());
        
        Action newFile = new NewFileAction(this);
        Action openFile = new OpenFileAction(this, openSaveFiletypes);
        Action saveFile = new SaveAction(this, openSaveFiletypes);
        Action saveFileAs = new SaveFileAsAction(this, openSaveFiletypes);
        Action importFile = new ImportAction(this, importFiletypes);
        Action exportFile = new ExportAction(this, exportFiletypes);
        Action simulateNet = new SimulateAction(this, simulateFileTypes);
        Action quit = new QuitAction(this);
        Action seeBehaviorAction = new BehaviorAction(this);
        setLabel = new SetLabelAction(this);
        setBehavior = new SetBehaviorAction(this);
        setTokens = new SetTokensAction(this);
        setPlaceStatic = new SetPlaceStaticAction(this);
        setArcMultiplicity = new SetArcMultiplicityAction(this);
        setArcInhibitory = new SetArcInhibitoryAction(this);
        setArcReset = new SetArcResetAction(this);
        setArcReader = new SetArcReaderAction(this);
        addSelectedTransitionsToSelectedRoles = new AddSelectedTransitionsToSelectedRolesAction(this);
        removeSelectedTransitionsFromSelectedRoles = new RemoveSelectedTransitionsFromSelectedRolesAction(this);
        convertTransitionToSubnet = new ConvertTransitionToSubnetAction(this);
        openSubnet = new OpenSubnetAction(this);
        closeSubnet = new CloseSubnetAction(this);
        delete = new DeleteAction(this);
        graphPlaceAction = new GraphPlaceAction(this);
        graphMultiplePlacesAction = new GraphMultiplePlacesAction(this);

        cutAction = new CutAction(this);
        copyAction = new CopyAction(this);
        pasteAction = new PasteAction(this);
        selectAllAction = new SelectAllAction();

        Action selectTool_SelectionAction = new SelectionSelectToolAction(this);
        Action selectTool_PlaceAction = new PlaceSelectToolAction(this);
        Action selectTool_TransitionAction = new TransitionSelectToolAction(this);
        Action selectTool_ArcAction = new ArcSelectToolAction(this);
        Action selectTool_TokenAction = new TokenSelectToolAction(this);

        stopSimulation = new StopSimulationAction(this);
        restartNet = new RestartNetAction(this);
        saveMarking = new SaveMarkingAction(this);
        reloadFile = new ReloadFileAction(this, openSaveFiletypes);

        saveSubnetAs = new SaveSubnetAsAction(this);
        replaceSubnet = new ReplaceSubnetAction(this);

        select = new JToggleButton(selectTool_SelectionAction);
        select.setSelected(true);
        place = new JToggleButton(selectTool_PlaceAction);
        transition = new JToggleButton(selectTool_TransitionAction);
        arc = new JToggleButton(selectTool_ArcAction);
        token = new JToggleButton(selectTool_TokenAction);

        select.setText("");
        place.setText("");
        transition.setText("");
        arc.setText("");
        token.setText("");

        ButtonGroup drawGroup = new ButtonGroup();
        drawGroup.add(select);
        drawGroup.add(place);
        drawGroup.add(transition);
        drawGroup.add(arc);
        drawGroup.add(token);

        toolBar.setFloatable(false);

        /*
         * Icon toolbar (new, open, save, etc...)
         */
        toolBar.add(newFile);
        toolBar.add(openFile);
        toolBar.add(saveFile);
        toolBar.add(importFile);
        toolBar.add(exportFile);
        toolBar.addSeparator();

        toolBar.add(cutAction);
        toolBar.add(copyAction);
        toolBar.add(pasteAction);
        toolBar.addSeparator();

        toolBar.add(undo);
        toolBar.add(redo);
        toolBar.add(delete);
        toolBar.addSeparator();
        toolBar.add(select);
        toolBar.add(place);
        toolBar.add(transition);
        toolBar.add(arc);
        toolBar.add(token);
        toolBar.addSeparator();

        /* TODO NOT IMPLEMENTED YET
        toolBar.add(addSelectedTransitionsToSelectedRoles);
        toolBar.add(removeSelectedTransitionsFromSelectedRoles);
        toolBar.addSeparator();*/

        toolBar.add(simulateNet);

        stopButton = toolBar.add(stopSimulation);

        restartButton = toolBar.add(restartNet);

        saveMarkingButton = toolBar.add(saveMarking);

        reloadFileButton = toolBar.add(reloadFile);

        toolBar.add(graphMultiplePlacesAction);
        toolBar.addSeparator();

        /*
         * Top toolbar (File, Edit, Draw, etc...)
         */

        mainFrame.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);
        
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');
        menuBar.add(viewMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        menuBar.add(editMenu);

        JMenu drawMenu = new JMenu("Draw");
        drawMenu.setMnemonic('D');
        menuBar.add(drawMenu);

        JMenu elementMenu = new JMenu("Element");
        elementMenu.setMnemonic('l');
        menuBar.add(elementMenu);

        //JMenu rolesMenu = new JMenu("Roles");
        //rolesMenu.setMnemonic('R');
        //menuBar.add(rolesMenu);

        //JMenu subnetMenu = new JMenu("Subnet");
        //subnetMenu.setMnemonic('S');
        // menuBar.add(subnetMenu);

        // Algorithms
        JMenu algorithmsMenu = new JMenu("Analysis");
        algorithmsMenu.setMnemonic('A');
        menuBar.add(algorithmsMenu);

        algorithmsMenu.add(new ClassificationAction(this));
        algorithmsMenu.add(new GSPNAction(this));
        algorithmsMenu.add(new InvariantAction(this));
        algorithmsMenu.add(new MatricesAction(this));
        algorithmsMenu.add(new ReachabilityAction(this));
        algorithmsMenu.add(new ResponseTimeAction(this));
        algorithmsMenu.add(new SiphonsAction(this));

        //matrixAction = new IncidenceMatrixAction(this);
        //algorithmsMenu.add(new BoundednessAction(this));
        //algorithmsMenu.add(matrixAction);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new AboutAction(this));
        menuBar.add(helpMenu);

        fileMenu.add(newFile);
        fileMenu.add(openFile);
        fileMenu.add(saveFile);
        fileMenu.add(saveFileAs);
        fileMenu.add(importFile);
        fileMenu.add(exportFile);
        fileMenu.addSeparator();
        fileMenu.add(quit);
        
        viewMenu.add(seeBehaviorAction);

        editMenu.add(undo);
        editMenu.add(redo);
        editMenu.addSeparator();
        editMenu.add(cutAction);
        editMenu.add(copyAction);
        editMenu.add(pasteAction);
        editMenu.add(selectAllAction);
        editMenu.add(delete);

        elementMenu.add(setLabel);
        elementMenu.addSeparator();
        elementMenu.add(setTokens);
        elementMenu.add(setPlaceStatic);
        elementMenu.addSeparator();
        elementMenu.add(setArcMultiplicity);
        elementMenu.add(setArcInhibitory);
        elementMenu.add(setArcReset);
        elementMenu.add(setArcReader);

        // rolesMenu.add(addSelectedTransitionsToSelectedRoles);
        // rolesMenu.add(removeSelectedTransitionsFromSelectedRoles);

        drawMenu.add(selectTool_SelectionAction);
        drawMenu.addSeparator();
        drawMenu.add(selectTool_PlaceAction);
        drawMenu.add(selectTool_TransitionAction);
        drawMenu.add(selectTool_ArcAction);
        drawMenu.add(selectTool_TokenAction);

        // subnetMenu.add(openSubnet);
        // subnetMenu.add(closeSubnet);
        // subnetMenu.add(replaceSubnet);
        // subnetMenu.add(saveSubnetAs);
        // subnetMenu.add(convertTransitionToSubnet);

        placePopup = new JPopupMenu();
        placePopup.add(setLabel);
        placePopup.add(setTokens);
        placePopup.add(setPlaceStatic);
        placePopup.addSeparator();
        placePopup.add(graphPlaceAction);
        placePopup.addSeparator();
        placePopup.add(cutAction);
        placePopup.add(copyAction);
        placePopup.add(delete);

        transitionPopup = new JPopupMenu();
        transitionPopup.add(setLabel);
        transitionPopup.addSeparator();
        transitionPopup.add(convertTransitionToSubnet);
        transitionPopup.add(addSelectedTransitionsToSelectedRoles);
        transitionPopup.add(removeSelectedTransitionsFromSelectedRoles);
        transitionPopup.addSeparator();
        transitionPopup.add(cutAction);
        transitionPopup.add(copyAction);
        transitionPopup.add(delete);
        transitionPopup.addSeparator();
        transitionPopup.add(setBehavior);

        Font boldFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);

        canvasPopup = new JPopupMenu();
        canvasPopup.add(closeSubnet).setFont(boldFont);
        canvasPopup.add(pasteAction);

        subnetPopup = new JPopupMenu();
        subnetPopup.add(openSubnet).setFont(boldFont);
        subnetPopup.add(setLabel);
        subnetPopup.add(replaceSubnet);
        subnetPopup.add(saveSubnetAs);
        subnetPopup.add(convertTransitionToSubnet);
        subnetPopup.add(addSelectedTransitionsToSelectedRoles);
        subnetPopup.add(removeSelectedTransitionsFromSelectedRoles);
        subnetPopup.addSeparator();
        subnetPopup.add(cutAction);
        subnetPopup.add(copyAction);
        subnetPopup.add(delete);

        arcEdgePopup = new JPopupMenu();
        arcEdgePopup.add(setArcMultiplicity);

        arcEdgePopup.add(setArcInhibitory);
        arcEdgePopup.add(setArcReset);
        arcEdgePopup.add(setArcReader);

        arcEdgePopup.add(delete);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        splitPane.setDividerSize(6);
        splitPane.setOneTouchExpandable(true);
        splitPane.setLeftComponent(events.getScrollPane());
        splitPane.setRightComponent(drawingBoard);
        splitPane.setDividerLocation(150);

        mainFrame.add(splitPane, BorderLayout.CENTER);
        mainFrame.add(toolBar, BorderLayout.NORTH);

        mainFrame.addWindowListener(this);
        mainFrame.setLocation(400,200);
        mainFrame.setSize(1000, 600);
    //  mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
        mainFrame.setVisible(true);

        stopButton.setEnabled(false);

    }

    public void disableWhileSimulating(){

        for(int i=0; i<toolBar.getAccessibleContext().getAccessibleChildrenCount(); i++){
            toolBar.getComponentAtIndex(i).setEnabled(false);
        }

        for (int i=0; i<menuBar.getAccessibleContext().getAccessibleChildrenCount(); i++){
            menuBar.getComponentAtIndex(i).setEnabled(false);
        }

        simulating = true;
        stopButton.setEnabled(true);
    }

    public void enableAfterStop(){

        for(int i=0; i<toolBar.getAccessibleContext().getAccessibleChildrenCount(); i++){
            toolBar.getComponentAtIndex(i).setEnabled(true);
        }

        for (int i=0; i<menuBar.getAccessibleContext().getAccessibleChildrenCount(); i++){
            menuBar.getComponentAtIndex(i).setEnabled(true);
        }

        enableOnlyPossibleActions();

        simulating = false;
        stopButton.setEnabled(false);
    }

    public boolean isSimulating(){
        return simulating;
    }

    public Marking getCurrentMarking() {
        return getDocument().petriNet.getInitialMarking();
    }

    public void setCurrentMarking(Marking currentMarking) {
    }

    private LocalClipboard clipboard = new LocalClipboard();

    public LocalClipboard getClipboard() {
        return clipboard;
    }

    private boolean isModified = false;

    public boolean isModified() {
        return isModified;
    }

    public void setModified(boolean isModified) {
        this.isModified = isModified;
        mainFrame.setTitle(getNewWindowTitle());
    }

    public EventList getEventList()
    {
        return events;
    }

    private String getNewWindowTitle() {
    	String windowTitle = "";
    	if (getCurrentFile() != null) {
    		windowTitle += getCurrentFile().getName();
    	} else {
    		windowTitle += "Untitled";
    	}
    	if (isModified()) {
    		windowTitle += " [modified]";
    	}
    	windowTitle += " - " + getAppShortName();
    	return windowTitle;
    }

    private File currentFile = null;

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
        mainFrame.setTitle(getNewWindowTitle());
    }

    public String getAppShortName() {
        return APP_NAME;
    }

    public String getAppLongName() {
        return APP_NAME + ", version " + APP_VERSION;
    }

    public DrawingBoard getDrawingBoard() {
        return drawingBoard;
    }



}
