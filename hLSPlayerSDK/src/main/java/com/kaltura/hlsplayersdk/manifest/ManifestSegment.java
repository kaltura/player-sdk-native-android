package com.kaltura.hlsplayersdk.manifest;

import java.nio.ByteBuffer;
import com.kaltura.hlsplayersdk.cache.HLSSegmentCache;
import com.kaltura.hlsplayersdk.cache.SegmentCacheItem;

import android.util.Log;

public class ManifestSegment extends BaseManifestItem 
{
	public ManifestSegment()
	{
		type = ManifestParser.SEGMENT;
	}
	
	public int id = 0;  // Based on the mediaSequence number
	//public String uri = "";
	public double duration;
	public String title;
	public double startTime;
	public int continuityEra;
	public int quality = 0;
	
	// Byte Range support. -1 means no byte range.
	public int byteRangeStart = -1;
	public int byteRangeEnd = -1;
	
	public ManifestSegment altAudioSegment = null;
	public int altAudioIndex = -1;
	
	public ManifestEncryptionKey key = null;

	public int cryptoId = -1;
	
	public double endTime()
	{
		return startTime + duration;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("id : " + id + " | ");
		sb.append("duration : " + duration + " | ");
		sb.append("title : " + title + " | ");
		sb.append("startTime : " + startTime + " | ");
		sb.append("continuityEra : " + continuityEra + " | ");
		sb.append("quality : " + quality + " | ");
		sb.append("byteRangeStart : " + byteRangeStart + " | ");
		sb.append("cryptoId : " + cryptoId + " | ");
		sb.append("byteRangeEnd : " + byteRangeEnd + " | ");
		sb.append("uri : " + uri + "\n");
		
		
		return sb.toString();
		
	}

	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}

	public void initializeCrypto(ManifestEncryptionKey eKey)
	{
		if(cryptoId != -1 || eKey == null)
			return;
		
		key = eKey;
		
		// If we're already in the cache, use the same handle
		cryptoId = HLSSegmentCache.getCryptoId(uri);
		if (cryptoId != -1) 
			return;

		// Read the key optimistically.
		ByteBuffer keyBytes = ByteBuffer.allocate(16);
		HLSSegmentCache.read(key.url, 0, 16, keyBytes);

		// Generate IV and cut off 0x part if present.
		String ivStr = key.getIV(id);
		if(ivStr.indexOf("0x") == 0 || ivStr.indexOf("0X") == 0)
			ivStr = ivStr.substring(2);

		byte[] iv = hexStringToByteArray(ivStr);

		cryptoId = SegmentCacheItem.allocAESCryptoState(keyBytes.array(), iv);
		Log.i("Crypto", "Got crypto ID " + cryptoId);
	}

}
