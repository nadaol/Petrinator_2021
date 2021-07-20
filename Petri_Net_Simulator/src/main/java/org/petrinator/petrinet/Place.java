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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents place in Petri net
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class Place extends PlaceNode implements Cloneable {

    public static int REGULAR = 0;
    public static int CONTROL = 1;
    private boolean isStatic = false;
    List<Double> values = new ArrayList<Double>();
    //nuevo para agregarle un atributo tipo a la plaza
    private int type = REGULAR;

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    //get y set types
    @Override
    public int getType(){return type;}

    @Override
    public void setType(int type){
        this.type=type;
    }

    public void addValue(int value)
    {
        values.add(new Double(value));
    }

    public void printValues()
    {
        System.out.print("[ ");
        for(int i = 0; i<values.size(); i++)
        {
            System.out.print(values.get(i) + " ");
        }
        System.out.println("]");
    }

    public void clearValues()
    {
        values.clear();
    }

    public List<Double> getValues()
    {
        return values;
    }
}
