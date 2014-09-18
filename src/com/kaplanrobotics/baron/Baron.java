package com.kaplanrobotics.baron;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Baron {
	
	private static final String TAG = Baron.class.getSimpleName();
	
	public static final String DEBUG_TEXT_TAG = "line";

	// Drive message label
    private static final byte COMMAND_START = -0x2; 

	ArduinoControl arduinoControl;
	BaronBrain baronBrain;		
	Thread baronBrainThread;
	
	Handler debugTextHandler;
	
	Baron(ArduinoControl arduinoControl, Handler debugTextHandler){
		this.debugTextHandler = debugTextHandler;		

		publishMessage(TAG,"Baron()");

		this.arduinoControl = arduinoControl;
		baronBrain = new BaronBrain(this); 
		baronBrainThread = new Thread(baronBrain);
		baronBrainThread.start();
	}
	
	void pause(){
		publishMessage(TAG,"pause()");
		baronBrain.pause();
	}
	
	void resume(){
		publishMessage(TAG,"resume()");
		baronBrain.resume();
	}
	
	void destroy(){
		publishMessage(TAG,"destroy()");
		baronBrain.destroy();
		publishMessage(TAG,"destroyED BRAIN!");
		
		while(true){
			try{
				publishMessage(TAG,"HULK SMASH!");
				
				baronBrainThread.join();
				
				publishMessage(TAG,"HULK SMASH DONE!");
				
			}
			catch (InterruptedException e){}
		}
	}
	
	void parseIncomingMessage(byte[] message){
		baronBrain.odometeryMessage();
		baronBrain.irSensorMessage();
	}
	
	void sendDriveMessage(float linearVelocity, float angularVelocity){
		if(arduinoControl.isBaronReady()){			
			int motorSpeeds[] = toDifferentialDrive(linearVelocity, angularVelocity);
			byte buffer[] = toDriveMessage(motorSpeeds);			
			arduinoControl.sendMessage(buffer);
		}
	}
	
	// Convert from the unicycle drive dynamics used by BaronBrain to the 
	// differential drive (dual motors - one per wheel) used by the actual robot
	// Both linearVelocity and angularVelocity are in the range [0,1].
	// For linearVelocity, 0 is no forward motion, and 1 is max forward motion (both motors full speed ahead in one direction)  
	// For angularVelocity, 0 is max CCW rotation, and 1 is max CW rotation (both motors in opposite direction for these) 	
	private int[] toDifferentialDrive(float linearVelocity, float angularVelocity) {
		
		// The motors can't operate past 100%
		// Scale as to preserve angular velocity		
		// Too much left motor
		if(linearVelocity > 2 * angularVelocity)
			linearVelocity = 2 * angularVelocity;		
		// Too much right motor
		else if(linearVelocity > -2 * angularVelocity + 2)
			linearVelocity = -2 * angularVelocity + 2; 			

		// Too much right motor in reverse
		if(linearVelocity < -2 * angularVelocity)
			linearVelocity = -2 * angularVelocity;
		// Too much left motor in reverse
		else if(linearVelocity < 2 * angularVelocity - 2)
			linearVelocity = 2 * angularVelocity - 2;
			
		// sit still at home 
		if(linearVelocity == 0 && angularVelocity == 0){
			int out[] = {0,0};
			return out;
		}
		
		// v_r = v_max * (rScale + 2*thetaScale - 1)
		// v_l = v_max * (rScale - 2*thetaScale + 1)
		int out [] = {(int) (100 * (linearVelocity + 2*angularVelocity - 1)), (int) (100 * (linearVelocity - 2*angularVelocity + 1))};
		return out;
	}

	private byte[] toDriveMessage(int[] motorSpeeds) {
        byte[] buffer = new byte[3]; 
        buffer[0] = COMMAND_START; 
        buffer[1] = (byte) motorSpeeds[1]; 
		buffer[2] = (byte) motorSpeeds[2];
		return buffer;

	}

	public void publishMessage(String tag, String text){
		Bundle bundle = new Bundle();
		bundle.putString(DEBUG_TEXT_TAG, tag+" - "+text);
		Message message = debugTextHandler.obtainMessage();
		message.setData(bundle);
		debugTextHandler.sendMessage(message);
		Log.e(tag, text);
	}
	
}
