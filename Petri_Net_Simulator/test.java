import java.io.*;
import java.net.*;
import java.lang.*;
import  java.util.Scanner;

public class test
{
    public static void main(String args[]) throws IOException
    {
        File f = new File("program.txt");
        String pathNet = f.getAbsolutePath();
        System.out.println(pathNet);
    }

}
