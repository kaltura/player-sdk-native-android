package com.kaltura.hlsplayersdk.manifest;

import android.util.Log;

public class ManifestStream extends BaseManifestItem {

	public int programId;
	public int bandwidth;
	public String codecs;
	public int width;
	public int height;
	public ManifestStream backupStream = null;
	public int numBackups = 0;
	
	
	public static ManifestStream fromString(String input)
	{
		ManifestStream newNote = new ManifestStream();
		
		newNote.type = ManifestParser.VIDEO;
		
		String accum="";
		String tmpKey = null;
		
		for (int i = 0; i < input.length(); ++i)
		{
			char curChar = input.charAt(i);
			
			if (curChar == '=')
			{
				if (tmpKey != null)
					Log.w("" , "Found unexpected =");
				
				tmpKey = accum;
				accum = "";
				continue;
				
			}
			
			if (curChar == ',' || i == input.length() - 1)
			{
				// Grab the last character and accumulate it.
				if (i == input.length() - 1)
					accum += curChar;
				
				if (tmpKey == null)
				{
					Log.w("ManifestStream.fromString", "No key set but found end of key-value pair, ignoring...");
					continue;
				}
				
				// We found the end of a value.
				if (tmpKey.equals("PROGRAM-ID"))
				{
					newNote.programId = Integer.parseInt(accum);
				}
				else if (tmpKey.equals("BANDWIDTH"))
				{
					newNote.bandwidth = Integer.parseInt(accum);
				}
				else if (tmpKey.equals("CODECS"))
				{
					newNote.codecs = accum;
				}
				else if (tmpKey.equals("RESOLUTION"))
				{
					String [] resSplit = accum.split("x");
					if (resSplit.length >= 2)
					{
						newNote.width = Integer.parseInt(resSplit[0]);
						newNote.height = Integer.parseInt(resSplit[1]);
					}
					else
					{
						Log.w("ManifestStream.fromString", "Argument " + accum + " did not split into two arguments (split('x'))");
					}
				}
				else
				{
					Log.w("ManifestStream.fromString", "Unexpected key '" + tmpKey + "', ignoring...");
				}
				
				tmpKey = null;
				accum = "";
				continue;
				
			}
			
			if (curChar == '\"')
			{
				// Walk ahead until next ", this is a quoted string.
				curChar = 0;
				int j = i + 1;
				for (j = i + 1; j < input.length(); ++j)
				{
					if (input.charAt(j) == '\"')
						break;
					accum += input.charAt(j);
				}
				
				// Bump our next character.
				i = j;
				
			}
			
			// Build up the accumulator.
			accum += curChar;
			
		}
		return newNote;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("programId : " + programId + " | ");
		sb.append("bandwidth : " + bandwidth + " | ");
		sb.append("codecs : " + codecs + " | ");
		sb.append("width : " + height + " | ");
		sb.append("programId : " + programId + " | ");
		sb.append(super.toString() + "\n");
		return sb.toString();
	}
}
