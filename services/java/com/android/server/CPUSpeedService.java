/*
 * Copyright (C) 2011 Terrence Ezrol (for ezGingerbread)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server;

import android.util.Log;
import android.content.ContentResolver;
import android.provider.Settings;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class CPUSpeedService extends Thread {
    private static final String TAG = "CPUFreq";
    private static final String CPU0_FREQ_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/";

    private ContentResolver mContentResolver;

    public CPUSpeedService(ContentResolver cr){
	    mContentResolver=cr;
    }

    private int waitForFBSleep(Boolean sleep){
        /*Wait for the screen to sleep/wake */
        String fn = (sleep ? "wait_for_fb_sleep" : "wait_for_fb_wake");
        try{
            FileInputStream in = null;
            in=new FileInputStream("/sys/power/" + fn);
            int value =  in.read();
            in.close();
            return(value);
        }
        catch(IOException e){
            Log.e(TAG,"Error reading /sys/power/" + fn + ": " + e.toString());
            return -1;
        }
    }

    private int cpuReadInt(String type){
        /* Read an int from a CPU Freq file */
        try{
            FileInputStream in = null;
            String ret="";
            int ch = 0;

            in = new FileInputStream(CPU0_FREQ_PATH + type);
            while(ch != -1){
                ch=in.read();
                if(ch >= 0x30 && ch <= 0x39){
                    ret += (char)ch;
                }
            }
            in.close();
            return(Integer.parseInt(ret));
        }
        catch(IOException e){
            Log.e(TAG,"Error reading " + type + ": " + e.toString());
            return -1;
        }
    }
    private void cpuWriteInt(String type,int value){
        /* Write an int to a CPU Freq file */
        try{
            FileOutputStream out = null;
            String s = ""+value;
            byte[] chrs=s.getBytes();

            out = new FileOutputStream(CPU0_FREQ_PATH + type);
            out.write(chrs,0,chrs.length);
            out.close();
        }
        catch(IOException e){
            Log.e(TAG,"Error writing " + type + ": " + e.toString());
        }
    }
    private int[] readCpuSpeeds(){
        /* Read the avalible CPU speeds */
        try{
            FileInputStream in = null;
            int cnt=0,ch;
            int[] result;
            String vals[]=new String[50];

            in = new FileInputStream(CPU0_FREQ_PATH + "scaling_available_frequencies");
            vals[0]="";
            while(cnt<50){
                ch=in.read();
                if(ch==-1){
                    if(!vals[cnt].equals("")){
                        cnt++;
                    }
                    break;
                }
                if(ch >= 0x30 && ch <= 0x39){
                    vals[cnt]+=(char)ch;
                }
                else if(ch == 0x20 && (!vals[cnt].equals(""))){
                    cnt++;
                    if(cnt < 50)
                        vals[cnt]="";
                }
            }
            in.close();
            result=new int[cnt];
            for(int i=0; i<cnt; i++){
                result[i]=Integer.parseInt(vals[i]);
            }
            return result;
        }
        catch(IOException e){
            Log.e(TAG,"Error reading CPU Speeds: " + e.toString());
            return null;
        }
    }

    @Override
    public void run() {
        /* Run CPU Speed (CPUFreq) Service 
         * First wait for the screen to go to sleep; then make the first update
         */
        int cpuMin,cpuWakeMax,cpuSleepMax; 
        int[] cpuSpeeds=readCpuSpeeds();

        if(cpuSpeeds == null){
            //Nothing to do
            return;
        }
        for(int i=0;i<cpuSpeeds.length;i++){
            Log.i(TAG,"CPU Speed: " + cpuSpeeds[i]);
        }

        cpuMin = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.CPUFREQ_PERSIST_MIN, -1);
        cpuWakeMax = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.CPUFREQ_PERSIST_WAKEMAX, -1);
        cpuSleepMax = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.CPUFREQ_PERSIST_SLEEPMAX, -1);

        //Validate Speeds
        for(int i=0;i<=cpuSpeeds.length && cpuMin > 0;i++){
            if(i==cpuSpeeds.length)
                cpuMin = -1;
            else if(cpuSpeeds[i]==cpuMin)
                break;
        }
        for(int i=0;i<=cpuSpeeds.length && cpuWakeMax > 0;i++){
            if(i==cpuSpeeds.length)
                cpuWakeMax = -1;
            else if(cpuSpeeds[i]==cpuWakeMax)
                break;
        }
        for(int i=0;i<=cpuSpeeds.length && cpuSleepMax > 0;i++){
            if(i==cpuSpeeds.length)
                cpuSleepMax = -1;
            else if(cpuSpeeds[i]==cpuSleepMax)
                break;
        }

        //Ensure speeds are calculated
        if(cpuMin == -1 || cpuWakeMax == -1 || cpuSleepMax == -1){
            Log.i(TAG,"Missing CPUFREQ defaults, re-calculating");
            cpuMin=cpuReadInt("scaling_min_freq");
            if(cpuMin == -1){
                cpuMin=0;
            }
            cpuWakeMax=cpuReadInt("scaling_max_freq");
            if(cpuWakeMax == -1){
                cpuWakeMax=0;
                cpuSleepMax=0;
            }
            else{
                cpuSleepMax=0;
                for(int i=1;i<cpuSpeeds.length;i++){
                    if(cpuSpeeds[i]==cpuWakeMax && cpuSpeeds[i-1] > cpuMin){
                        cpuSleepMax=cpuSpeeds[i-1];
                    }
                }
            }
        }
        //Set system active speeds
        Log.i(TAG,"Min = " + cpuMin + " | wakemax = " + cpuWakeMax + 
                  " | sleepmax = " + cpuSleepMax);
        Settings.Secure.putInt(mContentResolver,
            Settings.Secure.CPUFREQ_ACTIVE_MIN, cpuMin);
        Settings.Secure.putInt(mContentResolver,
            Settings.Secure.CPUFREQ_ACTIVE_WAKEMAX, cpuWakeMax);
        Settings.Secure.putInt(mContentResolver,
            Settings.Secure.CPUFREQ_ACTIVE_SLEEPMAX, cpuSleepMax);
    
        //Main Loop
        Log.i(TAG,"Starting main loop");
        while(true){
            if(waitForFBSleep(true) == -1)
                break;
            //screen is now off
            cpuMin = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.CPUFREQ_ACTIVE_MIN, 0);
            cpuSleepMax = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.CPUFREQ_ACTIVE_SLEEPMAX, 0);
            if(cpuMin != 0){
                Log.i(TAG,"ScreenOff Min = " + cpuMin); 
                cpuWriteInt("scaling_min_freq",cpuMin);
            }
            if(cpuSleepMax != 0){
                Log.i(TAG,"ScreenOff Max = " + cpuSleepMax); 
                cpuWriteInt("scaling_max_freq",cpuSleepMax);
            }

            if(waitForFBSleep(false) == -1)
                break;
            //screen is now on
            cpuMin = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.CPUFREQ_ACTIVE_MIN, 0);
            cpuWakeMax = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.CPUFREQ_ACTIVE_WAKEMAX, 0);
            if(cpuMin != 0){
                Log.i(TAG,"ScreenOn Min = " + cpuMin); 
                cpuWriteInt("scaling_min_freq",cpuMin);
            }
            if(cpuWakeMax != 0){
                Log.i(TAG,"ScreenOn Max = " + cpuWakeMax); 
                cpuWriteInt("scaling_max_freq",cpuWakeMax);
            }
        }
    }
}
