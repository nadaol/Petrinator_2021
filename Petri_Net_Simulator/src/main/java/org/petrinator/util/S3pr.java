package org.petrinator.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.petrinator.editor.Root;
import org.petrinator.editor.actions.algorithms.InvariantAction;
import org.petrinator.petrinet.Marking;
import org.petrinator.petrinet.Transition;

import pipe.utilities.math.Matrix;

public class S3pr {
      //S3PR classification
      public static boolean isS3PR(InvariantAction accion,Root root,String S3PRresults)
      {
          System.out.println("----- Running S3PR Analysis -----\n");
          int[][] IncidenceMatrix = root.getDocument().getPetriNet().getIncidenceMatrix();
          Matrix TInvariants = accion.findVectors(new Matrix(root.getDocument().getPetriNet().getIncidenceMatrix()));
          TInvariants.transpose();
          System.out.println("There are "+ TInvariants.getColumnDimension()+ " T-invariants\n");
  
          //1°Check that there is more than one Tinvariant
          if(!check_num_Tinvariants(TInvariants,S3PRresults))return false;
          System.out.println("1° CHECK: there is more than one T-Invariants");
          //Hashmaps of the transition and places of each Tinvariant
          Map<String,ArrayList<Integer>> Tinvariants_trans = new LinkedHashMap<String,ArrayList<Integer>>();
          Map<String,ArrayList<Integer>> Tinvariants_places = new LinkedHashMap<String,ArrayList<Integer>>();
          Map<String,ArrayList<Integer>> Tinvariants_SM_places = new LinkedHashMap<String,ArrayList<Integer>>();
          Map<String,ArrayList<Integer>> Tinvariants_SM_trans = new LinkedHashMap<String,ArrayList<Integer>>();
          Map<String,ArrayList<Integer>> Tinvariants_resources = new LinkedHashMap<String,ArrayList<Integer>>();
          Map<String,ArrayList<Integer>> Tinvariants_shared_resoruces = new LinkedHashMap<String,ArrayList<Integer>>();
  
          get_tinv_trans_and_places(IncidenceMatrix,TInvariants,Tinvariants_trans, Tinvariants_places);
  
          ArrayList<int[][]> Tinv_incidence_matrices = get_tinvariants_incidences_matrices(IncidenceMatrix, Tinvariants_trans, Tinvariants_places);
  
          Print.print_hashmap(Tinvariants_trans,"T-Invariants Transitions");
          Print.print_hashmap(Tinvariants_places,"T-Invariants Places");
  
          if(!check_closed_Tinvariants(Tinv_incidence_matrices,Tinvariants_trans,Tinvariants_places,Tinvariants_SM_places,Tinvariants_SM_trans,S3PRresults,root))return false;
          System.out.println("2° CHECK: All T-Invariants are closed paths, potential State Machines");
          Print.print_hashmap(Tinvariants_SM_places,"T-Invariants loop Places");
          Print.print_hashmap(Tinvariants_SM_trans,"T-invariant loop Transitions");
  
          if(!check_Tinvariants_SM(Tinv_incidence_matrices,Tinvariants_places,Tinvariants_SM_places,Tinvariants_trans,Tinvariants_SM_trans,Tinvariants_resources,root,S3PRresults))return false;
          System.out.println("3° CHECK: All T-Invariants are State Machines");
          if(!get_shared_places(Tinvariants_resources,Tinvariants_shared_resoruces,S3PRresults))return false;
          System.out.println("4° CHECK: All T-Invariants have Shared Resources");
          Print.print_hashmap(Tinvariants_resources,"T-invariant resources");
          Print.print_hashmap(Tinvariants_shared_resoruces,"T-Invariants Shared Resources");
          System.out.println("5° CHECK: All Resources have marking greater than or equal to one");
          //print_arraylist(getEnabledTransitions(),"Enabled transitions");
          //aca verifica el marcado de las plazas del state machine y las plazas IDLE
          if(!check_SM_places_and_pidle_marking(Tinvariants_SM_places,S3PRresults,root))return false;
          System.out.println("6° CHECK: All State Machine places have marking zero");
          System.out.println("7° CHECK: All Idles Places have marking greater than or equal to one");
          return true;
      }
  
      // ----------  S3PR CLASSIFICATION FUNCTIONS  ----------
  
      private static boolean check_SM_places_and_pidle_marking(Map<String,ArrayList<Integer>> Tinvariants_SM_places,String S3PRresults,Root root)
      {
          int Initial_marking[] = get_initial_marking(root);
          int Tinv_number = 1;
          int numPlaces;
          for (ArrayList<Integer> tinv : Tinvariants_SM_places.values())
          {
              numPlaces=0;
              for (Integer places : tinv)
              {
                  if(Initial_marking[places-1]>0 && !(numPlaces==tinv.size()-1))
                  {
                      System.out.println("The place "+ places + " of de Tinv " + Tinv_number+" have marking and isn´t the idle");
                      S3PRresults="<br>The net isn't S3PR because: The place "+ places + " of de Tinv " + Tinv_number+" is marked but isn´t the idle place";
                      return false;
                  }
                  if( numPlaces==tinv.size()-1 && Initial_marking[places-1]==0)
                  {
                      System.out.println("The place idle "+ places + " of de Tinv " + Tinv_number+" must have marked");
                      S3PRresults="<br>The net isn't S3PR because: The place idle"+ places + " of de Tinv " + Tinv_number+" needs to be marked";
                      return false;
                  }
                  numPlaces++;
              }
              Tinv_number++;
          }
  
          return true;
      }
      private static boolean check_closed_Tinvariants(ArrayList<int[][]> Tinv_incidence_matrices,Map<String,ArrayList<Integer>> Tinvariants_trans,Map<String,ArrayList<Integer>> Tinvariants_places,
          Map<String,ArrayList<Integer>> Tinvariants_SM_place,Map<String,ArrayList<Integer>> Tinvariants_SM_trans,String S3PRresults,Root root)
      {
      System.out.println("----- Running T-Invariants SM Analysis -----\n");
      int cont = 1;
      ArrayList<Integer> Trans_Auxiliar = new ArrayList<Integer>();
      ArrayList<Integer> Places_Auxiliar = new ArrayList<Integer>();
    //Iterate the Tinvariants incidence matrices
      for(int[][] matrices : Tinv_incidence_matrices)
      {
          int[][] Incidence_Auxiliar = new int [matrices.length][matrices[0].length];
          Trans_Auxiliar.clear();
          Places_Auxiliar.clear();
  
          for(int row=0; row<matrices.length;row++)
          {
              for (int column = 0; column < matrices[row].length; column++)
              {
                  Incidence_Auxiliar[row][column] = matrices[row][column];
              }
          }
  

          int t,p,pAnterior;
          t=find_first_Tinvariants_enable_transition(Tinvariants_trans.get(String.format("TInv%d (T)",cont)),root);
          if(t==-1)
          {
              System.out.println("T-Invariant " + cont + " doesn't have an idle place or isn't marked");
              S3PRresults="<br>The net isn't S3PR because: T inv " + cont + " has not idle place or isn't marked";
              return false;
          }
          p=0;
          pAnterior=0;
          System.out.println("---------- Analyzing Tinv "+cont+" ----------");
          while (true) 
          {
              p=0;
              while (Incidence_Auxiliar[p][t]!=1) //Iterates places of a transition
              {
                  p++;
                  if(p==Incidence_Auxiliar.length)
                  {
                      System.out.println("The T-invariant "+ cont + " dosn't have a closed loop\n");
                      S3PRresults="<br>The net isn't S3PR because: The T Invariant "+cont+ " isn't closed";
                      return false;
                  }
              }
              if(!Trans_Auxiliar.contains(Tinvariants_trans.get(String.format("TInv%d (T)",cont)).get(t)))
              {
                  // Saves the recorred transition
                  Trans_Auxiliar.add(Tinvariants_trans.get(String.format("TInv%d (T)",cont)).get(t));
                  System.out.println("Transition "+Tinvariants_trans.get(String.format("TInv%d (T)",cont)).get(t) +" added to T-invariant "+ cont);
              }
              else
              {
                  if((Trans_Auxiliar.get(0) == Tinvariants_trans.get(String.format("TInv%d (T)",cont)).get(t)) && (Trans_Auxiliar.containsAll(Tinvariants_trans.get(String.format("TInv%d (T)",cont)))))//si el bucle contiene todas las transiciones de t invariante
                  {
                      Tinvariants_SM_place.put(String.format("TInv%d (P-Loop)",(cont)), new ArrayList<Integer>(Places_Auxiliar));
                      Tinvariants_SM_trans.put(String.format("TInv%d (T-Loop)",(cont)), new ArrayList<Integer>(Trans_Auxiliar));
                      System.out.println("The T-invariant "+ cont +" contains a closed loop\n");
                      break;//Condition fullfilled
                  }
                  else
                  {
                      if(!(Trans_Auxiliar.get(0) == Tinvariants_trans.get(String.format("TInv%d (T)",cont)).get(t)))
                          System.out.println("The transition that was repeated was not the first");
                      if(!(Trans_Auxiliar.containsAll(Tinvariants_trans.get(String.format("TInv%d (T)",cont)))))
                          System.out.println("Encountered a loop that doesn't contain all T-invariant transitions");
                    //Deletes the place arcs that forms the loop 
                      delete_place_arcs(Incidence_Auxiliar,pAnterior);
                      t=find_first_Tinvariants_enable_transition(Tinvariants_trans.get(String.format("TInv%d (T)",cont)),root);//para q comience desde la t sencibilizada, sino 0
                      p=0;
                      pAnterior=0;
                      Trans_Auxiliar.clear();
                      Places_Auxiliar.clear();
                      System.out.println("A resource of T-Invariant " + cont + " has been found\n");
                      continue;
                  }
              }
                  t=0;
            //Iterate transitions of the place
              while (Incidence_Auxiliar[p][t]!=-1) 
              {
                  t++;
                  if(t==Incidence_Auxiliar[0].length)
                  {
                      System.out.println("The T-Invariant "+ cont + "has a place that doesn't have an exit arc");
                      S3PRresults="<br>The net isn't S3PR because: The T Invariant "+cont+ " isn't closed";
                      return false;
                  }
              }
              if(!Places_Auxiliar.contains(Tinvariants_places.get(String.format("TInv%d (P)",cont)).get(p)))
              {
                  //Save the recorred place
                  Places_Auxiliar.add(Tinvariants_places.get(String.format("TInv%d (P)",cont)).get(p));
                  System.out.println("Place "+Tinvariants_places.get(String.format("TInv%d (P)",cont)).get(p) +" Added to T-Invariant "+ cont);
              }
              pAnterior=p;
          }
          cont++;
      }
      return true;
  }
  // Checks the resourses of the Tinvariant (oposite of flux), if there is a place that is not a resourse and is not included in the primary loop
  // the Tinvariant is not a SM.
  private static boolean check_Tinvariants_SM(ArrayList<int[][]> Tinv_incidence_matrices,Map<String,ArrayList<Integer>> Tinvariants_places,
  Map<String,ArrayList<Integer>>  Tinvariants_SM_places,Map<String,ArrayList<Integer>>  Tinvariants_trans,Map<String,ArrayList<Integer>>  Tinvariants_SM_trans,Map<String,ArrayList<Integer>> Tinvariants_Shared_resources,Root root,String S3PRresults)
  {
      // Iterate each Tinvariant
      int Ntinv =1;
      int Initial_marking[] = get_initial_marking(root);
  
      for (Map.Entry<String, ArrayList<Integer>> TinvSM_trans : Tinvariants_SM_trans.entrySet())
      {
          ArrayList<Integer> Places_leftOvers = new ArrayList<Integer>();
          // Deletes the places of the Tinvariant and the places with the primary loop
          get_leftOvers(Tinvariants_places.get(String.format("TInv%d (P)",Ntinv)), Tinvariants_SM_places.get(String.format("TInv%d (P-Loop)",(Ntinv))),Places_leftOvers);
          Print.print_arraylist_int(Places_leftOvers, String.format("T-Invariant %d not operational places",Ntinv));
  
        // Checks that the places are oposite to the flux of the primary loop, if not its not a SM.
          if(!check_Tinvariant_resources(Tinv_incidence_matrices.get(Ntinv-1),Tinvariants_places.get(String.format("TInv%d (P)",Ntinv)),Tinvariants_SM_places.get(String.format("TInv%d (P-Loop)",(Ntinv)))
          ,Tinvariants_trans.get(String.format("TInv%d (T)",Ntinv)),Tinvariants_SM_trans.get(String.format("TInv%d (T-Loop)",Ntinv)),Places_leftOvers,Ntinv,Initial_marking,S3PRresults))return false;
          Tinvariants_Shared_resources.put(String.format("TInv%d (R)",Ntinv),Places_leftOvers);
          Ntinv ++ ;
  
      }
      return true;
  }


  private static boolean check_Tinvariant_resources(int[][] Tinv_incidence_matrices,ArrayList<Integer> Tinvariants_places,
  ArrayList<Integer>  Tinvariants_SM_place,ArrayList<Integer> Tinvariants_trans,ArrayList<Integer>  Tinvariants_SM_trans,ArrayList<Integer> Places_leftOvers,int Ntinv,int[] Initial_marking,String S3PRresults)
  {
      int place_row_index;
      for (Integer leftOver_place : Places_leftOvers)
      {
          //Go to row of place'leftOver_place'
          place_row_index = Tinvariants_places.indexOf(leftOver_place);
          //System.out.println(String.format("Plaza leftover %d (indice (%d) ) del T-invariant %d",leftOver_place,place_row_index,Ntinv));
          System.out.println(String.format("The not operational place %d is a resource in T-invariant %d",leftOver_place,Ntinv));
          //get_index(Tinvariants_places,leftOver_place);
          int trans_entrada=0;
          int trans_salida=0;
  
          for(int column=0;column<Tinv_incidence_matrices[0].length;column++)
          {
             if(Tinv_incidence_matrices[place_row_index][column]==1)
             {
               trans_entrada = Tinvariants_trans.get(column);
             }
  
             if(Tinv_incidence_matrices[place_row_index][column]==-1)
             {
               trans_salida = Tinvariants_trans.get(column);
             }
  
          }
  
  
          if(Tinvariants_SM_trans.indexOf(trans_entrada) <= Tinvariants_SM_trans.indexOf(trans_salida))
          {
              System.out.println(String.format("Not operational place %d of T-Invariant %d has an output transition %d and input %d"
                      ,leftOver_place,Ntinv,trans_salida,trans_entrada));
              System.out.println(String.format("The place %d isn't a resource of T-invariant %d",leftOver_place,Ntinv));
              S3PRresults=String.format("<br>The net isn't S3PR because: The place %d isn't a resource of T-invariant %d",leftOver_place,Ntinv);
              return false;
          }
  
          //Check that the place mark is 1
  
          if(Initial_marking[leftOver_place-1] == 0)
          {
              System.out.println(String.format("The place %d of T-invariant %d is a resource but the marking is zero",leftOver_place,Ntinv));
              return false;
          }
  
      }
  
   return true;
  }
  
  //find the index of the first enabled transition of a Tinvariant
  private static int find_first_Tinvariants_enable_transition(ArrayList<Integer> Tinvariant_trans,Root root)
  {
      int index = 0;
      for(Integer trans : Tinvariant_trans)
      {
          if(getEnabledTransitions(root).contains(trans))
          {
              return index;
          }
          else
              index++;
      }
      if(index==Tinvariant_trans.size())
          return -1;
      else
          return index;
  }

  // Verifies that there is more than one Closed Tinvariant, else return false 
  private static boolean check_num_Tinvariants(Matrix TInvariants,String S3PRresults)
  {
      if(TInvariants.getColumnDimension()<=1)
      {
          System.out.println("The net can't be S3PR because there is only one T-Invariant \n");
          S3PRresults="<br>The net isn't S3PR because there is only one T Invariant<br>";
          return false;
      }
      else return true;
  }
  
  // Returns a hashmap of shared places between Tinvariants. (String Tinv -> arraylist of shared places )
  private static boolean get_shared_places(Map<String,ArrayList<Integer>> Tinvariants_resources,Map<String,ArrayList<Integer>> Tinvariants_shared_resources,String S3PRresults)
  {
      int Tinv_number = 1;
      boolean paso =false;
      for (ArrayList<Integer> places : Tinvariants_resources.values())
      {
          if(places.isEmpty())
          {
              System.out.println(String.format("The T-Invariant %d can't be S3PR beacause it doesn't have any resources\n",Tinv_number));
              S3PRresults="<br>The net isn't S3PR because: The T Invariant "+Tinv_number+ "doesn't have any resources";
              return false;
          }
  
          Tinvariants_shared_resources.put(String.format("TInv%d (SR)",(Tinv_number)), new ArrayList<Integer>());
          paso = false;
          for (ArrayList<Integer> places_others : Tinvariants_resources.values())
          {
              if(places.equals(places_others)&&!paso)
              {
                  paso=true;
                  continue;
              }
  
              add_intersection(places,places_others,Tinvariants_shared_resources.get(String.format("TInv%d (SR)",(Tinv_number))));
              if(places.isEmpty())
              {
                  System.out.println(String.format("The T-Invariant %d can't be S3PR because it doesn't share any resources\n",Tinv_number));
                  S3PRresults="<br>The net isn't S3PR because: The T Invariant "+Tinv_number+ "doesn't have any resources";
                  return false;
              }
          }
          Tinv_number ++ ;
      }
      return true;
  }
  
  // Adds the intersection elements between list1 and list2 to list_dest
  private static void add_intersection(ArrayList<Integer> list1,ArrayList<Integer> list2,ArrayList<Integer> list_dest)
  {
      for (Integer element : list1)
          if ( (list2.contains(element)) && (!list_dest.contains(element)))
              list_dest.add(element);
  }
  
  // Get the not shared elements between list1 and list2 to list_dest
  private static void get_leftOvers(ArrayList<Integer> list1_original,ArrayList<Integer> list2,ArrayList<Integer> list_dest)
  {
      list_dest.clear();
      for (Integer element : list1_original)
          if ( !(list2.contains(element)) )
              list_dest.add(element);
  }
  
  // Other S3PR3 associated functions
  
  // Obtains Tinvariants transition numbers (TinvN (T) -> [2,3,4,5]) and Tinvariants places numbers (TinvN (P) -> [1,2,5,7]) including shared and own places .
  private static void get_tinv_trans_and_places(int [][]IncidenceMatrix,Matrix TInvariants,Map<String,ArrayList<Integer>> Tinvariants_trans,Map<String,ArrayList<Integer>> Tinvariants_places)
  {
      //1° Add the tinvariant arraylists to the hashmap
      for(int i=0;i<TInvariants.getColumnDimension();i++)
      {
          Tinvariants_places.put(String.format("TInv%d (P)",(i+1)), new ArrayList<Integer>());
          Tinvariants_trans.put(String.format("TInv%d (T)",(i+1)), new ArrayList<Integer>());
      }
  
       // ----- Get Tinvariant Transitions
      for (int c=0; c < TInvariants.getColumnDimension(); c++)
      {
          for (int f=0; f < TInvariants.getRowDimension(); f++)
          {
              if(TInvariants.get(f,c)==1)
              {
                  Tinvariants_trans.get(String.format("TInv%d (T)",(c+1))).add((Integer)(f+1));
              }
          }
      }
  
      // ----- Get Tinvariant Places
  
      int suma,numarcos,Tinv_number=1;
      //1 Iterate the arraylists of transition per Tinvariant
      for (Map.Entry<String, ArrayList<Integer>> Tinv_trans : Tinvariants_trans.entrySet())
      {
          //2 Iterate the places in the incidence matrix 
          for (int f=0; f < IncidenceMatrix.length; f++)//plazas
          {
              suma=0;
              numarcos=0;
              for(int trans : Tinv_trans.getValue()) //Column iterator
              {
                  //Verify that its a 1 or -1 to get the places of the tinvariant
                  if(IncidenceMatrix[f][trans-1] == 1 || IncidenceMatrix[f][trans-1] == -1)
                      numarcos++;
                  suma += IncidenceMatrix[f][trans-1];
              }
              //Check that it has 2 or more oposite arcs
              if((numarcos>=2) && (suma ==0))
                  Tinvariants_places.get(String.format("TInv%d (P)",(Tinv_number))).add((Integer)(f+1));
          }
          Tinv_number++;
      }
  }
  
  // 
  private static ArrayList<int[][]> get_tinvariants_incidences_matrices(int [][]IncidenceMatrix,Map<String,ArrayList<Integer>> Tinvariants_trans,Map<String,ArrayList<Integer>> Tinvariants_places)
  {
          ArrayList<int[][]> Tinv_incidences = new ArrayList<int[][]>();
          for (int Tinv =0;Tinv<Tinvariants_trans.size();Tinv++)
          {
              // Get places and transitions of Tinvariant i .
              ArrayList<Integer> Tinv_trans = Tinvariants_trans.get(String.format("TInv%d (T)",(Tinv+1)));
              ArrayList<Integer> Tinv_places = Tinvariants_places.get(String.format("TInv%d (P)",(Tinv+1)));
  
              int [][] Tinv_incidence = new int[Tinv_places.size()][Tinv_trans.size()];
  
              for(int place_index=0;place_index<Tinv_places.size();place_index++)
              {
                  int place = Tinv_places.get(place_index);
  
                  for(int trans_index=0;trans_index<Tinv_trans.size();trans_index++)
                  {
                      int transition = Tinv_trans.get(trans_index);
                      Tinv_incidence[place_index][trans_index] = IncidenceMatrix[place-1][transition-1];
                  }
              }
              Tinv_incidences.add(Tinv_incidence);
          }
          return Tinv_incidences;
  
  }
  
  public static int[] get_initial_marking(Root root)
  {
      Marking mark = root.getDocument().getPetriNet().getInitialMarking();
      return mark.getMarkingAsArray()[1];
  }
  
  public static ArrayList<Integer> getEnabledTransitions(Root root)
  {
      ArrayList<Transition> enabledArray = new ArrayList<Transition>(root.getDocument().getPetriNet().getInitialMarking().getAllEnabledTransitions());
      ArrayList<Integer> enabledNames= new ArrayList<Integer>();
  
      for (Transition transition : enabledArray)
      {
          enabledNames.add(Integer.valueOf(transition.getLabel().substring(1)));
      }
      return enabledNames;
  }
  private static void delete_place_arcs(int[][] matrix,Integer row)
  {
       for (int c=0; c < matrix[0].length; c++)
      {
          matrix[row][c]=0;
      }
  
  }
}
