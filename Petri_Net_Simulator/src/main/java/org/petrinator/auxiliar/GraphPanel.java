/*
 * Copyright (C) 2016-2017 Joaquin Rodriguez Felici <joaquinfelici at gmail.com>
 * Copyright (C) Rodrigo Castro
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

import org.petrinator.editor.Root;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;

public class GraphPanel extends JPanel
{
    private int width = 600;
    private int heigth = 500;
    private int padding = 25;
    private int labelPadding = 25;
    private int shiftLeft = 25;
    private Root root;
    private Color gridColor = new Color(200, 200, 200, 200);
    private static final Stroke GRAPH_STROKE = new BasicStroke(2f);
    private int pointWidth = 4;
    private int numberYDivisions = 10;
    private List<List<Double>> vectors;
    private List<Color> colors = new ArrayList<Color>();
    private List<String> names = new ArrayList<String>();

    public GraphPanel(Root root, List<List<Double>> vectors, List<String> names)
    {
        this.vectors = vectors;
        this.names = names;
        this.root = root;
        colors.add(new Color(25, 7, 99));
        colors.add(new Color(180,4,5));
        colors.add(new Color(0, 150, 39));
        colors.add(new Color(0, 141, 150));
        colors.add(new Color(190, 174, 0));
        shiftLeft = 25 + getLongestLabel() * 8;
        width += getLongestLabel() * 12;

        setUp();
    }

    private void setUp()
    {
        this.setPreferredSize(new Dimension(width, heigth));
        JDialog frame = new JDialog(root.getParentFrame(), "Places history");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double xScale = ((double) getWidth()-shiftLeft - (2 * padding) - labelPadding) / (Collections.max(vectors.get(0)));
        double yScale = ((double) getHeight() - 2 * padding - labelPadding) / (getMaxScore() - getMinScore());

        // Draw white background
        g2.setColor(Color.WHITE);
        g2.fillRect(padding + labelPadding, padding, getWidth()-shiftLeft - (2 * padding) - labelPadding, getHeight() - 2 * padding - labelPadding);
        g2.setColor(Color.BLACK);

        // Create hatch marks and grid lines for y axis.
        for (int i = 0; i < numberYDivisions + 1; i++)
        {
            int x0 = padding + labelPadding;
            int x1 = pointWidth + padding + labelPadding;
            int y0 = getHeight() - ((i * (getHeight() - padding * 2 - labelPadding)) / numberYDivisions + padding + labelPadding);
            int y1 = y0;
            if (vectors.get(1).size() > 0)
            {
                g2.setColor(gridColor);
                g2.drawLine(padding + labelPadding + 1 + pointWidth, y0, getWidth()-shiftLeft - padding, y1);
                g2.setColor(Color.BLACK);
                String yLabel = ((int) ((getMinScore() + (getMaxScore() - getMinScore()) * ((i * 1.0) / numberYDivisions)) * 100)) / 100.0 + "";
                FontMetrics metrics = g2.getFontMetrics();
                int labelWidth = metrics.stringWidth(yLabel);
                g2.drawString(yLabel, x0 - labelWidth - 5, y0 + (metrics.getHeight() / 2) - 3);
            }
            g2.drawLine(x0, y0, x1, y1);
        }

        // For x axis
        int numberXDivisions = 15;
        if(vectors.get(0).size() < numberXDivisions)
            numberXDivisions = vectors.get(0).size();

        for (int i = 0; i < numberXDivisions; i++) {
            if (vectors.get(1).size() > 1) {
                int x0 = i * (getWidth()-shiftLeft - padding * 2 - labelPadding) / (numberXDivisions-1) + padding + labelPadding;
                int x1 = x0;
                int y0 = getHeight() - padding - labelPadding;
                int y1 = y0 - pointWidth;
                if ((i % ((int) ((numberXDivisions / 20.0)) + 1)) == 0) {
                    g2.setColor(gridColor);
                    g2.drawLine(x0, getHeight() - padding - labelPadding - 1 - pointWidth, x1, padding);
                    g2.setColor(Color.BLACK);
                    String xLabel = vectors.get(0).get(i) + "";
                    FontMetrics metrics = g2.getFontMetrics();
                    int labelWidth = metrics.stringWidth(xLabel);
                    g2.drawString(xLabel, x0 - labelWidth / 2, y0 + metrics.getHeight() + 3);

                    if(i == numberXDivisions - 1)
                    {
                        g2.drawString("[s]", x0 - labelWidth / 2 + 35, y0 + metrics.getHeight() + 3);
                    }
                    else if(i == 3)
                    {
                        g2.drawString("[tokens vs. seconds]", x0 - labelWidth / 2 + 50, y0 + metrics.getHeight() + 22);
                    }
                }
                g2.drawLine(x0, y0, x1, y1);
            }
        }

        // Create x and y axes
        g2.drawLine(padding + labelPadding, getHeight() - padding - labelPadding, padding + labelPadding, padding);
        g2.drawLine(padding + labelPadding, getHeight() - padding - labelPadding, getWidth()-shiftLeft - padding, getHeight() - padding - labelPadding);

        int count = 35;

        // Draw the dots and lines according to the arrays in the vectors list
        for(int k = 1; k < vectors.size(); k++)
        {
            List<Point> graphPoints = new ArrayList<>();
            for (int i = 0; i < vectors.get(k).size(); i++)
            {
                int x1 = (int) (vectors.get(0).get(i) * xScale + padding + labelPadding);
                int y1 = (int) ((getMaxScore() - vectors.get(k).get(i)) * yScale + padding);

                if(i != 0) // Add point to get a squared wave
                {
                    int y2 = (int) ((getMaxScore() - vectors.get(k).get(i-1)) * yScale + padding); // The point has the same x position as the new one and the same y position as the last one.
                    graphPoints.add(new Point(x1,y2));
                }

                graphPoints.add(new Point(x1, y1)); // Add normal point
            }

            Stroke oldStroke = g2.getStroke();
            g2.setColor(colors.get((k-1)%5));
            g2.setStroke(GRAPH_STROKE);
            for (int i = 0; i < graphPoints.size() - 1; i++) {
                int x1 = graphPoints.get(i).x;
                int y1 = graphPoints.get(i).y;
                int x2 = graphPoints.get(i + 1).x;
                int y2 = graphPoints.get(i + 1).y;
                g2.drawLine(x1, y1, x2, y2);

                if(i == graphPoints.size()-2)
                {
                    g2.drawString(names.get(k), x2+33, count);
                    g2.fillRect(x2+15, count-10, 8, 8);
                    count += 20;
                }
                //g2.drawString(names.get(k), x2+6, y2+5);
            }


            g2.setStroke(oldStroke);
            g2.setColor(colors.get((k-1)%5));
            for (int i = 0; i < graphPoints.size(); i++) {
                int x = graphPoints.get(i).x - pointWidth / 2;
                int y = graphPoints.get(i).y - pointWidth / 2;
                int ovalW = pointWidth;
                int ovalH = pointWidth;
                // g2.fillOval(x, y, ovalW, ovalH); // Draw circle on each point
            }
        }
    }

    private double getMinScore()
    {
        double minScore = Double.MAX_VALUE;
        for(List<Double> vector : vectors)
        {
            if(vectors.indexOf(vector) != 0)
            {
                for (Double score : vector)
                {
                    minScore = Math.min(minScore, score);
                }
            }
        }
        return minScore;
    }

    public int getLongestLabel()
    {
        int longest = names.get(0).length();
        for(String label : names)
        {
            if(label.length() > longest)
                longest = label.length();
        }
        return longest;
    }

    private double getMaxScore()
    {
        double maxScore = Double.MIN_VALUE;
        for(List<Double> vector : vectors)
        {
            if(vectors.indexOf(vector) != 0)
            {
                for (Double score : vector)
                {
                    maxScore = Math.max(maxScore, score);
                }
            }
        }
        return maxScore;
    }
}