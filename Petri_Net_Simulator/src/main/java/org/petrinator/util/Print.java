package org.petrinator.util;

import java.util.ArrayList;
import java.util.Map;

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

}
