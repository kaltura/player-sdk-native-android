package com.kaltura.hlsplayersdk.subtitles;

import android.util.Log;

public class WebVTTRegion {
	public String id = "";
	public double width = 100;
	public int lines = 3;
	public double anchorX = 0;
	public double anchorY = 100;
	public double viewportAnchorX = 0;
	public double viewportAnchorY = 0;
	public String scroll = "scroll none";
	
	public WebVTTRegion()
	{
		
	}
	
	public static WebVTTRegion fromString(String input)
	{
		WebVTTRegion result = new WebVTTRegion();
		
		String[] settings = input.split(" ");
		
		for (int i = 0; i < settings.length; ++i)
		{
			String token = settings[i];
			int equalsIndex = token.indexOf('=');
			
			// Check if valid token, otherwise skip
			if (equalsIndex < 1 || equalsIndex == token.length() - 1) continue;
			
			String name = token.substring(0, equalsIndex);
			String value = token.substring(equalsIndex + 1);
			
			if (name.equals("id"))
			{
				result.id = value;
			}
			else if (name.equals("width"))
			{
				result.width = parsePercentage(value);
			}
			else if (name.equals("lines"))
			{
				result.lines = Integer.parseInt(value);
			}
			else if (name.equals("regionanchor"))
			{
				int commaIndex = value.indexOf(',');
				result.anchorX = parsePercentage(value.substring(0, commaIndex));
				result.anchorY = parsePercentage(value.substring(commaIndex + 1));
			}
			else if (name.equals("viewportanchor"))
			{
				int commaIndex = value.indexOf(',');
				result.viewportAnchorX = parsePercentage(value.substring(0, commaIndex));
				result.viewportAnchorY = parsePercentage(value.substring(commaIndex + 1));
			}
			else if (name.equals("scroll"))
			{
				if (value.equals("up")) result.scroll = "scroll up";
			}
			else
			{
				Log.i("WebVTTRegion.fromString", "Unknown token " + name + ". Ignoring.");
			}
			
		}
		return result;
	}
	
	private static double parsePercentage(String input)
	{
		if (input.charAt(input.length() - 1) != '%') return 0;
		return Double.parseDouble(input.substring(0, input.length() - 1));
	}

}
