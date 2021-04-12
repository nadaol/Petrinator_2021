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
package org.petrinator.petrinet;

import java.util.Set;


public class NodeSimpleIdGenerator extends NodeIdGenerator{

    private PetriNet petriNet;

    public NodeSimpleIdGenerator(PetriNet petriNet) {
        this.petriNet = petriNet;
    }

    public void setUniqueId(Node node) {
    	super.setUniqueId(node);
    }

    public void fixFutureUniqueIds() {
        super.fixFutureUniqueIds(petriNet.getRootSubnet());
    }

    public void ensureNumberIds() {
        for (Node node : petriNet.getRootSubnet().getNodesRecursively()) {
            try {
                Integer.parseInt(node.getId().substring(1));
            } catch (NumberFormatException ex) {
                setUniqueId(node);
            }
        }
    }

    public void resetUniqueIds() {
        super.resetUniqueIds();
    }
}
