package com.graceplayer.data;

import java.util.ArrayList;

/*
 * MusicList�࣬���õ�һʵ����
 * ֻ��ͨ��getMusicList������ȡ
 * ����Ψһ��ArrayList<Music>����
 * */
public class MusicList {

	private static ArrayList<Music> musicarray = new ArrayList<Music>();
	private MusicList(){}
	
	public static ArrayList<Music> getMusicList()
	{
		return musicarray;
	}
}
