package com.kaltura.playersdk;

import org.json.JSONException;
import org.json.JSONObject;

public class QualityTrack {
	public String assetId;
	public int originalIndex;
	public int bandwidth;
	public int height;
	public int width;
	public String type;

	public QualityTrack(){};

	public QualityTrack(String assetId, int originalIndex, int bandwidth, int height, int width, String type) {
		this.assetId = assetId;
		this.originalIndex = originalIndex;
		this.bandwidth = bandwidth;
		this.type = type;
		this.height = height;
		this.width = width;

	}

	public JSONObject toJSONObject() {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("assetid", assetId);
			jsonObject.put("originalIndex", originalIndex);
			jsonObject.put("bandwidth", bandwidth);
			jsonObject.put("type", type);
			jsonObject.put("height", height);
			jsonObject.put("width", width);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		return jsonObject;
	}
}
