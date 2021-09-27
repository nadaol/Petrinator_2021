package org.petrinator.util;

import pipe.views.PetriNetView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

public class Print {
    public static void print_matrix(int[][] matrix,String Title)
    {
        System.out.println(Title);
        for (int f=0; f < matrix.length; f++)
        {
            for (int c=0; c < matrix[f].length; c++)
            {
                System.out.print(Integer.toString(matrix[f][c])+" ");
            }
            System.out.println("\n");
        }
    }

    public static void print_arraylist_arraylist_String(ArrayList<ArrayList<String>> ars,String Title)
    {
        System.out.println(Title);
        for(ArrayList<String> ar : ars)
        {
            Print.print_arraylist_string(ar, String.format("Conflicto") );
        }
    }

    public static void print_arraylist_arraylist_int(ArrayList<ArrayList<Integer>> ars,String Title)
    {
        System.out.println(Title);
        int cont=1;
        for(ArrayList<Integer> ar : ars)
        {
            Print.print_arraylist_int(ar,"Grupo " +cont);
            cont++;
        }
    }
    public static void print_arraylist_string(ArrayList<String> list,String Title)
    {
        System.out.println(Title);
        for(String string_array : list)
        {
                System.out.print(string_array+" ");
        }
        System.out.println("\n");
    }


    public static void print_arraylist_int(ArrayList<Integer> list,String Title)
    {
        System.out.println(Title);
        for(int list_element : list)
        {
            System.out.print(list_element+" ");
        }
        System.out.println("\n");
    }

    public static void print_arraylist_int_array(ArrayList<Integer[]> list,String Title)
    {
        System.out.println(Title);
        for(Integer[] list_element : list)
        {
            for (int j=0; j<list_element.length; j++)
            {
                System.out.print(list_element[j]+" ");
            }
            System.out.println("\n");
        }
    }

    public static void print_int_array(Integer[] list,String Title)
    {
        System.out.println(Title);

        for (int j=0; j<list.length; j++)
        {
            System.out.print(list[j]+" ");
        }
        System.out.println("\n");
    }

    public static void print_hashmap(Map<String,ArrayList<Integer>> hashmap,String Title)
    {
        System.out.println(Title);
        //iterate over the linked hashmap
        for (Map.Entry<String, ArrayList<Integer>> entry : hashmap.entrySet())
        {
            //Print key
            System.out.print(entry.getKey()+" : ");
            //Print value (arraylist)
            for(int list_element : entry.getValue()){
                System.out.print(list_element+" ");
            }
            System.out.println();
        }
        System.out.println();
    }
    public static String toString(Vector<boolean[]> vector, PetriNetView pnmlData) {
        String s = "";
        if (vector.size() == 0) {
            return "None found.<br>";
        } else {
            for(Iterator var5 = vector.iterator(); var5.hasNext(); s = s + "}<br>") {
                boolean[] element = (boolean[])var5.next();
                s = s + "{";

                for(int i = 0; i < element.length; ++i) {
                    s = s + (element[i] ? pnmlData.getPlace(i).getName() + ", " : "");
                }

                StringBuilder b = new StringBuilder(s);
                if (s.contains(",")) {
                    b.replace(s.lastIndexOf(","), s.lastIndexOf(",") + 1, "");
                    s = b.toString();
                }
            }

            return s;
        }
    }

}
