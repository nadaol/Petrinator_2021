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
/*
package org.petrinator.auxiliar;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import net.sourceforge.jpowergraph.lens.CursorLens;
import net.sourceforge.jpowergraph.lens.LegendLens;
import net.sourceforge.jpowergraph.lens.LensSet;
import net.sourceforge.jpowergraph.lens.NodeSizeLens;
import net.sourceforge.jpowergraph.lens.RotateLens;
import net.sourceforge.jpowergraph.lens.TooltipLens;
import net.sourceforge.jpowergraph.lens.ZoomLens;
import net.sourceforge.jpowergraph.swing.viewcontrols.CursorControlPanel;
import net.sourceforge.jpowergraph.swing.viewcontrols.LegendControlPanel;
import net.sourceforge.jpowergraph.swing.viewcontrols.NodeSizeControlPanel;
import net.sourceforge.jpowergraph.swing.viewcontrols.RotateControlPanel;
import net.sourceforge.jpowergraph.swing.viewcontrols.TooltipControlPanel;
import net.sourceforge.jpowergraph.swing.viewcontrols.ZoomControlPanel;
import net.sourceforge.powerswing.panel.PPanel;

/**r embedded use in applications.[2] Lua is cross-platform, since the interpreter of compiled bytecode is written in ANSI C,[3] and Lua has a relatively simple C API to embed it into applications.[4]

Lua was originally designed in 1993 a
 * @author Mick Kerrigan
 */

/*
public class SwingJGraphViewPane extends JPanel
{

    private JPanel startPanel;
    private JPanel endPanel;

    public SwingJGraphViewPane(JComponent theJComponent, LensSet theLensSet, boolean allowContributions) {
        super();
        startPanel = new JPanel();
        endPanel = new JPanel();
        CursorControlPanel cursorControl = new CursorControlPanel((CursorLens) theLensSet.getFirstLensOfType(CursorLens.class));
        ZoomControlPanel zoomControl = new ZoomControlPanel((ZoomLens) theLensSet.getFirstLensOfType(ZoomLens.class));
        RotateControlPanel rotateControl = new RotateControlPanel((RotateLens) theLensSet.getFirstLensOfType(RotateLens.class));
        NodeSizeControlPanel nodeSizeControl = new NodeSizeControlPanel((NodeSizeLens) theLensSet.getFirstLensOfType(NodeSizeLens.class));
        LegendControlPanel legendControl = new LegendControlPanel((LegendLens) theLensSet.getFirstLensOfType(LegendLens.class));
        TooltipControlPanel tooltipControl = new TooltipControlPanel((TooltipLens) theLensSet.getFirstLensOfType(TooltipLens.class));

        this.setLayout(new BorderLayout());

        JPanel toolTipPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toolTipPanel.add(legendControl);
        toolTipPanel.add(tooltipControl);


        PPanel top;
        top = new PPanel(1, 2, 2, 4, new Object[]{"", "0", "0", "0", zoomControl, nodeSizeControl }, 0, 0, 0, 0);

        this.add(new PPanel(2, 1, 0, 0, new Object[]{"", "0,1", "0", top, "0,1", theJComponent,}));
    }

    public JPanel getContributePanel() {
        return startPanel;
    }

    public JPanel getEndContributeComposite() {
        return endPanel;
    }
}
*/