package org.unc.lac.javapetriconcurrencymonitor.petrinets;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.unc.lac.javapetriconcurrencymonitor.exceptions.NotInitializedPetriNetException;
import org.unc.lac.javapetriconcurrencymonitor.exceptions.PetriNetException;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MArc;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MPlace;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MTransition;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class CudaPetriNet extends RootPetriNet {

    private String serverIP;

    /**
     * Makes a PetriNet Object. This is intended to be used by PetriNetFactory
     *
     * @param _places           Array of Place objects (dimension p)
     * @param _transitions      Array of Transition objects (dimension t)
     * @param _arcs             Array of Arcs
     * @param _initialMarking   Array of Integers (tokens in each place) (dimension p)
     * @param _preI             Pre-Incidence matrix (dimension p*t)
     * @param _posI             Post-Incidence matrix (dimension p*t)
     * @param _I                Incidence matrix (dimension p*t)
     * @param _inhibitionMatrix Pre-Incidence matrix for inhibition arcs only. If no inhibition arcs, null is accepted.
     * @param _resetMatrix      Pre-Incidence matrix for reset arcs only. If no reset arcs, null is accepted.
     * @param _readerMatrix     Pre-Incidence matrix for reader arcs only. If no reader arcs, null is accepted.
     */
    public CudaPetriNet(MPlace[] _places, MTransition[] _transitions, MArc[] _arcs, Integer[] _initialMarking, Integer[][] _preI, Integer[][] _posI, Integer[][] _I, Boolean[][] _inhibitionMatrix, Boolean[][] _resetMatrix, Integer[][] _readerMatrix) {
        super(_places, _transitions, _arcs, _initialMarking, _preI, _posI, _I, _inhibitionMatrix, _resetMatrix, _readerMatrix);
    }

    @Override
    protected boolean[] computeEnabledTransitions() {
        return enabledTransitions;
    }

    @Override
    public synchronized PetriNetFireOutcome fire(final MTransition transition) throws IllegalArgumentException, NotInitializedPetriNetException, PetriNetException {

        if(transition == null){
            throw new IllegalArgumentException("Null Transition passed as argument");
        }
        if(!initializedPetriNet){
            throw new NotInitializedPetriNetException();
        }

        int transitionIndex = transition.getIndex();

        if(transitionIndex < 0 || transitionIndex > transitions.length){
            throw new IllegalArgumentException("Index " + transitionIndex + " doesn't match any transition's index in this petri net");
        }

        if(!enabledTransitions[transitionIndex]){
            return PetriNetFireOutcome.NOT_ENABLED;
        }

        sendToServer("fire", Integer.toString(transitionIndex));

        return PetriNetFireOutcome.SUCCESS;
    }

    /**
     * Structure to parse matrices into JSON
     */
    public class JsonMatrices{
        String matrix;
        Integer[][] values;
        int rows;
        int columns;

        JsonMatrices(String matrix, Integer[][] values){

            this.matrix = matrix;
            this.rows = values.length;
            this.columns = values[0].length;
            this.values = values;

        }
    }

    /**
     * Structure to parse server's JSON response
     */
    public class JsonResponse{
        String status;
        boolean[] vector;
    }

    /**
     * Method used to convert matrices to a json format, needed to send via http
     * @return string containing all matrices used in json format
     */
    private String matricesToJSON(){

        Gson gson = new Gson();

        Integer [][] inh = booltoInt(inhibitionMatrix);
        Integer [][] res = booltoInt(resetMatrix);
        Integer [][] mark = new Integer[1][];
        mark[0] = currentMarking;

        ArrayList<JsonMatrices> matrices = new ArrayList<>();

        matrices.add(new JsonMatrices("Incidence", inc));
        matrices.add(new JsonMatrices("Inhibition", inh));
        matrices.add(new JsonMatrices("Reader", readerMatrix));
        matrices.add(new JsonMatrices("Reset", res));
        matrices.add(new JsonMatrices("Marking", mark));

        return gson.toJson(matrices);
    }

    /**
     * Converts a boolean matrix into an integer matrix containing 1s or 0s
     * @param mat matrix to be converted
     * @return integer matrix calculated
     */
    private Integer[][] booltoInt(Boolean[][] mat){

        Integer [][] mat_int = new Integer[mat.length][mat[0].length];

        for(int i = 0; i<mat.length; i++){
            for(int j = 0; j<mat[0].length; j++){
                if(mat[i][j]){
                    mat_int[i][j] = 1;
                }
                else{
                    mat_int[i][j] = 0;
                }
            }
        }

        return mat_int;

    }

    /**
     * Sends a string to a remote server
     * @param path URL's path
     * @param data string to send to the server
     * @return true if response's status was SUCCESSFUL; false otherwise
     */
    private boolean sendToServer(String path, String data){

        try{

            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost= new HttpPost(serverIP + "/" + path);

            StringEntity ent = new StringEntity(data);

            httppost.setEntity(ent);
            httppost.setHeader("Accept","application/json");
            httppost.setHeader("Content-type","application/json");

            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                try (InputStream instream = entity.getContent()) {

                    StringWriter writer = new StringWriter();
                    IOUtils.copy(instream, writer, "UTF-8");

                    Gson gson = new Gson();

                    JsonResponse svResponse = gson.fromJson(writer.toString(), JsonResponse.class);

                    if(!svResponse.status.equals("SUCCESS")){
                        JOptionPane.showMessageDialog(null, "Internal server Error");
                        return false;
                    }

                    enabledTransitions = svResponse.vector.clone();

                }
            }
            else{
                return false;
            }

        }
        catch (UnsupportedEncodingException e) {
            JOptionPane.showMessageDialog(null, "Error while sending data");
            return false;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Connection to server failed");
            return false;
        }
        return true;

    }

    private void setServerIP(String serverIP){
        System.out.println(serverIP);
        this.serverIP = serverIP;
    }

    public boolean initializeCuda(String serverIP){
        setServerIP(serverIP);
        return sendToServer("matrices", matricesToJSON());
    }

}
