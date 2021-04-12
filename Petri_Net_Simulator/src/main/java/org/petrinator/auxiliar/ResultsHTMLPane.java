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

package org.petrinator.auxiliar;

import pipe.gui.widgets.ButtonBar;
import pipe.gui.widgets.FileBrowser;
import pipe.utilities.math.Matrix;
import pipe.views.PetriNetViewComponent;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ResultsHTMLPane extends JPanel implements HyperlinkListener
{
    private JEditorPane results;
    private File defaultPath;
    private final Clipboard clipboard = this.getToolkit().getSystemClipboard();
    private ButtonBar copyAndSaveButtons;
    private final JProgressBar progressBar = new JProgressBar();

    public static final String HTML_STYLE = "<style type=\"text/css\">" +
            "body{font-family:Arial,Helvetica,sans-serif;text-align:center;"+
            "background:#ffffff}" +
            "td.colhead{font-weight:bold;text-align:center;" +
            "background:#A9D0F5}" +
            "td.rowhead{font-weight:bold;background:#A9D0F5}"+
            "td.cell{text-align:center;padding:5px,0; background:#ffffff}" +
            "tr.even{background:#ffffff}" +
            "tr.odd{background:#ffffff}" +
            "td.empty{background:#A9D0F5}" +
            "</style>";

    public ResultsHTMLPane(String path)
    {
        super(new BorderLayout());

        if(path == null)
        {
            try
            {
                defaultPath = File.createTempFile(  "PIPE", ".xml" ).getParentFile();
                defaultPath.deleteOnExit();
                System.out.println("defaultpath: " + defaultPath);
            } catch (IOException e)
            {
                throw new RuntimeException("Cannot create temp file. " + "Save net before running analysis modules." );
            }
        }
        else
        {
            defaultPath = new File(path);
            if(defaultPath.isFile())
            {
                defaultPath = defaultPath.getParentFile();
            }
        }

        results = new JEditorPane();
        results.setEditable(false);
        results.setMargin(new Insets(5,5,5,5));
        results.setContentType("text/html");
        results.addHyperlinkListener(this);
        JScrollPane scroller=new JScrollPane(results);
        scroller.setPreferredSize(new Dimension(400,300));
        scroller.setBorder(new BevelBorder(BevelBorder.LOWERED));
        this.add(scroller);

        ActionListener copyHandler = new ActionListener()
        {

            public void actionPerformed(ActionEvent arg0)
            {
                StringSelection data = new StringSelection(results.getText());
                try
                {
                    clipboard.setContents(data, data);
                }
                catch(IllegalStateException e)
                {
                    System.out.println("Error copying to clipboard, seems it's busy?");
                }
            }
        };
        ActionListener saveHandler = new ActionListener()
        {

            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    FileBrowser fileBrowser = new FileBrowser("HTML file", "html", defaultPath.getPath());
                    String destFN = fileBrowser.saveFile();
                    if(!destFN.toLowerCase().endsWith(".html"))
                    {
                        destFN += ".html";
                    }
                    FileWriter writer = new FileWriter(new File(destFN));
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
                    System.out.println("Error saving HTML to file");
                }
            }
        };

        copyAndSaveButtons = new ButtonBar(new String[]{"Copy","Save"}, new ActionListener[]{copyHandler, saveHandler});
        copyAndSaveButtons.setButtonsEnabled(false);

        JPanel panel = new JPanel();
        // progressBar = new JProgressBar();

        progressBar.setString("");
        progressBar.setStringPainted(true);


        //progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add( progressBar, BorderLayout.NORTH);
        panel.add( copyAndSaveButtons, BorderLayout.SOUTH);

        //panel.add(pb);
        this.add( panel, BorderLayout.PAGE_END);/* */

        //this.add( copyAndSaveButtons, BorderLayout.PAGE_END);
        //this.setBorder(new TitledBorder(new EtchedBorder(),"Results"));

    }


    public void setText(String text)
    {
        results.setText("<html><head>" + HTML_STYLE + "</head><body>" + text + "</body></html>");
        results.setCaretPosition(0); // scroll to top
    }

    public String getText() {
        return results.getText();
    }


    public void setIndeterminateProgressBar(boolean flag) {
        progressBar.setIndeterminate(flag);
    }


    public void setVisibleProgressBar(boolean flag){
        progressBar.setVisible(flag);
    }


    public void setStringProgressBar(String text){
        progressBar.setString(text);
    }


    //<pere>
    @Override
    public void setEnabled(boolean flag) {
        //copyAndSaveButtons.setEnabled(flag);
        //copyAndSaveButtons.setButtonsEnabled(flag);
    }
    //</pere>

    public static String makeTable(Object[] items, int cols, boolean showLines,
                                   boolean doShading, boolean columnHeaders, boolean rowHeaders) {
        StringBuilder s = new StringBuilder();
        //s.append(items[0]);
        //items[0] = "";
        s.append("<br>");
        s.append("<table border=").append(showLines ? 1 : 0).append(" cellspacing = 2>");
        int j = 0;
        for (int i=0; i < items.length; i++) {
            if (j==0) {
                s.append("<tr").append(doShading ? " class=" + (i / cols % 2 == 1 ? "odd>" : "even>")
                        : ">");
            }
            s.append("<td class=");
            if (i==0 && ((String) items[i]).length() != 0) {
                s.append("cell>");
            } else if(i == 0) {
                s.append("empty>");
            } else if ((j == 0) && rowHeaders) {
                s.append("rowhead>");
            } else if ((i < cols) && columnHeaders) {
                s.append("colhead>");
            } else {
                s.append("cell>");
            }
            s.append(items[i]).append("</td>");


            if ( ++j == cols) {
                s.append("</tr>");
                j=0;
            }
        }
        s.append("</table>");
        return s.toString();
    }

    public static String makeTable(Matrix matrix, PetriNetViewComponent[] name, boolean showLines, boolean doShading, boolean columnHeaders, boolean rowHeaders)
    {
        int cols = name.length;
        int k[] = matrix.getColumnPackedCopy();

        StringBuilder s = new StringBuilder();
        s.append("<table border=").append(showLines ? 1 : 0).append(" cellspacing=2>");
        s.append("<tr").append(doShading ? " class= odd>" : ">");

        for (int i = 0; i< cols; i++)
        {
            if ((i == 0) && rowHeaders)
            {
                s.append("<td class=empty> </td>");
            }
            s.append("<td class=").append(columnHeaders ? "colhead>" : "cell>").append(name[i].getName()).append("</td>");
        }
        s.append("</tr>");

        int j = 0;
        for (int i = 0; i< k.length; i++)
        {
            if (j == 0)
            {
                s.append("<tr").append(doShading ? " class=" +
                        (i / cols % 2 == 1 ? "odd>" : "even>") : ">");
            }
            if ((j == 0) && rowHeaders)
            {
                s.append("<td class=empty></td>");
            }
            s.append("<td class=cell>").append(k[i]).append("</td>");
            if (++j == cols)
            {
                s.append("</tr>");
                j=0;
            }
        }
        s.append("</table>");
        return s.toString();
    }



    public static void makeTable(Object[] items, int cols, boolean showLines, boolean doShading, boolean columnHeaders, boolean rowHeaders, BufferedWriter writer)
    {
        try
        {
            writer.append("<table border=" + (showLines ? 1 : 0)+" cellspacing=2>");
            int j = 0;
            for (int i=0; i < items.length; i++)
            {
                if (j==0)
                {
                    writer.append("<tr").append(doShading ? " class=" + (i / cols % 2 == 1 ? "odd>" : "even>")
                            : ">");
                }
                writer.append("<td class=");
                if (i==0 && items[i]=="") {
                    writer.append("empty>");
                } else if ((j == 0) && rowHeaders)
                {
                    writer.append("rowhead>");
                } else if ((i < cols) && columnHeaders)
                {
                    writer.append("colhead>");
                } else {
                    writer.append("cell>");
                }
                writer.append(String.valueOf(items[i])).append("</td>");
                if ( ++j == cols)
                {
                    writer.append("</tr>");
                    j = 0;
                }
            }
            writer.append("</table>");
        } catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }


    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
            if (e.getDescription().startsWith("#"))
            {
                results.scrollToReference(e.getDescription().substring(1));
            }
            else
            {
                try
                {
                    results.setPage(e.getURL());
                } catch (IOException ex)
                {
                    System.err.println("Error changing page to " + e.getURL());
                }
            }
        }
    }


}
