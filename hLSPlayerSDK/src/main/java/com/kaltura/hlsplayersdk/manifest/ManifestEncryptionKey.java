package com.kaltura.hlsplayersdk.manifest;

import java.util.HashMap;
import java.util.Locale;

public class ManifestEncryptionKey {

	private static HashMap KEY_CACHE = new HashMap(); 

	public boolean usePadding = false;
	public String iv;
	public String url;
	
	// Keep track of the segments this key applies to
	public int startSegmentId = 0;
	public int endSegmentId = Integer.MAX_VALUE;
	
	@Override
	public String toString()
	{
		return "ManifestEncryptionKey[" + url + "][" + iv + "]";
	}
	
	public ManifestEncryptionKey()
	{
		
	}
	
	public static void clearKeyCache()
	{
		KEY_CACHE.clear();
	}
	
	public static ManifestEncryptionKey fromParams(String params)
	{
		ManifestEncryptionKey result = new ManifestEncryptionKey();
		
		String[] tokens = EncryptionKeyParamParser.parseParams(params);
		int tokenCount = tokens.length;
		
		for (int i = 0; i < tokenCount; i += 2)
		{
			String name = tokens[i];
			String value = tokens[i+1];
			
			if (name.equals("URI"))
			{
				result.url = value;
			}
			else if (name.equals("IV"))
			{
				result.iv = value;
			}
		}
		return result;
	}

	public String getIV(int id)
	{
		if (iv != null) return iv;
		return createIVFromID(id);
	}
	
	private String createIVFromID(int id)
	{
		String result = "0x" + String.format(Locale.US, "%032x", id);
		return result;
	}
}
