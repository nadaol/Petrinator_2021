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

package org.petrinator.editor.actions.algorithms;

import org.petrinator.editor.Root;
import org.petrinator.editor.filechooser.*;
import org.petrinator.petrinet.Marking;
import org.petrinator.util.GraphicsTools;
import pipe.gui.widgets.ButtonBar;
import pipe.gui.widgets.EscapableDialog;
import pipe.gui.widgets.ResultsHTMLPane;
import pipe.utilities.math.Matrix;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author Joaquin Felici <joaquinfelici at gmail.com>
 * @brief
 */
public class InvariantAction extends AbstractAction
{
    private Root root;
    //private PetriNetView _pnmlData; // A reference to the Petri Net to be analysed
    private Matrix _incidenceMatrix;
    private Matrix _PInvariants;
    private ResultsHTMLPane results;

    public InvariantAction(Root root)
    {
        this.root = root;
        String name = "Invariant analysis";
        putValue(NAME, name);
        putValue(SHORT_DESCRIPTION, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/invariant16.png"));
    }

    public void actionPerformed(ActionEvent e)
    {
        /*
         * Create tmp.pnml file
         */
        FileChooserDialog chooser = new FileChooserDialog();

        if (root.getCurrentFile() != null) {
            chooser.setSelectedFile(root.getCurrentFile());
        }

        chooser.addChoosableFileFilter(new PipePnmlFileType());
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setCurrentDirectory(root.getCurrentDirectory());
        chooser.setDialogTitle("Save as...");

        /*
         * Show initial pane
         */
        EscapableDialog guiDialog = new EscapableDialog(root.getParentFrame(), "Invariant analysis", true);
        Container contentPane = guiDialog.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        //sourceFilePanel = new PetriNetChooserPanel("Source net", null);
        results = new ResultsHTMLPane("");
        contentPane.add(results);
        contentPane.add(new ButtonBar("Analyse", analyseButtonClick, guiDialog.getRootPane()));
        guiDialog.pack();
        guiDialog.setLocationRelativeTo(root.getParentFrame());
        guiDialog.setVisible(true);
    }

    private final ActionListener analyseButtonClick = new ActionListener()
    {

        public void actionPerformed(ActionEvent arg0)
        {
            //PetriNetView sourceDataLayer = new PetriNetView("tmp/tmp.pnml");
            _incidenceMatrix = new Matrix(root.getDocument().getPetriNet().getIncidenceMatrix());
            String s = "<h2>Petri Net Invariant Analysis</h2>";

            if(!root.getDocument().getPetriNet().getRootSubnet().hasPlaces() || !root.getDocument().getPetriNet().getRootSubnet().hasTransitions())
            {
                s += "Invalid net!";
            }
            else
            {
                try
                {

                    //PNMLWriter.saveTemporaryFile(sourceDataLayer,this.getClass().getName());

                    s += analyse();
                    results.setEnabled(true);
                }
                catch(OutOfMemoryError oome)
                {
                    System.gc();
                    results.setText("");
                    s = "Memory error: " + oome.getMessage();

                    s += "<br>Not enough memory. Please use a larger heap size." + "<br>" + "<br>Note:" + "<br>The Java heap size can be specified with the -Xmx option." + "<br>E.g., to use 512MB as heap size, the command line looks like this:" + "<br>java -Xmx512m -classpath ...\n";
                    results.setText(s);
                    return;
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    s = "<br>Error" + e.getMessage();
                    results.setText(s);
                    return;
                }
            }
            results.setText(s);
        }
    };

    /**
     * Invariant analysis was originally only performed on the initial markup
     * matrix. This is not what the user expects however as when changes are made
     * to the petri net, the invariant analysis then does not change to reflect
     * this. The method calls have been changed to pass the current markup matrix
     * as the parameter for invariant analysis.
     *
     * @author Nadeem Akharware
     * @return html information to write to panel
     */
    private String analyse()
    {
        Date start_time = new Date(); // start timer for program execution
        // extract data from PN object
        int[] currentMarking = root.getDocument().getPetriNet().getInitialMarking().getMarkingAsArray()[Marking.CURRENT].clone();

        String output = findNetInvariants(currentMarking); // Nadeem 26/05/2005

        Date stop_time = new Date();
        double etime = (stop_time.getTime() - start_time.getTime()) / 1000.;
        return output + "<br>Analysis time: " + etime + "s";
    }

    /**
     * Find the net invariants.
     *
     * @param M An array containing the current marking of the net.
     * @return A string containing the resulting matrices of P and T
     *         Invariants or "None" in place of one of the matrices if it does
     *         not exist.
     */
    private String findNetInvariants(int[] M)
    {
        return reportTInvariants(M) + "<br>" + reportPInvariants(M) + "<br>";
    }

    /**
     * Reports on the P invariants.
     *
     * @param M An array containing the current marking of the net.
     * @return A string containing the resulting matrix of P Invariants,
     *         the P equations and some analysis
     */
    private String reportPInvariants(int[] M)
    {
        _PInvariants = findVectors(_incidenceMatrix.transpose());
        String result = "<h3>P-Invariants</h3>";
        result += makeTable(
                _PInvariants, root.getDocument().getPetriNet().getSortedPlacesNames(), false, true, true, false);

        if(_PInvariants.isCovered())
        {
            result += "The net is covered by positive P-Invariants, " +
                    "therefore it is bounded.";
        }
        else
        {
            result += "The net is not covered by positive P-Invariants, " +
                    "therefore we do not know if it is bounded.";
        }
        return result + "<br>" + findPEquations(M);
    }

    /**
     * Reports on the T invariants.
     *
     * @param M An array containing the current marking of the net.
     * @return A string containing the resulting matrix of T Invariants and
     *         some analysis of it
     */
    private String reportTInvariants(int[] M)
    {
        Matrix TInvariants = findVectors(_incidenceMatrix);

        String result = "<h3>T-Invariants</h3>";
        result += makeTable(
                TInvariants, root.getDocument().getPetriNet().getSortedTransitionsNames(), false, true, true, false);

        if(TInvariants.isCovered())
        {
            result += "The net is covered by positive T-Invariants, " +
                    "therefore it might be bounded and live.";
        }
        else
        {
            result += "The net is not covered by positive T-Invariants, " +
                    "therefore we do not know if it is bounded and live.";
        }

        return result + "<br>";
    }

    //<Marc>
    /* It returns a PNMatrix containing the place invariants of the sourceDataLayer net.
     * @param sourceDataLayer A _dataLayer type object with all the information about
     *                        the petri net.
     * @return a PNMatrix where each column contains a place invariant.
     */
    /*
    public Matrix getPInvariants(PetriNetView sourceDataLayer)
    {
        int[][] array = sourceDataLayer.getActiveTokenView()
                .getIncidenceMatrix(sourceDataLayer.getArcsArrayList(), sourceDataLayer.getTransitionsArrayList(),
                        sourceDataLayer.getPlacesArrayList());
        if(array.length == 0)
        {
            return null;
        }
        _incidenceMatrix = new Matrix(array);


        LinkedList<MarkingView>[] markings = sourceDataLayer.getCurrentMarkingVector();
        int[] currentMarking = new int[markings.length];
        for(int i = 0; i < markings.length; i++)
        {
            currentMarking[i] = markings[i].getFirst().getCurrentMarking();
        }

        return findVectors(_incidenceMatrix.transpose());
    }*/

    /* It returns a PNMatrix containing the transition invariants of the sourceDataLayer net.
     * @param sourceDataLayer A _dataLayer type object with all the information about
     *                        the petri net.
     * @return a PNMatrix where each column contains a transition invariant.
     */
    /*
    public Matrix getTInvariants(PetriNetView sourceDataLayer)
    {
        int[][] array = sourceDataLayer.getActiveTokenView().getIncidenceMatrix(
                sourceDataLayer.getArcsArrayList(), sourceDataLayer.getTransitionsArrayList(),
                sourceDataLayer.getPlacesArrayList());
        if(array.length == 0)
        {
            return null;
        }
        _incidenceMatrix = new Matrix(array);

        LinkedList<MarkingView>[] markings = sourceDataLayer.getCurrentMarkingVector();
        int[] currentMarking = new int[markings.length];
        for(int i = 0; i < markings.length; i++)
        {
            currentMarking[i] = markings[i].getFirst().getCurrentMarking();
        }

        return findVectors(_incidenceMatrix);
    }*/
    //</Marc>

    /**
     * Find the P equations of the net.
     *
     * @param currentMarking An array containing the current marking of the net.
     * @return A string containing the resulting P equations,
     *         empty string if the equations do not exist.
     */
    private String findPEquations(int[] currentMarking)
    {
        ArrayList<String> placeNames = root.getDocument().getPetriNet().getSortedPlacesNames();
        String eq = "<h3>P-Invariant equations</h3>";
        int m = _PInvariants.getRowDimension();
        int n = _PInvariants.getColumnDimension();
        if(n < 1)
        { // if there are no P-invariants don't return any equations
            return "";
        }

        Matrix col = new Matrix(m, 1);
        int rhs = 0; // the right hand side of the equations

        // form the column vector of current marking
        Matrix M = new Matrix(currentMarking, currentMarking.length);
        // for each column c in PInvariants, form one equation of the form
        // a1*p1+a2*p2+...+am*pm = c.transpose*M where a1, a2, .., am are the
        // elements of c
        for(int i = 0; i < n; i++)
        { // column index
            for(int j = 0; j < m; j++)
            { // row index
                // form left hand side:
                int a = _PInvariants.get(j, i);
                if(a > 1)
                {
                    eq += Integer.toString(a);
                }
                if(a > 0)
                {
                    eq += "M(" + placeNames.get(j) + ") + "; // Nadeem 28/05/2005
                }
            }
            // replace the last occurance of "+ "
            eq = eq.substring(0, (eq.length() - 2)) + "= ";

            // form right hand side
            col = _PInvariants.getMatrix(0, m - 1, i, i);
            rhs = col.transpose().vectorTimes(M);
            eq += rhs + "<br>"; // and separate the equations
        }
        return eq;
    }

    /**
     * Transform a matrix to obtain the minimal generating set of vectors.
     *
     * @param c The matrix to transform.
     * @return A matrix containing the vectors.
     */
    private Matrix findVectors(Matrix c)
    {
        /*
       | Tests Invariant Analysis IModule
       |
       |   C          = incidence matrix.
       |   B          = identity matrix with same number of columns as C.
       |                Becomes the matrix of vectors in the end.
       |   pPlus      = integer array of +ve indices of a row.
       |   pMinus     = integer array of -ve indices of a row.
       |   pPlusMinus = set union of the above integer arrays.
        */
        int m = c.getRowDimension(), n = c.getColumnDimension();

        // generate the nxn identity matrix
        Matrix B = Matrix.identity(n, n);

        // arrays containing the indices of +ve and -ve elements in a row vector
        // respectively
        int[] pPlus, pMinus;

        // while there are no zero elements in C do the steps of phase 1
//--------------------------------------------------------------------------------------
        // PHASE 1:
//--------------------------------------------------------------------------------------
        while(!(c.isZeroMatrix()))
        {
            if(c.checkCase11())
            {
                // check each row (case 1.1)
                for(int i = 0; i < m; i++)
                {
                    pPlus = c.getPositiveIndices(i); // get +ve indices of ith row
                    pMinus = c.getNegativeIndices(i); // get -ve indices of ith row
                    if(isEmptySet(pPlus) || isEmptySet(pMinus))
                    { // case-action 1.1.a
                        // this has to be done for all elements in the union pPlus U pMinus
                        // so first construct the union
                        int[] pPlusMinus = uniteSets(pPlus, pMinus);

                        // eliminate each column corresponding to nonzero elements in pPlusMinus union
                        for(int j = pPlusMinus.length - 1; j >= 0; j--)
                        {
                            if(pPlusMinus[j] != 0)
                            {
                                c = c.eliminateCol(pPlusMinus[j] - 1);
                                B = B.eliminateCol(pPlusMinus[j] - 1);
                                n--;  // reduce the number of columns since new matrix is smaller
                            }
                        }
                    }
                    resetArray(pPlus);   // reset pPlus and pMinus to 0
                    resetArray(pMinus);
                }
            }
            else if(c.cardinalityCondition() >= 0)
            {
                while(c.cardinalityCondition() >= 0)
                {
                    // while there is a row in the C matrix that satisfies the cardinality condition
                    // do a linear combination of the appropriate columns and eliminate the appropriate column.
                    int cardRow = -1; // the row index where cardinality == 1
                    cardRow = c.cardinalityCondition();
                    // get the column index of the column to be eliminated
                    int k = c.cardinalityOne();
                    if(k == -1)
                    {
                        System.out.println("Error");
                    }

                    // get the comlumn indices to be changed by linear combination
                    int j[] = c.colsToUpdate();

                    // update columns with linear combinations in matrices C and B
                    // first retrieve the coefficients
                    int[] jCoef = new int[n];
                    for(int i = 0; i < j.length; i++)
                    {
                        if(j[i] != 0)
                        {
                            jCoef[i] = Math.abs(c.get(cardRow, (j[i] - 1)));
                        }
                    }

                    // do the linear combination for C and B
                    // k is the column to add, j is the array of cols to add to
                    c.linearlyCombine(k, Math.abs(c.get(cardRow, k)), j, jCoef);
                    B.linearlyCombine(k, Math.abs(c.get(cardRow, k)), j, jCoef);

                    // eliminate column of cardinality == 1 in matrices C and B
                    c = c.eliminateCol(k);
                    B = B.eliminateCol(k);
                    // reduce the number of columns since new matrix is smaller
                    n--;
                }
            }
            else
            {
                // row annihilations (condition 1.1.b.2)
                // operate only on non-zero rows of C (row index h)
                // find index of first non-zero row of C (int h)
                int h = c.firstNonZeroRowIndex();
                while((h = c.firstNonZeroRowIndex()) > -1)
                {

                    // the column index of the first non zero element of row h
                    int k = c.firstNonZeroElementIndex(h);

                    // find first non-zero element at column k, chk
                    int chk = c.get(h, k);

                    // find all the other indices of non-zero elements in that row chj[]
                    int[] chj = new int[n - 1];
                    chj = c.findRemainingNZIndices(h);

                    while(!(isEmptySet(chj)))
                    {
                        // chj empty only when there is just one nonzero element in the
                        // whole row, this should not happen as this case is eliminated
                        // in the first step, so we would never arrive at this while()
                        // with just one nonzero element

                        // find all the corresponding elements in that row (coefficients jCoef[])
                        int[] jCoef = c.findRemainingNZCoef(h);

                        // adjust linear combination coefficients according to sign
                        int[] alpha, beta; // adjusted coefficients for kth and remaining columns respectively
                        alpha = alphaCoef(chk, jCoef);
                        beta = betaCoef(chk, jCoef.length);

                        // linearly combine kth column, coefficient alpha, to jth columns, coefficients beta
                        c.linearlyCombine(k, alpha, chj, beta);
                        B.linearlyCombine(k, alpha, chj, beta);

                        // delete kth column
                        c = c.eliminateCol(k);
                        B = B.eliminateCol(k);

                        chj = c.findRemainingNZIndices(h);
                    }
                }
                // show the result
                // System.out.println("Pseudodiagonal positive basis of Ker C after phase 1:");
                // B.print(2, 0);
            }
        }
        // System.out.println("end of phase one");
        // END OF PHASE ONE, now B contains a pseudodiagonal positive basis of Ker C
//--------------------------------------------------------------------------------------
        // PHASE 2:
//--------------------------------------------------------------------------------------
        // h is -1 at this point, make it equal to the row index that has a -ve element.
        // rowWithNegativeElement with return -1 if there is no such row, and we exit the loop.
        int h;
        while((h = B.rowWithNegativeElement()) > -1)
        {

            pPlus = B.getPositiveIndices(h); // get +ve indices of hth row (1st col = index 1)
            pMinus = B.getNegativeIndices(h); // get -ve indices of hth row (1st col = index 1)

            // effective length is the number of non-zero elements
            int pPlusLength = effectiveSetLength(pPlus);
            int pMinusLength = effectiveSetLength(pMinus);

            if(pPlusLength != 0)
            { // set of positive coef. indices must me non-empty
                // form the cross product of pPlus and pMinus
                // for each pair (j, k) in the cross product, operate a linear combination on the columns
                // of indices j, k, in order to get a new col with a zero at the hth element
                // The number of new cols obtained = the number of pairs (j, k)
                for(int j = 0; j < pPlusLength; j++)
                {
                    for(int k = 0; k < pMinusLength; k++)
                    {
                        // coefficients of row h, cols indexed j, k in pPlus, pMinus
                        // respectively
                        int jC = pPlus[j] - 1, kC = pMinus[k] - 1;

                        // find coeficients for linear combination, just the abs values
                        // of elements this is more efficient than finding the least
                        // common multiple and it does not matter since later we will
                        // find gcd of col and we will normalise with that the col
                        // elements
                        int a = -B.get(h, kC), b = B.get(h, jC);

                        // create the linear combination a*jC-column + b*kC-column, an
                        // IntMatrix mx1 where m = number of rows of B
                        m = B.getRowDimension();
                        Matrix v1 = new Matrix(m, 1); // column vector mx1 of zeros
                        Matrix v2 = new Matrix(m, 1); // column vector mx1 of zeros
                        v1 = B.getMatrix(0, m - 1, jC, jC);
                        v2 = B.getMatrix(0, m - 1, kC, kC);
                        v1.timesEquals(a);
                        v2.timesEquals(b);
                        v2.plusEquals(v1);

                        // find the gcd of the elements in this new col
                        int V2gcd = v2.gcd();

                        // divide all the col elements by their gcd if gcd > 1
                        if(V2gcd > 1)
                        {
                            v2.divideEquals(V2gcd);
                        }

                        // append the new col to B
                        n = B.getColumnDimension();
                        Matrix f = new Matrix(m, n + 1);
                        f = B.appendVector(v2);
                        B = f.copy();
                    }
                } // endfor (j,k) operations

                // delete from B all cols with index in pMinus
                for(int ww = 0; ww < pMinusLength; ww++)
                {
                    B = B.eliminateCol(pMinus[ww] - 1);
                }

            } // endif
        } // end while
        // System.out.println("\nAfter column transformations in phase 2 (non-minimal generating set) B:");
        // B.print(2, 0);

        // delete from B all cols having non minimal support
        // k is the index of column to be eliminated, if it is -1 then there is
        // no col to be eliminated
        int k = 0;
        // form a matrix with columns the row indices of non-zero elements
        Matrix bi = B.nonZeroIndices();

        while(k > -1)
        {
            k = bi.findNonMinimal();

            if(k != -1)
            {
                B = B.eliminateCol(k);
                bi = B.nonZeroIndices();
            }
        }

        // display the result
        // System.out.println("Minimal generating set (after phase 2):");
        // B.print(2, 0);
        return B;
    }

    /**
     * find the number of non-zero elements in a set
     *
     * @param pSet The set count the number of non-zero elements.
     * @return The number of non-zero elements.
     */
    private int effectiveSetLength(int[] pSet)
    {
        int effectiveLength = 0; // number of non-zero elements
        //int setLength = pSet.length;

        for (int value : pSet) {
            if (value != 0) {
                effectiveLength++;
            } else {
                return effectiveLength;
            }
        }
        return effectiveLength;
    }

    /**
     * adjust linear combination coefficients according to sign
     * if sign(j) <> sign(k) then alpha = abs(j) beta = abs(k)
     * if sign(j) == sign(k) then alpha = -abs(j) beta = abs(k)
     *
     * @param k The column index of the first coefficient
     * @param j The column indices of the remaining coefficients
     * @return The adjusted alpha coefficients
     */
    private int[] alphaCoef(int k, int[] j)
    {
        int n = j.length; // the length of one row
        int[] alpha = new int[n];

        for(int i = 0; i < n; i++)
        {
            if((k * j[i]) < 0)
            {
                alpha[i] = Math.abs(j[i]);
            }
            else
            {
                alpha[i] = -Math.abs(j[i]);
            }
        }
        return alpha;
    }

    /**
     * adjust linear combination coefficients according to sign
     * if sign(j) <> sign(k) then alpha = abs(j) beta = abs(k)
     * if sign(j) == sign(k) then alpha = -abs(j) beta = abs(k)
     *
     * @param chk The first coefficient
     * @param n   The length of one row
     * @return The adjusted beta coefficients
     */
    private int[] betaCoef(int chk, int n)
    {
        int[] beta = new int[n];
        int abschk = Math.abs(chk);

        for(int i = 0; i < n; i++)
        {
            beta[i] = abschk;
        }
        return beta;
    }

    private void resetArray(int[] a)
    {
        for(int i = 0; i < a.length; i++)
            a[i] = 0;
    }

    /**
     * Unite two sets (arrays of integers) so that if there is a common entry in
     * the arrays it appears only once, and all the entries of each array appear
     * in the union. The resulting array size is the same as the 2 arrays and
     * they are both equal. We are only interested in non-zero elements. One of
     * the 2 input arrays is always full of zeros.
     *
     * @param A The first set to unite.
     * @param B The second set to unite.
     * @return The union of the two input sets.
     */
    private int[] uniteSets(int[] A, int[] B)
    {
        int[] union = new int[A.length];

        if(isEmptySet(A))
        {
            union = B;
        }
        else
        {
            union = A;
        }
        return union;
    }

    /**
     * check if an array is empty (only zeros)
     *
     * @param pSet The set to check if it is empty.
     * @return True if the set is empty.
     */
    private boolean isEmptySet(int[] pSet)
    {
        int setLength = pSet.length;

        for(int i = 0; i < setLength; i++)
        {
            if(pSet[i] != 0)
            {
                return false;
            }
        }
        return true;
    }

    //TODO view if we should update the ResultsHTMLPane class or keep here
    private static String makeTable(Matrix matrix, ArrayList<String> name, boolean showLines, boolean doShading, boolean columnHeaders, boolean rowHeaders) {
        int cols = name.size();
        int[] k = matrix.getColumnPackedCopy();
        StringBuilder s = new StringBuilder();
        s.append("<table border=").append(showLines ? 1 : 0).append(" cellspacing=2>");
        s.append("<tr").append(doShading ? " class= odd>" : ">");

        int j;
        for(j = 0; j < cols; ++j) {
            if (j == 0 && rowHeaders) {
                s.append("<td class=empty> </td>");
            }

            s.append("<td class=").append(columnHeaders ? "colhead>" : "cell>").append(name.get(j)).append("</td>");
        }

        s.append("</tr>");
        j = 0;

        for(int i = 0; i < k.length; ++i) {
            if (j == 0) {
                s.append("<tr").append(doShading ? " class=" + (i / cols % 2 == 1 ? "odd>" : "even>") : ">");
            }

            if (j == 0 && rowHeaders) {
                s.append("<td class=empty></td>");
            }

            s.append("<td class=cell>").append(k[i]).append("</td>");
            ++j;
            if (j == cols) {
                s.append("</tr>");
                j = 0;
            }
        }

        s.append("</table>");
        return s.toString();
    }
}
