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

import javax.swing.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Joaquin Rodriguez Felici <joaquinfelici at gmail.com>
 */
public class EventList
{
    List<String> events;
    JList<String> list;
    JScrollPane scroller;
    Color scrollerColor = new Color(220,220,220);

    public EventList()
    {
        events = new ArrayList<String>();
        list = new JList(events.toArray());
        list.setBackground(scrollerColor);
        scroller = new JScrollPane(list);
    }

    /*
     * @brief Adds event to the list and, therefore, the scrollPane
     * @param e The string that wants to be added. Ex: "T1 was fired!"
     */
    public void addEvent(String e)
    {
        events.add(e);
        list = new JList(events.toArray());
        list.setBackground(scrollerColor);
        scroller = new JScrollPane(list);
        scrollToBottom(scroller);
    }

    /**
     * Resets event list, clearing the scrollPane
     */

    public void resetEvents(){
        events.clear();
        list = new JList(events.toArray());
        list.setBackground(scrollerColor);
        scroller = new JScrollPane(list);
        scrollToBottom(scroller);
    }

    /*
     * @brief Provides with the scrollPane
     * @return scroller
     */
    public JScrollPane getScrollPane()
    {
        return scroller;
    }

    /*
     * @brief Scrolls down to the bottom of the list when a new event is aded
     * @param scrollPane the ScrollPane object we want to scroll
     */
    private void scrollToBottom(JScrollPane scrollPane)
    {
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        AdjustmentListener downScroller = new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                Adjustable adjustable = e.getAdjustable();
                adjustable.setValue(adjustable.getMaximum());
                verticalBar.removeAdjustmentListener(this);
            }
        };
        verticalBar.addAdjustmentListener(downScroller);
    }
}
