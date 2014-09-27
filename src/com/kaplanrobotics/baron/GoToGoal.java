package com.kaplanrobotics.baron;

import com.kaplanrobotics.baron.BaronBrain.BaronWorldInfo;

import android.graphics.PointF;

public class GoToGoal implements BehavoirType.Behavoir {

	private final static float piF = (float) Math.PI;
	// How close to the goal is good enough?
	private static final float DISTANCE_TO_GOAL_TOLERANCE = 0.1f;
	
	// Error trackers
	float accumulatedError = 0, previousError = 0;
	// PID constants
	float Kp = 1;
	float Ki = 0.1f;
	float Kd = 0.2f;
	
	@Override
	public PointF getCommand(BaronWorldInfo baronInfo) {
		// Unless we're close enough to the goal
		if(Math.hypot(baronInfo.goal.x - baronInfo.xPos, baronInfo.goal.y - baronInfo.yPos) > DISTANCE_TO_GOAL_TOLERANCE){		
			
			// Calculate heading error
			float headingError = baronInfo.theta - (float) Math.atan2(baronInfo.goal.x - baronInfo.xPos, baronInfo.goal.y - baronInfo.yPos);
			//  fix to [-pi, pi] range
			headingError = (headingError + piF) % (2 * piF) - piF;
			
			// PID controller for steering Angle
			float proportionalError = headingError;
			float integralError = accumulatedError + headingError;
			float derivativeError = headingError - previousError;
			
			float angularVelocity = Kp * proportionalError + Ki * integralError + Kd*derivativeError;
			
			// update errors
			accumulatedError = integralError;
			previousError = headingError;
			
			// prepare output
			// Scale to [0,1]
			angularVelocity = (angularVelocity + piF) / (2*piF);				
			return new PointF(0.5f, angularVelocity);
		}
		else
			// we're here - stop
			return new PointF(0,0.5f);
	}

}
