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

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class NodeIdGenerator {
    //TODO estaban static, ver si hay algun error al sacarlo
    private int nextUniquePlaceNumber = 1;
    private int nextUniqueTransitionNumber = 1;
    private int nextUniqueSubnetNumber = 1;
    private int nextUniqueReferencePlaceNumber = 1;

    public void setUniqueId(Node node)
    {
        String id;
        if (node instanceof Place) {
            id = "P" + Integer.toString(nextUniquePlaceNumber++);
        } else if (node instanceof Transition) {
            id = "T" + Integer.toString(nextUniqueTransitionNumber++);
        } else if (node instanceof Subnet) {
            id = "S" + Integer.toString(nextUniqueSubnetNumber++);
        } else if (node instanceof ReferencePlace) {
            id = "RP" + Integer.toString(nextUniqueReferencePlaceNumber++);
        } else {
            throw new RuntimeException("Node which is not Place, Transition, Subnet neither ReferencePlace");
        }
        node.setId(id);
    }

    public void resetUniqueIds(){
        nextUniquePlaceNumber = 1;
        nextUniqueReferencePlaceNumber = 1;
        nextUniqueSubnetNumber = 1;
        nextUniqueTransitionNumber = 1;
    }

    public void fixFutureUniqueIds(Subnet rootSubnet) {
        int maxPlaceNumber = 0;
        int maxTransitionNumber = 0;
        int maxSubnetNumber = 0;
        int maxReferencePlaceNumber = 0;

        for (Place place : rootSubnet.getPlacesRecursively()) {
            String placeId = place.getId();
            if (placeId.toUpperCase().startsWith("P")) {
                try {
                    int placeNumber = Integer.parseInt(placeId.substring(1));
                    if (placeNumber > maxPlaceNumber) {
                        maxPlaceNumber = placeNumber;
                    }
                } catch (NumberFormatException ex) {
                    //do nothing
                }
            }
        }
        for (Transition transition : rootSubnet.getTransitionsRecursively()) {
            String transitionId = transition.getId();
            if (transitionId.toUpperCase().startsWith("T")) {
                try {
                    int transitionNumber = Integer.parseInt(transitionId.substring(1));
                    if (transitionNumber > maxTransitionNumber) {
                        maxTransitionNumber = transitionNumber;
                    }
                } catch (NumberFormatException ex) {
                    //do nothing
                }
            }
        }
        for (Subnet subnet : rootSubnet.getSubnetsRecursively()) {
            String subnetId = subnet.getId();
            if (subnetId.toUpperCase().startsWith("S")) {
                try {
                    int subnetNumber = Integer.parseInt(subnetId.substring(1));
                    if (subnetNumber > maxSubnetNumber) {
                        maxSubnetNumber = subnetNumber;
                    }
                } catch (NumberFormatException ex) {
                    //do nothing
                }
            }
        }
        for (ReferencePlace referencePlace : rootSubnet.getReferencePlacesRecursively()) {
            String referencePlaceId = referencePlace.getLabel();
            if (referencePlaceId.toUpperCase().startsWith("RP")) {
                try {
                    int referencePlaceNumber = Integer.parseInt(referencePlaceId.substring(1));
                    if (referencePlaceNumber > maxReferencePlaceNumber) {
                        maxReferencePlaceNumber = referencePlaceNumber;
                    }
                } catch (NumberFormatException ex) {
                    //do nothing
                }
            }
        }

        nextUniquePlaceNumber = maxPlaceNumber + 1;
        nextUniqueTransitionNumber = maxTransitionNumber + 1;
        nextUniqueSubnetNumber = maxSubnetNumber + 1;
        nextUniqueReferencePlaceNumber = maxReferencePlaceNumber + 1;
    }

}
