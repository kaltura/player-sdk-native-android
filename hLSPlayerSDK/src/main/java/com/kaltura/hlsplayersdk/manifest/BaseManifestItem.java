package com.kaltura.hlsplayersdk.manifest;

import com.kaltura.hlsplayersdk.manifest.ManifestParser;

public class BaseManifestItem {
	
	public String type = ManifestParser.DEFAULT;
	public ManifestParser manifest = null;
	public String uri = "";
	
	public BaseManifestItem()
	{
		
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("type : " + type + " | ");
		sb.append("uri : " + uri + "\n");
		if (manifest != null)
		{
			sb.append(manifest.toString());
		}
		else
		{
			sb.append("manfest : null");
		}
		return sb.toString();
	}

}
