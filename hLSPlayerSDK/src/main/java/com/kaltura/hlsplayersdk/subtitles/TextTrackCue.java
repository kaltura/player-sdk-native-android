package com.kaltura.hlsplayersdk.subtitles;

import android.util.Log;

public class TextTrackCue {
	private enum ParseState
	{
		WHITESPACE,
		TIMESTAMP,
		PARSE_TOKENS
	}
	
	public String id = "";
	public boolean pauseOnExit = false;
	public String regionName = "";
	public WebVTTRegion region = null;
	public String writingDirection = "horizontal";
	public boolean snapToLines = false;
	public String linePosition = "auto";
	public String lineAlignment = "start alignment";
	public int textPosition = 50;
	public String textPositionAlignment = "middle alignment";
	public int size = 100;
	public String textAlignment = "middle alignment";
	public String text = "";
	public String buffer = "";
	
	public double startTime = -1;
	public double endTime = -1;
	
	public TextTrackCue()
	{
		
	}
	
	@Override
	public String toString()
	{
		return "Cue: " + startTime + "-->" + endTime + " :: " + text;
	}
	
	public void parse(String input)
	{
		int position = 0;
		ParseState state = ParseState.WHITESPACE;
		String accum = "";
		
		// Remove tabs, just in case
		input = input.replace("\t", " ");
		
		while (position < input.length())
		{
			char c = input.charAt(position);
			switch (state)
			{
			case WHITESPACE:
				if (c == ' ') ++position;
				else if (startTime == -1 || endTime == -1) state = ParseState.TIMESTAMP;
				else state = ParseState.PARSE_TOKENS;
				break;
			case TIMESTAMP:
				if (c != ' ')
					accum += c;
				if (c == ' ' || position == input.length() - 1)
				{
					double timeStamp = SubTitleSegment.parseTimeStamp(accum);
					if (startTime == -1) startTime = timeStamp;
					else endTime = timeStamp;
					accum = "";
					int arrowIndex = input.indexOf("-->", position);
					if (arrowIndex != -1) position = arrowIndex + 3;
					state = ParseState.WHITESPACE;
					break;
				}
				++position;
				break;
				
			case PARSE_TOKENS:
				parseTokens(input.substring(position));
				position = input.length();
				break;
			}
		}
	}

	private void parseTokens(String input)
	{
		String[] tokens = input.split(" ");
		for (int i = 0; i < tokens.length; ++i)
		{
			String token = tokens[i];
			int colonIndex = token.indexOf(':');
			
			if (colonIndex == -1) continue;
			
			String name = token.substring(0, colonIndex);
			String value = token.substring(colonIndex + 1);
			// The following two items don't seem to be used... dead code in the flash?
//			boolean positionSet = false;
//			boolean positionAlignSet = false;
			
			if (name.equals("region"))
			{
				regionName = value;
			}
			else if (name.equals("vertical"))
			{
				writingDirection = value;
			}
			else if (name.equals("line"))
			{
				// Not yet implemented
				Log.i("TextTrackCue.parseTokens", " 'line' NOT SUPPORTED");
			}
			else if (name.equals("size"))
			{
				// not yet implemented
				Log.i("TextTrackCue.parseTokens", " 'size' NOT SUPPORTED");
			}
			else
			{
				Log.i("TextTrackCue.parseTokens", "Unknown tag " + name + ". Ignoring.");
			}
			
		}
	}
}
