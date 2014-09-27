package com.kaplanrobotics.baron;

import android.graphics.PointF;

public class BaronBrain implements Runnable{
	
	private static final String TAG = BaronBrain.class.getSimpleName();

	private final static int INSTRUCTIONS_PER_SEC = 1; 
	
	private final static float piF = (float) Math.PI;
	
	Baron baron;
	
	long lastTimeMillis;
	private volatile boolean paused = true;
		
	// Track world
	private class BaronWorldInfo{
		float xPos = 0;
		float yPos = 0;
		float theta = 0;
		float leftDistance = 50;
		float leftCenterDistance = 50;
		float centerDistance = 50;
		float rightCenterDistance = 50;
		float rightDistance = 50;
	}
	
	// Physical Robot constants  - meters
	private static final float WHEEL_RADIUS = 0.033f;
	private static final float WHEELBASE_LENGTH = 0.144f;
	// 10 spokes per wheel, each spoke ticks on and off
	private static final int WHEEL_TICKS_PER_REVOLUTION = 20;	
	private static final float METERS_PER_TICK = 2 * piF * WHEEL_RADIUS/WHEEL_TICKS_PER_REVOLUTION;
	
	BaronWorldInfo baronInfo;
	
	PointF goal;
	
	BaronBrain(Baron baron){
		baron.publishMessage(TAG,"BaronBrain()");
		this.baron = baron;
		lastTimeMillis = System.currentTimeMillis();
		baronInfo = new BaronWorldInfo();
		
		// We're fine where we're at until we hear otherwise
		goal = new PointF(0,0);
	}
	
	public void pause(){
		baron.publishMessage(TAG,"pause()");
		paused = true;
	}
	
	public void resume(){
		baron.publishMessage(TAG,"resume()");
		paused = false;
	}
	
	public void destroy(){
		baron.publishMessage(TAG,"destroy()");
	}
	
	public void setGoal(float x, float y){
		goal.x = x;
		goal.y = y;
	}
	
	boolean leftCovered = true, rightCovered = true;
	int leftTickCount = 0, rightTickCount = 0;
	
	// Right now, the incoming messages gives us the state of each wheel at given interval
	// We may need to change this to the number of state changes within a given interval
	public void odometeryMessage(boolean leftCovered, boolean rightCovered, boolean leftPositive, boolean rightPositive) {

		// Remember, we should get a message on EVERY wheel encoder state change
		if(this.leftCovered ^ leftCovered){
			if(leftPositive)
				leftTickCount++;
			else
				leftTickCount--;
			this.leftCovered = leftCovered;
		}
		if(this.rightCovered ^ rightCovered){
			if(rightPositive)
				rightTickCount++;
			else
				rightTickCount--;
			this.rightCovered = rightCovered;
		}	
	}
	
	private void consumeWheelEncoderTicks(){		
		
		float ticks_per_rev_center = (float) (Math.ceil(WHEEL_TICKS_PER_REVOLUTION / 2) - 1);
        float adj_deltaticks_r = (rightTickCount + ticks_per_rev_center) % WHEEL_TICKS_PER_REVOLUTION - ticks_per_rev_center;
        float adj_deltaticks_l = (leftTickCount  + ticks_per_rev_center) % WHEEL_TICKS_PER_REVOLUTION - ticks_per_rev_center;           
       
        float D_r = METERS_PER_TICK * adj_deltaticks_r;
        float D_l = METERS_PER_TICK * adj_deltaticks_l;
        float D_c = (D_r + D_l)/2;
        
        float x_dt = D_c * (float) Math.cos(baronInfo.theta);
        float y_dt = D_c * (float) Math.sin(baronInfo.theta);
        float theta_dt = (D_r - D_l) / WHEELBASE_LENGTH;
        
        baronInfo.xPos += x_dt;
        baronInfo.yPos += y_dt;
        baronInfo.theta += theta_dt;
        
        // reset the counters
    	leftTickCount = 0;
    	rightTickCount = 0;

		
	}

	public void irSensorMessage(float leftIR, float leftCenterIR, float centerIR, float rightCenterIR, float rightIR) {
		baronInfo.leftDistance = leftIR;
		baronInfo.leftCenterDistance = leftCenterIR;
		baronInfo.centerDistance = centerIR;
		baronInfo.rightCenterDistance = rightCenterIR;
		baronInfo.rightDistance = rightIR;
	}

	@Override
	public void run() {
		while(!Thread.currentThread().isInterrupted()){

			if(!paused && System.currentTimeMillis() - lastTimeMillis >= 1000/INSTRUCTIONS_PER_SEC){
				// Where we at?
				consumeWheelEncoderTicks();
				
				baron.publishMessage(TAG,"Baron at ("+baronInfo.xPos+", "+baronInfo.yPos+"), facing "+baronInfo.theta*180/piF+" degrees");
				
				issueDriveCommand();
				lastTimeMillis = System.currentTimeMillis();
			}
		}
		
	}
	

	private void issueDriveCommand(){
		// Scale to [0,1]
		float linearVelocity = 0.5f;		
		float angularVelocity = (goToGoal() + piF) / (2*piF);
		baron.sendDriveMessage(linearVelocity, angularVelocity);
		
		//baron.publishMessage(TAG, "Issuing Drive Command.  v = "+linearVelocity+", omega = "+angularVelocity);
		
	}
	
	// Error trackers
	float accumulatedError = 0, previousError = 0;
	// PID constants
	float Kp = 1;
	float Ki = 0.1f;
	float Kd = 0.2f;
	
	private float goToGoal(){
		// Calculate heading error
		float headingError = baronInfo.theta - (float) Math.atan2(goal.x - baronInfo.xPos, goal.y - baronInfo.yPos);
		//  fix to [-pi, pi] range
		headingError = (headingError + piF) % (2 * piF) - piF;
		
		// PID controller for steering Angle
		float proportionalError = headingError;
		float integralError = accumulatedError + headingError;
		float derivativeError = headingError - previousError;
		
		float outputAngle = Kp * proportionalError + Ki * integralError + Kd*derivativeError;
		
		// update errors
		accumulatedError = integralError;
		previousError = headingError;
		
		return outputAngle;
	}

}
