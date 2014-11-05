package com.graceplayer.model;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import com.graceplayer.graceplayer.R;

import android.content.Context;

public class PropertyBean {
	public static String[] THEMES;
	private static String DEFAULT_THEME;
	// Ӧ��������
	private Context context;
	// ����
	private String theme;
	public PropertyBean(Context context) {
		this.context = context;
		// ��ȡarray.xml�е���������
		THEMES = context.getResources().getStringArray(R.array.theme);
		DEFAULT_THEME = THEMES[0];
		loadTheme();
	}
	/** ��ȡ���⡣�������ļ�"configuration.cfg"�� */
	private void loadTheme() {
		Properties properties = new Properties();
		try {
			FileInputStream stream = context.openFileInput("configuration.cfg");
			properties.load(stream);
			theme = properties.getProperty("theme").toString();
		} catch (Exception e) {
			saveTheme(DEFAULT_THEME); // Ĭ��ֵ
		}
	}
	
	/** �������⡣�������ļ�"configuration.cfg"�� */
	private boolean saveTheme(String theme) {
		Properties properties = new Properties();
		properties.put("theme", theme);
		try {
			FileOutputStream stream = context.openFileOutput(
					"configuration.cfg", Context.MODE_PRIVATE);
			properties.store(stream, "");
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public String getTheme() {
		return theme;
	}

	public void setAndSaveTheme(String theme) {
		this.theme = theme;
		saveTheme(theme);
	}
}
