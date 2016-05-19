package com.kaltura.playersdk;

import org.json.JSONException;
import org.json.JSONObject;

public class LanguageTrack {
	
	public int index;
	public String kind;
	public String label;
	public String language;
	public String srcLang;
	public String title;

	public LanguageTrack(){}

	public LanguageTrack(int index, String kind, String label, String language, String srcLang, String title) {
		this.index = index;
		this.kind = kind;
		this.label = label;
		this.language = language;
		this.srcLang = srcLang;
		this.title = title;
	}

	public JSONObject toJSONObject() {
		JSONObject jsonObject = new JSONObject();
		try {
			// need id???
			// need mode???
			jsonObject.put("index", index);
			jsonObject.put("kind", kind);
			jsonObject.put("label", label);
			jsonObject.put("language", language);
			jsonObject.put("srclang", srcLang);
			jsonObject.put("title", title);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		return jsonObject;
	}
}
