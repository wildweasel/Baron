package com.kaplanrobotics.baron;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class ConsoleTextView extends TextView{
	
	String consoleBuffer[];
	
	int maxSize = 1;
	int size = 0;
	int head = 0;

	public ConsoleTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	void setMaxSize(int maxSize){
		this.maxSize = maxSize;
		consoleBuffer = new String[maxSize];
	}
	
	void addLine(String line){
		consoleBuffer[head] = line;
		if(size < maxSize)
			size++;
		
		String text = "";		
		for(int i = 0; i < size; i++)
			text += consoleBuffer[(head-i+size)%size]+"\n";			
		this.setText(text);		

		head = ++head % maxSize;
	}

}
