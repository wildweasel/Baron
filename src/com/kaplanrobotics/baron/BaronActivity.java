package com.kaplanrobotics.baron;

import java.lang.ref.WeakReference;

import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class BaronActivity extends Activity {
	
	private static final String TAG = BaronActivity.class.getSimpleName();

	// comms
	ArduinoControl arduinoControl;
    Handler sensorDataHandler;

    Baron baron;

    // UI elements
    ConsoleTextView debugConsole;
    Handler debugTextHandler;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_robot_brain);

		Log.e(TAG, "onCreate()");
		
		// Debug setup
		debugConsole = (ConsoleTextView) findViewById(R.id.debug_console);
		debugConsole.setMaxSize(25);
		debugConsole.addLine("onCreate()");
		debugTextHandler = new DebugTextHandler(debugConsole);
		
		// Comms setup
		sensorDataHandler = new SensorHandler(this);
		arduinoControl = new ArduinoControl(this, sensorDataHandler);
		baron = new Baron(arduinoControl, debugTextHandler);		
	}

	@Override
	public void onResume() {
		super.onResume();		
		Log.e(TAG, "onResume()");
		debugConsole.addLine("onResume()");
		arduinoControl.resume();
		baron.resume();
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.e(TAG, "onPause()");
		debugConsole.addLine("onPause()");
		arduinoControl.pause();
		baron.pause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.e(TAG, "onDestroy()");
		debugConsole.addLine("onDestroy()");
		arduinoControl.destroy();
		baron.destroy();
	}
	
	private static class DebugTextHandler extends Handler{
		private final WeakReference<ConsoleTextView> wrConsoleTextView;
		
		public DebugTextHandler(ConsoleTextView consoleTextView){
			wrConsoleTextView = new WeakReference<ConsoleTextView>(consoleTextView);
		}
		@Override
		public void handleMessage(Message msg) {
			ConsoleTextView consoleTextView = wrConsoleTextView.get();
			consoleTextView.addLine(msg.getData().getString(Baron.DEBUG_TEXT_TAG));
		}
	};
	
	// Handles 
	private static class SensorHandler extends Handler{
		private final WeakReference<BaronActivity> wrRobotBrainActivity;
		
		public SensorHandler(BaronActivity baronActivity){
			wrRobotBrainActivity = new WeakReference<BaronActivity>(baronActivity);
		}
		
		@Override
		public void handleMessage(Message msg){
			BaronActivity robotBrainActivity = wrRobotBrainActivity.get();
			if(robotBrainActivity != null){
				byte message[] = msg.getData().getByteArray("data");

				// parse message
				robotBrainActivity.baron.parseIncomingMessage(message);
			}
		}
	}

}
