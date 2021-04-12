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

package org.petrinator.editor.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.*;

import net.miginfocom.swing.MigLayout;
import org.petrinator.editor.Root;
import org.petrinator.editor.commands.SetLabelCommand;;
import org.petrinator.petrinet.TransitionNode;
import org.petrinator.util.GraphicsTools;

/**
 * Set behavior to clicked transition
 *
 * @author Leandro Asson leoasson at gmail.com
 */
public class SetBehaviorAction extends AbstractAction{
	private Root root;

    public SetBehaviorAction(Root root) {
        this.root = root;
        String name = "Properties";
        putValue(NAME, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/Behavior16.gif"));
        putValue(SHORT_DESCRIPTION, name);
        setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        if (root.getClickedElement() != null && root.getClickedElement() instanceof TransitionNode)
        {
			TransitionNode clickedTransition = (TransitionNode) root.getClickedElement();
			String newBehavior;
			String guardValue;
			boolean automatic;
			boolean informed;
			boolean enablewhentrue;
			boolean timed;
			String distribution;
			double var1;
			double var2;
			String label_var1;
			String label_var2;
                             
            JTextField field_guard = new JTextField(8);
            JTextField field_label = new JTextField(8);
			//JTextField field_rate = new JTextField(8);
			JLabel Jlabel_var1 = new JLabel("μ (1/λ)");
			JLabel Jlabel_var2 = new JLabel("σ²  ");
			JTextField value_var1 = new JTextField();
			JTextField value_var2 = new JTextField();
            JCheckBox checkBoxAutomatic = new JCheckBox();
            JCheckBox checkBoxInformed = new JCheckBox();
            JCheckBox checkBoxEnablewhentrue = new JCheckBox();
            JCheckBox checkBoxTimed = new JCheckBox();
            JPanel myPanel = new JPanel();
			JComboBox<String> comboBoxDistribution;


            myPanel.setLayout(new MigLayout());
			myPanel.add(new JLabel("Label:  "));
			myPanel.add(field_label,"span, grow");
			myPanel.add(new JLabel(""), "wrap");
			myPanel.add(new JLabel(""), "wrap");
			myPanel.add(new JSeparator(), "span, growx, wrap");
			myPanel.add(new JLabel(""), "wrap");
			myPanel.add(new JLabel(""), "wrap");
			myPanel.add(new JLabel("Automatic:"));
			myPanel.add(checkBoxAutomatic);
			myPanel.add(new JLabel(""));
			myPanel.add(new JLabel("Informed:"));
			myPanel.add(checkBoxInformed, "al right, wrap");
			myPanel.add(new JLabel("Timed:"));
			myPanel.add(checkBoxTimed);
			myPanel.add(new JLabel(""));
			//myPanel.add(new JLabel("Rate (λ):  "));
			//myPanel.add(field_rate, "wrap");
			myPanel.add(new JLabel(""), "wrap");
			myPanel.add(new JLabel(""), "wrap");
			myPanel.add(new JSeparator(), "span, growx, wrap");

			//myPanel.add(new JLabel(""), "wrap");
			//myPanel.add(new JLabel(""), "wrap");
			//myPanel.add(new JLabel("Enable when true:"));
			//myPanel.add(checkBoxEnablewhentrue);
			//myPanel.add(new JLabel("    "));
			//myPanel.add(new JLabel("Guard:  "));
			//myPanel.add(field_guard, "wrap");

			myPanel.add(new JLabel("Distribution: "));
			String[] choice = {"Exponential", "Normal","Cauchy", "Uniform"};
			comboBoxDistribution = new JComboBox<String>(choice);
			myPanel.add(comboBoxDistribution,"span, grow");
			myPanel.add(new JLabel(" "));
			myPanel.add(Jlabel_var1);
			myPanel.add(new JLabel(" "));
			myPanel.add(value_var1);
			myPanel.add(new JLabel("seg."),"wrap, grow");

			myPanel.add(new JLabel(" "));
			myPanel.add(Jlabel_var2);
			myPanel.add(new JLabel(" "));
			myPanel.add(value_var2);
			myPanel.add(new JLabel("seg."),"wrap, grow");

			//set in the panel the behavior of the transition.
			field_label.setText(clickedTransition.getLabel());
			field_guard.setText(clickedTransition.getGuard());
			//field_rate.setText(Double.toString(clickedTransition.getRate()));
			checkBoxAutomatic.setSelected(clickedTransition.isAutomatic());
			checkBoxInformed.setSelected(clickedTransition.isInformed());
			checkBoxEnablewhentrue.setSelected(clickedTransition.isEnablewhentrue());
			checkBoxTimed.setSelected(clickedTransition.isTimed());
			checkBoxInformed.setEnabled(false);
			value_var1.setText(Double.toString(clickedTransition.getVar1()));
			value_var2.setText(Double.toString(clickedTransition.getVar2()));
			Jlabel_var1.setText(clickedTransition.getLabelVar1());
			Jlabel_var2.setText(clickedTransition.getLabelVar2());
			comboBoxDistribution.setSelectedIndex(clickedTransition.getIndexDistribution());

			if(clickedTransition.isTimed())
			{
				//field_rate.setEnabled(true);
				checkBoxAutomatic.setEnabled(false);
				comboBoxDistribution.setEnabled(true);
				if(comboBoxDistribution.getSelectedItem().toString().equals("Exponential"))
				{
					value_var1.setText(Double.toString(clickedTransition.getRate()));
					value_var1.setEnabled(true);
					value_var2.setEnabled(false);
				}
			}
			else
			{
				//field_rate.setEnabled(false);
				comboBoxDistribution.setEnabled(false);
				value_var1.setEnabled(false);
				value_var2.setEnabled(false);
			}

			//Interrupt when the distribution change.
			comboBoxDistribution.addActionListener (new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if(comboBoxDistribution.getSelectedItem().toString().equals("Normal"))
					{
						Jlabel_var1.setText("μ (1/λ)");
						Jlabel_var2.setText("σ²");
						value_var1.setEnabled(true);
						value_var2.setEnabled(true);
						value_var1.setText(Double.toString(1/clickedTransition.getRate()));
					}
					else if(comboBoxDistribution.getSelectedItem().toString().equals("Cauchy"))
					{
						Jlabel_var1.setText("x");
						Jlabel_var2.setText("y");
						value_var1.setEnabled(true);
						value_var2.setEnabled(true);
					}
					else if(comboBoxDistribution.getSelectedItem().toString().equals("Exponential"))
					{
						Jlabel_var1.setText("Rate (λ)");
						Jlabel_var2.setText(" ");
						value_var1.setEnabled(true);
						value_var2.setEnabled(false);
						value_var1.setText(Double.toString(clickedTransition.getRate()));
					}
					else if(comboBoxDistribution.getSelectedItem().toString().equals("Uniform"))
					{
						Jlabel_var1.setText("lower");
						Jlabel_var2.setText("upper");
						value_var1.setEnabled(true);
						value_var2.setEnabled(true);
					}
					myPanel.revalidate();
					myPanel.repaint();
				}
			});


			//Interrupt when the timed comboBox change.
			checkBoxTimed.addItemListener(new ItemListener(){
				@Override
				public void itemStateChanged(ItemEvent e) {
					if(e.getStateChange() == ItemEvent.SELECTED){

						//field_rate.setEnabled(true);
						//field_rate.setText(Double.toString(clickedTransition.getRate()));
						checkBoxAutomatic.setEnabled(false);
						checkBoxAutomatic.setSelected(true);
						comboBoxDistribution.setEnabled(true);
						Jlabel_var1.setText(clickedTransition.getLabelVar1());
						Jlabel_var2.setText(clickedTransition.getLabelVar2());
						comboBoxDistribution.setSelectedIndex(clickedTransition.getIndexDistribution());

						if(comboBoxDistribution.getSelectedItem().toString().equals("Exponential")) {
							value_var1.setEnabled(true);//
							value_var1.setText(Double.toString(clickedTransition.getRate()));//
						}
						else
						{
							value_var1.setEnabled(true);//
							value_var2.setEnabled(true);//
							value_var1.setText(Double.toString(clickedTransition.getVar1()));
							value_var2.setText(Double.toString(clickedTransition.getVar2()));
						}
					}
					else if(e.getStateChange() == ItemEvent.DESELECTED){
						//field_rate.setEnabled(false);
						//field_rate.setText(Double.toString(clickedTransition.getRate()));
						checkBoxAutomatic.setEnabled(true);
						comboBoxDistribution.setEnabled(false);
						value_var1.setEnabled(false);//
						value_var2.setEnabled(false);//
					}
					myPanel.validate();
					myPanel.repaint();
				}
			});

            int result = JOptionPane.showConfirmDialog(root.getParentFrame(), myPanel,
                    "Transition properties", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, GraphicsTools.getIcon("pneditor/Behavior32.png"));
            if (result == JOptionPane.OK_OPTION)
            {
            	//When the ok button is push. The values are saved.
               	guardValue = field_guard.getText();
               	automatic = checkBoxAutomatic.isSelected();
               	informed = checkBoxInformed.isSelected();
               	timed = checkBoxTimed.isSelected();
               	enablewhentrue = checkBoxEnablewhentrue.isSelected();
               	clickedTransition.setGuard(field_guard.getText());
               	root.getUndoManager().executeCommand(new SetLabelCommand(clickedTransition,field_label.getText()));
               	clickedTransition.generateBehavior(automatic,informed,guardValue,enablewhentrue);
               	clickedTransition.setAutomatic(automatic);
               	clickedTransition.setInformed(informed);
               	clickedTransition.setEnableWhenTrue(enablewhentrue);
               	clickedTransition.setTime(timed);

               	//Check if the rate number is a valid number
               	//try {
				//	clickedTransition.setRate(Double.parseDouble(field_rate.getText()));
			 	//}
			   	//catch(NumberFormatException e1) {
				//   JOptionPane.showMessageDialog(null, "Invalid number");
				//   return; // Don't execute further code
			    //	}

			   	//if a timed transition, the stochastic properties are saved

               	if(timed)
			   	{
					try
					{
						distribution = comboBoxDistribution.getSelectedItem().toString();
						var1 = Double.parseDouble(value_var1.getText());
						var2 = Double.parseDouble(value_var2.getText());
						label_var1 = Jlabel_var1.getText();
						label_var2 = Jlabel_var2.getText();

						if(distribution.equals("Uniform") && var1 >= var2)
						{
							JOptionPane.showMessageDialog(null, "Lower bound must be strictly less than upper bound ");
						}
						else
						{
							clickedTransition.setVar1(var1);
							clickedTransition.setVar2(var2);
							clickedTransition.setLabelvar1(label_var1);
							clickedTransition.setLabelVar2(label_var2);
							clickedTransition.setDistribution(distribution);
							//if is a normal distribution set a new rate (1/media)
							if(distribution.equals("Normal"))
							{
								clickedTransition.setRate(1/var1);
							}
							if(distribution.equals("Exponential"))
							{
								clickedTransition.setRate(var1);
							}
						}
						//System.out.println("Distribution: " + clickedTransition.getDistribution() + ", var1: " + clickedTransition.getVar1() + ", var2: "+ clickedTransition.getVar2() + ", rate:" + clickedTransition.getRate());
					}
					catch (NumberFormatException n)
					{
						JOptionPane.showMessageDialog(null, "Invalid number.");
					}
			   	}
			   else
			   {
				   clickedTransition.setDistribution("Exponential");
				   clickedTransition.setVar1(1);
				   clickedTransition.setVar2(1);
			   }
            }
         }
    }
}