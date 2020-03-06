package com.parkking491prototype.parkking;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ParkingStatus {
    private Map<String, StatusDot> statusDots;
    private int numOfOpenSpots;
    private boolean canvasNormalizedFlag;
    public ParkingStatus(){
        statusDots=  new HashMap<String, StatusDot>();
        canvasNormalizedFlag = false;
    }

//    private void setTestDotCoords(){
//        for(int i = 1; i<8; i++){
//            StatusDot sd;
//            if (i<6) {
//                sd = new StatusDot(160, 60 + i * 50 );
//                statusDotList.add(sd);
//            }else if (i>=6 && i<11){
//                sd = new StatusDot(240, 60 + (i-5) * 50 );
//                statusDotList.add(sd);
//            }else if (i>=11 && i<16){
//                sd = new StatusDot(650, 100 + (i-10) * 80 );
//                statusDotList.add(sd);
//            }else{
//                sd = new StatusDot(780, 100 + (i-15) * 80 );
//                statusDotList.add(sd);
//            }
//            int min = 0;
//            int max = 1;
////            sd.setStatus((min + (int)(Math.random() * ((max - min) + 1)))==1);
//            sd.setStatus(false);
//        }
//
//    }


    public void setDots(JSONArray coordArray){
        for(int i=0; i<coordArray.length(); i++){
            try{
                //get coord object
                JSONObject coordObject = coordArray.getJSONObject(i);

                //get point
                JSONArray pointArray =  coordObject.getJSONArray("point");
                String id = coordObject.getString("id");
                if(!id.equals("None")) {
                    statusDots.put(id , new StatusDot(pointArray.getInt(0), pointArray.getInt(1)));
                }
            }catch(JSONException e){
                e.printStackTrace();
            }
        }
    }


    public Map<String, StatusDot> getStatusDotList() {
        return statusDots;
    }


//      //TEST FUNCTION
//    public void updateStatus(){
//        numOfOpenSpots = 0;
//        for(int i = 0; i<statusDotList.size(); i++){
//            statusDotList.get(i).setStatus((0 + (int)(Math.random() * ((1 - 0) + 1)))==1);
//            if (statusDotList.get(i).getStatus()==true){
//                numOfOpenSpots++;
//            }
//        }
//
//    }

    public void updateStatus(JSONArray statusArray){
        numOfOpenSpots = 0;

        try {
            for (int i = 0; i < statusArray.length(); i++) {
                JSONObject statusObject = statusArray.getJSONObject(i);
                Double confidence = statusObject.getDouble("confidence");
                String id = statusObject.getString("id");
                if(statusDots.containsKey(id)) {
                    statusDots.get(id).setStatus(confidence != 0);
                }
                if (confidence!=0) {
                    numOfOpenSpots++;
                }
            }
        } catch (
                JSONException e) {
            e.printStackTrace();
        }


    }

    public int getNumOfOpenSpots(){
        return numOfOpenSpots;
    }


    public void setCanvasNormalizedFlag(boolean canvasNormalizedFlag){
        this.canvasNormalizedFlag = canvasNormalizedFlag;
    }

    public boolean isCanvasNormalizedFlag(){
        return canvasNormalizedFlag;
    }
}