package com.kaplanrobotics.baron;

public class BaronBrain implements Runnable{
	
	private static final String TAG = BaronBrain.class.getSimpleName();

	private final static int INSTRUCTIONS_PER_SEC = 1; 
	
	Baron baron;
	
	long lastTimeMillis;
	private volatile boolean running;
	private volatile boolean paused = true;
		
	// Track world
	
	BaronBrain(Baron baron){
		baron.publishMessage(TAG,"BaronBrain()");
		this.baron = baron;
		running = true;
		lastTimeMillis = System.currentTimeMillis();
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
		running = false;
	}
	
	public void odometeryMessage() {
		// TODO Auto-generated method stub
		
	}

	public void irSensorMessage() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run() {

		while(running){
			
			if(!paused && System.currentTimeMillis() - lastTimeMillis >= 1000/INSTRUCTIONS_PER_SEC){
				issueDriveCommand();
				lastTimeMillis = System.currentTimeMillis();
			}
		}
		
	}
	

	private void issueDriveCommand(){
		float linearVelocity = 0;
		float angularVelocity = 0;
		baron.sendDriveMessage(linearVelocity, angularVelocity);
		
		//baron.publishMessage(TAG, "Issuing Drive Command.  v = "+linearVelocity+", omega = "+angularVelocity);
		
	}

}
