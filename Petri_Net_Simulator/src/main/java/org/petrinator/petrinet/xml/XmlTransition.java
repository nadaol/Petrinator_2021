/*
 * Copyright (C) 2008-2010 Martin Riesz <riesz.martin at gmail.com>
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
package org.petrinator.petrinet.xml;

import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class XmlTransition extends XmlNode {

    @XmlElement(name = "label")
    public String label;
    
    @XmlElement(name = "behavior")
    public String behavior;

    @XmlElement(name = "rate")
    public double rate;

    @XmlElement(name = "timed")
    public boolean timed;

    @XmlElement(name = "informed")
    public boolean informed;

    @XmlElement(name = "automatic")
    public boolean automatic;

    @XmlElement(name = "guard")
    public String guard;

    @XmlElement(name = "enableWhenTrue")
    public boolean enableWhenTrue;

    @XmlElement(name = "labelVar1")
    public String labelVar1;

    @XmlElement(name = "labelVar2")
    public String labelVar2;

    @XmlElement(name = "var1")
    public double var1;

    @XmlElement(name = "var2")
    public double var2;

    @XmlElement(name = "distribution")
    public String distribution;


}
