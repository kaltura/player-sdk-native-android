package com.kaltura.hlsplayersdk.manifest;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.kaltura.hlsplayersdk.types.ByteArray;

public class M2TSParser implements PacketStreamHandler
{
	public class PacketStream
	{
		private static final int MAX_PACKET_SIZE = 256;
		
		private ByteArray _buffer = null;
		private int _packetID = 0;
		private int _lastContinuity = -1;
		
		public PacketStream(int packetID)
		{
			_buffer = new ByteArray(MAX_PACKET_SIZE);
			_packetID = packetID;			
		}
		
		public void appendBytes(ByteArray bytes, int offset, int length, boolean payloadStart, int continuityCounter, boolean discontinuity, PacketStreamHandler packetStreamHandler)
		{
			
			if (_lastContinuity == continuityCounter)
			{
				// Ignore duplicate packets.
				if ( (!payloadStart)
				  && (!discontinuity))
					return; // duplicate
			}
			
			if (payloadStart)
			{
				onPacketComplete(packetStreamHandler);
			}
			else
			{
				if (_lastContinuity < 0)
					return;
				
				if ( (((_lastContinuity + 1) & 0x0f) != continuityCounter) && !discontinuity)
				{
					// Corrupt packet - skip it.
					_buffer.clear();
					_lastContinuity = -1;
					
					return;
				}
			}
			
			if (length > 0)
				_buffer.write(bytes, offset, length);
			
			_lastContinuity = continuityCounter;
			
			if ( (length > 0)
				&& (_buffer.length() > 1)
				&& (packetStreamHandler.onProgress(_packetID, _buffer)))
			{
				_buffer.clear();
				_lastContinuity = -1;
			}
			
			// Check to see if we can fire a complete PES packet...
			if (_buffer.length() > 5)
			{
				// extract length...
				int packetLength = ((_buffer.unsigned(4) << 8) + _buffer.unsigned(5));
				
				// Check against observed payload.
				if (packetLength > 0)
				{
					if (_buffer.length() >= packetLength + 6)
					{
						onPacketComplete(packetStreamHandler);
					}
				}
			}
			
		}
		
		private void onPacketComplete(PacketStreamHandler packetStreamHandler)
		{
			if (_buffer.length() > 1)
				packetStreamHandler.onComplete(_packetID, _buffer);
			
			_buffer.clear();
		}
	}
	
	public class PESPacketStream
	{
		public PESPacketStream ( double pts, double dts)
		{
			_buffer = new ByteArray(M2TSParser.PacketStream.MAX_PACKET_SIZE);
			_shiftBuffer = new ByteArray(M2TSParser.PacketStream.MAX_PACKET_SIZE);
		}
		
		public void shiftLeft(int num)
		{
			int newLength = _buffer.length() - num;
			ByteArray tmpBytes = null;
		
			_shiftBuffer.clear();
			_shiftBuffer.write(_buffer, num, newLength);
			
			tmpBytes = _buffer;
			_buffer = _shiftBuffer;
			_shiftBuffer = tmpBytes;
		}
		
		public ByteArray _buffer = null;
		public long _pts;
		public long _dts;
		
		private ByteArray _shiftBuffer = null;
	}
	
	public static byte [] _totalh264 = null;
	
	private ByteArray _buffer = null;
	private Map<Integer, PacketStream> _packets = null;
	private Map<Integer, Integer> _types = null;
	private Map<Integer, PESPacketStream> _pesPackets = null;
	
	public long pts = -1;
	public long dts = -1;
	
	public M2TSParser()
	{
		_types = new HashMap<Integer, Integer>();
		_packets = new HashMap<Integer, PacketStream>();
		_pesPackets = new HashMap<Integer, PESPacketStream>();
		clear();
	}
	
	public void flush()
	{
		ByteArray tmp = new ByteArray(PacketStream.MAX_PACKET_SIZE);
		
		for (int i = 0; i < _packets.size(); ++i)
		{
			_packets.get(i).appendBytes(tmp, 0, 0, true, -1, true, this);
		}
		
		for (int i = 0; i < _pesPackets.size(); ++i)
		{
			parsePESPacketStreamComplete(_pesPackets.get(i), i, true);
		}
		
		clear();
	}
	
	public void clear()
	{
		_buffer = null;
		_packets.clear();
		_pesPackets.clear();
	}
	
	public void reset()
	{
		clear();
		_types.clear();
	}
	
	public void appendBytes(ByteArray bytes, int offset, int count)
	{
		if (_buffer == null)
		{
			_buffer = new ByteArray(0);
		}
		_buffer.write(bytes, offset, count);
		
		int cursor = 0;
		int len = _buffer.length();
		
		while (true)
		{
			int scanCount = 0;
			while (cursor + 187 < len)
			{
				if (0x47 == _buffer.array[cursor]) // search for TS synch byte
					break;
				
				++cursor;
				++scanCount;
			}
			
			if (cursor + 188 > len)
				break;
			
			parseTSPacket(cursor);
			if (pts >= 0)
				break; // we're done
			
			cursor += 188;
		}
		
		// Snarf remainder into beginning of buffer
		int remainder = _buffer.length() - cursor;
		_buffer.toBeginning();
		_buffer.write(_buffer, cursor, remainder);
		_buffer.length(remainder);
	}
	
	
	private void parseTSPacket(int cursor)
	{
		boolean payloadStart = false;
		int packetID = 0;
		boolean hasAdaptationField = false;
		boolean hasPayload = false;
		int continuityCounter = 0;
		int headerLength = 4;
		int payloadLength = 0;
		boolean discontinuity = false;
		
		// Decode header bytes.
		payloadStart 		= (_buffer.array[cursor + 1] & 0x40) != 0;
		packetID			= ((_buffer.unsigned(cursor + 1) & 0x1f) << 8) + _buffer.unsigned(cursor + 2);
		continuityCounter	= _buffer.unsigned(cursor + 3) & 0x0f;
		hasPayload			= (_buffer.array[cursor + 3] & 0x10) != 0;
		hasAdaptationField	= (_buffer.array[cursor + 3] & 0x20) != 0;
		
		// Set up rest of parsing.
		if (hasAdaptationField)
		{
			int adaptationFieldLength = _buffer.unsigned(cursor + 4);
			if (adaptationFieldLength > 183)
				return; // invalid
			
			headerLength += adaptationFieldLength + 1;
			
			discontinuity = (_buffer.unsigned(cursor + 5) & 0x80) != 0;
		}
		
		payloadLength = 188 - headerLength;
		
		if (!hasPayload)
			return;
		
		switch (packetID)
		{
		case 0x1fff:
			break;
		default:
			parseTSPayload(packetID, payloadStart, continuityCounter, discontinuity, cursor + headerLength, payloadLength);
		}
	}
	
	private void parseTSPayload(int packetID, boolean payloadStart, int continuityCounter, boolean discontinuity, int cursor, int length )
	{
		PacketStream stream = null;
		stream = _packets.get(packetID);
		if (stream == null)
		{
			stream = new PacketStream(packetID);
			_packets.put(packetID, stream);
		}
		
		stream.appendBytes(_buffer, cursor, length, payloadStart, continuityCounter, discontinuity, this);		
	}
	
	
	private void parsePESPacketStreamComplete(PESPacketStream pes, int packetID, boolean flushing)
	{
		pts = pes._pts;
		dts = pes._dts;
	}
	
	private boolean parsePESPacket(int packetID, int type, ByteArray bytes)
	{
		int streamID = bytes.unsigned(3);
		int packetLength = (bytes.unsigned(4) << 8) + bytes.unsigned(5);
		int cursor = 6;

		PESPacketStream pes = null;
		
		switch (streamID)
		{
		case 0xbc: // program stream map
		case 0xbe: // padding stream
		case 0xbf: // private_stream_2
		case 0xf0: // ECM_stream
		case 0xf1: // EMM_stream
		case 0xff: // program_stream_directory
		case 0xf2: // DSMCC stream
		case 0xf8: // H.222.1 type E
			// onOtherPacket
			return false;
		default:
			break;
		}
		
		if (packetLength != 0)
		{
			if (bytes.length() < packetLength)
			{
				return false; // not enough bytes in packet
			}
		}
		
		if (bytes.length() < 9)
		{
			// Too short
			return false;
		}
		
		boolean dataAlignment = (bytes.array[cursor] & 0x04) != 0;
		++cursor;
		
		int ptsDts = (bytes.array[cursor] & 0xc0) >> 6;
		++cursor;
		
		int pesHeaderDataLength = bytes.array[cursor];
		++cursor;
		
		if ((ptsDts & 0x02) != 0)
		{
			// has PTS at least
			if (cursor + 5 > bytes.length())
				return false;
			
			Log.i("M2TSParser.parsePESPacket", bytes.toString(cursor, 5));

			
			pts = bytes.unsigned(cursor) & 0x0e;
			pts *= 128;
			pts += bytes.unsigned(cursor + 1);
			pts *= 256;
			pts += bytes.unsigned(cursor + 2) & 0xfe;
			pts *= 128;
			pts += bytes.unsigned(cursor + 3);
			pts *= 256;
			pts += bytes.unsigned(cursor + 4) & 0xfe;
			pts /= 2;

		
			if ((ptsDts & 0x01) != 0)
			{
				Log.i("M2TSParser.parsePESPacket", bytes.toString(cursor + 5, 5));
				dts  = bytes.array[cursor + 5] & 0x0e;
				dts *= 128;
				dts += bytes.array[cursor + 6];
				dts *= 256;
				dts += bytes.array[cursor + 7] & 0xfe;
				dts *= 128;
				dts += bytes.array[cursor + 8];
				dts *= 256;
				dts += bytes.array[cursor + 9] & 0xfe;
				dts /= 2;
			}
			else
				dts = pts;
		}
		
		if (pts >= 0)
		{
			return true;
		}
		return false;
	}
	
	
	

	@Override
	public void onComplete(int packetID, ByteArray bytes)
	{
		if (bytes.length() < 3)
			return;
		
		if ( (bytes.array[0] == 0x00)
			&& (bytes.array[1] == 0x00)
			&& (bytes.array[2] == 0x01) )
		{
			
			parsePESPacket(packetID, 0, bytes);
			return;
		}
		
		int cursor = ((int)(bytes.array[0] & 0xFF)) + 1;
		int remaining = 0;
		
		if (cursor > bytes.length())
			return;
		
		remaining = bytes.length() - cursor;
		
		if ( (remaining < 23)
			|| (bytes.array[cursor] != 0x02)
			|| ((bytes.array[cursor + 1] & 0xfc)) != 0xb0)
		{
			// different packet type
		}
		
	}

	@Override
	public boolean onProgress(int packetID, ByteArray bytes)
	{
		if (bytes.length() < 3)
			return false;
		
		// Skip PES
		if (bytes.array[0] == 0x00 && bytes.array[1] == 0x00 && bytes.array[2] == 0x01)
			return false;
		
		int cursor = ((int)(bytes.array[0] & 0xFF)) + 1;
		int remaining;
		
		if (cursor > bytes.length())
			return false;
		
		remaining = bytes.length() - cursor;
		
		if ( (remaining < 23)
				&& (bytes.array[cursor] == 0x02)
				&& ((bytes.array[cursor + 1] & 0xfc)) == 0xb0)
			{
				return true; // ??? -- we're not actually caring about these.
			}
		return false;
	}
}
