package com.kaltura.hlsplayersdk.types;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteArray
{
	public byte [] array = null;
	
	private int _length = 0; // This is the length of our data IN the array - not the length of the array
	public int position = 0;
	
	
	/* ByteArray (int size)
	 * 
	 * Creates an empty array of size bytes
	 * 
	 */
	public ByteArray(int size)
	{
		array = new byte[size];
	}
	
	
	/*
	 *  ByteArray (byte [] bytes)
	 *  
	 *  Does NOT make a copy of the data, but uses the byte array
	 *  directly.
	 *  
	 */
	public ByteArray(byte [] bytes)
	{
		array = bytes;
		if (array != null)
		{
			_length = bytes.length;
			position = _length;
		}
		else
			array = new byte[0];
	}
	
	/*
	 *  ByteArray (ByteArray bytes)
	 *  
	 *  Makes a copy of the data.
	 */
	public ByteArray(ByteArray bytes)
	{
		array = Arrays.copyOf(bytes.array, bytes.length());
		_length = array.length;
		position = _length;
	}
	
	public ByteArray(ByteBuffer bytes)
	{
		array = Arrays.copyOf(bytes.array(), bytes.capacity());
		_length = array.length;
		position = _length;
	}
	
	public short unsigned(int index)
	{
		return (short)(array[index] & 0xff);
	}

	/*
	 * length()
	 * 
	 * The length of the data that we've written so far. May be less than capacity
	 */
	public int length()
	{
		return _length;
	}
	
	/*
	 * length
	 * 
	 * Adjusts the length of the buffer. If length is larger than size(),
	 * the array will be expanded to fit, and the difference filled with 0.
	 * If position > the new length, it will be set at length
	 * 
	 */
	public void length(int length)
	{
		if (length > array.length)
		{
			int len = _length;
			expand(length - array.length);
			Arrays.fill(array, len, array.length - len, (byte)0);
		}
		_length = length;
		if (position > _length) position = length;
	}
	
	/*
	 * size()
	 * 
	 * The total available size of the array.
	 * 
	 */
	public int size()
	{
		return array.length;
	}
	
	public void clear()
	{
		_length = 0;
		position = 0;
	}
	
	public void toEnd()
	{
		position = _length;
	}
	
	public void toBeginning()
	{
		position = 0;
	}
	
	public void write(ByteArray bytes)
	{
		write (bytes.array, 0, bytes.length());
	}
	
	public void write(ByteArray bytes, int offset)
	{
		int count = bytes.length() - offset;
		if (count < 0) count = 0; // don't think a negative count would work
		write (bytes.array, offset, count);
	}
	
	public void write(ByteArray bytes, int offset, int count)
	{
		write (bytes.array, offset, count);
	}

	public void write(byte [] bytes, int offset, int count)
	{
		// Make sure we're not overshooting the size of the bytes buffer
		if (offset + count > bytes.length) count = bytes.length - offset;
		
		if (position + count > array.length)
		{
			expand(position + count - array.length);
		}
		
		System.arraycopy(bytes, offset, array, position, count);
		if (position + count > _length)
			_length = position + count;
		position += count;
	}
	
	
	public void expand(int bySize)
	{
		byte [] oldArray = array;
		array = new byte[oldArray.length + bySize];
		System.arraycopy(oldArray, 0, array, 0, oldArray.length);
		oldArray = null;
	}
	
	public String toString(int start, int length)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Bytes(" + start + "," + length + ") = ");
		for (int i = start; i < array.length && i < start + length; ++i)
		{
			sb.append(Integer.toHexString(array[i] & 0xff));
			if ( i + 1 < array.length && i + 1 < start + length)
				sb.append(",");
		}
		return sb.toString();
	}
	
	
	
	
}
