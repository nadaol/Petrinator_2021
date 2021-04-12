package org.petrinator.editor.commands;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Set;

import org.petrinator.petrinet.*;
import org.petrinator.util.Command;

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class PasteCommand implements Command {

    private Subnet subnet;
    private Set<Element> elements;
    private PetriNet petriNet;

    public PasteCommand(Set<Element> elements, Subnet currentSubnet, PetriNet petriNet) {
        this.subnet = currentSubnet;
        this.elements = elements;
        this.petriNet = petriNet;
        petriNet.getNodeLabelGenerator().setLabelsToPastedContent(elements);

        Point translation = calculateTranslatioToCenter(elements, currentSubnet);
        for (Element element : elements)
        {
            element.moveBy(translation.x, translation.y);

            /*
             * Let's change the labels of the nodes we just copied, for example transition becomes transition (1)
             */
            if((element instanceof Place) || (element instanceof Transition))
            {
                //String label = ((Node) element).getLabel();
                while(subnet.labelExists(((Node) element).getLabel()))  // If the label/name exists in the net
                {
                    String label = ((Node) element).getLabel();
                    try { // It's already been copied and we have to increment the number
                        int copy = Integer.parseInt(label.substring(label.indexOf("(")+1, label.indexOf(")")));
                        ((Node) element).setLabel(((Node) element).getLabel().substring(0, ((Node) element).getLabel().indexOf("(")) + "(" + (copy + 1) +")");
                    } catch (StringIndexOutOfBoundsException e) // It's never been copied so it doesn't have parentheses, we just add (1)
                    {
                        ((Node) element).setLabel(((Node) element).getLabel() + " (1)");
                    }
                }
            }
        }
    }

    public void execute()
    {
        subnet.addAll(elements);
    }

    public void undo() {
        subnet.removeAll(elements);
    }

    public void redo() {
        execute();
    }

    @Override
    public String toString() {
        return "Paste";
    }

    private Point calculateTranslatioToCenter(Set<Element> elements, Subnet currentSubnet) {
        Point viewTranslation = currentSubnet.getViewTranslation();
        Subnet tempSubnet = new Subnet();
        tempSubnet.addAll(elements);
        Rectangle bounds = tempSubnet.getBounds();

        Point result = new Point();
        result.translate(Math.round(-(float) bounds.getCenterX()), Math.round(-(float) bounds.getCenterY()));
        result.translate(-viewTranslation.x, -viewTranslation.y);
        return result;
    }

}
