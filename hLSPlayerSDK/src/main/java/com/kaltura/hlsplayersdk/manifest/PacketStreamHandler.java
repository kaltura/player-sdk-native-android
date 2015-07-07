package com.kaltura.hlsplayersdk.manifest;

import com.kaltura.hlsplayersdk.types.ByteArray;

public interface PacketStreamHandler
{
	public void onComplete(int packetID, ByteArray buffer);
	public boolean onProgress(int packetID, ByteArray buffer);
}
