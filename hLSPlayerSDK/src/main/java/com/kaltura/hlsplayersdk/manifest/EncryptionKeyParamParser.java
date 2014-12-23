package com.kaltura.hlsplayersdk.manifest;

import java.util.ArrayList;

public class EncryptionKeyParamParser {
	
	private static final int STATE_PARSE_NAME = 0;
	private static final int STATE_BEGIN_PARSE_VALUE = 1;
	private static final int STATE_PARSE_VALUE = 2;
	
	public static String[] parseParams(String paramString)
	{
		ArrayList<String> result = new ArrayList<String>();
		int cursor = 0;
		int state = STATE_PARSE_NAME;
		String accum = "";
		boolean usingQuotes = false;
		
		while (cursor < paramString.length())
		{
			char c = paramString.charAt(cursor);
			
			switch (state)
			{
			case STATE_PARSE_NAME:
				if (c == '=')
				{
					result.add(accum);
					accum = "";
					state = STATE_BEGIN_PARSE_VALUE;
				}
				else accum += c;
				break;
				
			case STATE_BEGIN_PARSE_VALUE:
				if (c == '"') usingQuotes = true;
				else accum += c;
				state = STATE_PARSE_VALUE;
				break;
				
			case STATE_PARSE_VALUE:
				if (!usingQuotes && c == ',')
				{
					result.add(accum);
					accum = "";
					state = STATE_PARSE_NAME;
					break;
				}
				
				if (usingQuotes  &&  c == '"')
				{
					usingQuotes = false;
					break;
				}
				
				accum += c;
				break;
				
			}
			
			++cursor;
			
			if (cursor == paramString.length()) result.add(accum);
			
		}
		
		return result.toArray(new String[result.size()]);
	}
}
