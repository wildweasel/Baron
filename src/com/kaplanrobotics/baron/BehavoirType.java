package com.kaplanrobotics.baron;

import android.graphics.PointF;

import com.kaplanrobotics.baron.BaronBrain.BaronWorldInfo;

public enum BehavoirType {
	GO_TO_GOAL(0);
	Behavoir behavoir;
	
	BehavoirType(int code){
		switch (code){
		case 0:
			behavoir = new GoToGoal();
			break;
		default:
			behavoir = new GoToGoal();
			break;		
		}
	}
	
	public interface Behavoir{
		 PointF getCommand(BaronWorldInfo baronInfo);
	}
	
	public PointF getCommand(BaronWorldInfo baronInfo) {
		return behavoir.getCommand(baronInfo);
	}

}
