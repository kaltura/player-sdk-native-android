package com.kaltura.hlsplayersdk.manifest;



import android.util.Log;

public class ManifestPlaylist extends BaseManifestItem {
	public String groupId = "";
	public String language = "";
	public String name = "";
	public boolean autoSelect = false;
	public boolean isDefault = false;
	
	public static ManifestPlaylist fromString(String input)
	{
		ManifestPlaylist result = new ManifestPlaylist();
		result.parseKeyPairs(input);
		return result;
	}
	
	private void parseKeyPairs(String input)
	{
		int firstCommaIndex = input.indexOf(',');
		int firstEqualSignIndex = input.indexOf('=');
		int firstQuoteIndex = input.indexOf('"');
		int endIndex = firstCommaIndex > -1 ? firstCommaIndex : input.length() + 1;
		
		String key = input.substring( 0, firstEqualSignIndex ).trim();
		String value;
		
		if ( firstEqualSignIndex == -1 )
		{
			Log.w("ManifestPlaylist.parseKeyPairs", "ENCOUNTERED BAD KEY PAIR IN '" + input + "', IGNORING." );
			return;
		}
		else if ( firstQuoteIndex == -1 || ( firstCommaIndex > -1 && firstQuoteIndex > firstCommaIndex ) )
		{
			value = input.substring( firstEqualSignIndex + 1, endIndex );
		}
		else
		{
			int secondQuoteIndex = input.indexOf( '"', firstQuoteIndex + 1 );
			int endCommaIndex = input.indexOf( ',', secondQuoteIndex );
			value = input.substring( firstQuoteIndex + 1, secondQuoteIndex );
			endIndex = endCommaIndex > -1 ? endCommaIndex : input.length();
		}
		
		setProperty( key, value );
		
		if (endIndex + 1 < input.length())
		{
			String newInput = input.substring( endIndex + 1 );
			if ( newInput.length() > 0 ) parseKeyPairs( newInput );
		}
	}
	
	private void setProperty(String propertyName, String value)
	{
		if (propertyName == null) return;
		if (propertyName.equals("TYPE"))
		{
			this.type = value;
		}
		else if (propertyName.equals("GROUP-ID"))
		{
			this.groupId = value;
		}
		else if (propertyName.equals("LANGUAGE"))
		{
			this.language = value;
		}
		else if (propertyName.equals("NAME"))
		{
			this.name = value;
			if (this.language.length() == 0) this.language = value;
		}
		else if (propertyName.equals("AUTOSELECT"))
		{
			this.autoSelect = value.equals("YES");
		}
		else if (propertyName.equals("DEFAULT"))
		{
			this.isDefault = value.equals("YES");
		}
		else if (propertyName.equals("URI"))
		{
			this.uri = value;
		}
	}
	
	
}
