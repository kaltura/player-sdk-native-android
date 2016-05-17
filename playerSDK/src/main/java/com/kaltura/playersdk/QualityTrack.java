package com.kaltura.playersdk;

import org.json.JSONException;
import org.json.JSONObject;

public class QualityTrack {
	public String assetId;
	public int bandwidth;
	public int height;
	public int width;
	public String type;

	public QualityTrack(){};

	public QualityTrack(String assetId, int bandwidth, int height, int width, String type) {
		this.assetId = assetId;
		this.bandwidth = bandwidth;
		this.type = type;
		this.height = height;
		this.width = width;

	}

	public String getAssetId() {
		return assetId;
	}

	public void setAssetId(String assetId) {
		this.assetId = assetId;
	}

	public int getBandwidth() {
		return bandwidth;
	}

	public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public String toJSONString() {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("assetId", getAssetId());
			jsonObject.put("bandwidth", getBandwidth());
			jsonObject.put("type", getType());
			jsonObject.put("height", getHeight());
			jsonObject.put("width", getWidth());

			return jsonObject.toString();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}

	public JSONObject toJSONObject() {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("assetId", getAssetId());
			jsonObject.put("bandwidth", getBandwidth());
			jsonObject.put("type", getType());
			jsonObject.put("height", getHeight());
			//jsonObject.put("width", getWidth());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return jsonObject;
	}
}
