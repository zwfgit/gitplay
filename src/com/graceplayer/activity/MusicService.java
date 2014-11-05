package com.graceplayer.activity;

import java.util.ArrayList;
import java.util.Random;

import com.graceplayer.activity.MainActivity;
import com.graceplayer.data.Music;
import com.graceplayer.data.MusicList;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.IBinder;
import android.widget.Toast;

public class MusicService extends Service {
	// ���ſ��������ʶ����
	public static final int COMMAND_UNKNOWN = -1;
	public static final int COMMAND_PLAY = 0;
	public static final int COMMAND_PAUSE = 1;
	public static final int COMMAND_STOP = 2;
	public static final int COMMAND_RESUME = 3;
	public static final int COMMAND_PREVIOUS = 4;
	public static final int COMMAND_NEXT = 5;
	public static final int COMMAND_CHECK_IS_PLAYING = 6;
	public static final int COMMAND_SEEK_TO = 7;
	// ������״̬
	public static final int STATUS_PLAYING = 0;
	public static final int STATUS_PAUSED = 1;
	public static final int STATUS_STOPPED = 2;
	public static final int STATUS_COMPLETED = 3;
	// �㲥��ʶ
	public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTION_CONTROL";
	public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS = "MusicService.ACTION_UPDATE";
	// �㲥������
	private CommandReceiver receiver;
	private int status;
	// ý�岥����
	private MediaPlayer player = new MediaPlayer();
	//������ţ���0��ʼ
	private int number = 0;
	Random randoms = new Random();
	private ArrayList<Music> musicLists;
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// �󶨹㲥�����������Խ��չ㲥
		bindCommandReceiver();
		status = MusicService.STATUS_STOPPED;
	}

	@Override
	public void onDestroy() {
		// �ͷŲ�������Դ
		if (player != null) {
			player.release();
		}
		super.onDestroy();
	}

	/** �󶨹㲥������ */
	private void bindCommandReceiver() {
		receiver = new CommandReceiver();
		IntentFilter filter = new IntentFilter(BROADCAST_MUSICSERVICE_CONTROL);
		registerReceiver(receiver, filter);
	}

	/** �ڲ��࣬���չ㲥�����ִ�в��� */
	class CommandReceiver extends BroadcastReceiver {
		
		public void onReceive(Context context, Intent intent) {
			// ��ȡ����
			int command = intent.getIntExtra("command", COMMAND_UNKNOWN);
			// ִ������
			switch (command) {
			case COMMAND_SEEK_TO:
				seekTo(intent.getIntExtra("time", 0));
				break;
			case COMMAND_PLAY:
				number = intent.getIntExtra("number", 0);
				play(number);
				break;
			case COMMAND_PREVIOUS:
				if(command==MainActivity.MODE_RANDOM_CYCLE)
				{
					moveRandomNumber();
				}else{
					moveNumberToPrevious();
				}							
				break;
			case COMMAND_NEXT:
				if(command==MainActivity.MODE_RANDOM_CYCLE)
				{
					moveRandomNumber();
				}else{
					moveNumberToNext();
				}				
				break;
			case COMMAND_PAUSE:
				pause();
				break;
			case COMMAND_STOP:
				stop();
				break;
			case COMMAND_RESUME:
				resume();
				break;
			case COMMAND_CHECK_IS_PLAYING:
				if (player != null && player.isPlaying()) {
					sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
				}
				break;
			case COMMAND_UNKNOWN:
			default:
				break;
			}
		}
	}

	/** ���͹㲥������״̬�ı��� */
	private void sendBroadcastOnStatusChanged(int status) {
		Intent intent = new Intent(BROADCAST_MUSICSERVICE_UPDATE_STATUS);
		intent.putExtra("status", status);
		if (status !=STATUS_STOPPED) {
			
				intent.putExtra("time", player.getCurrentPosition());
				intent.putExtra("duration", player.getDuration());
				intent.putExtra("number", number);
				intent.putExtra("musicName", MusicList.getMusicList().get(number).getmusicName());
				intent.putExtra("musicArtist", MusicList.getMusicList().get(number).getmusicArtist());
			
		}
		sendBroadcast(intent);
	}

	/** ��ȡ�����ļ� */
	private void load(int number) {
		try {
			player.reset();
			player.setDataSource(MusicList.getMusicList().get(number).getmusicPath());
			player.prepare();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// ע�������
		player.setOnCompletionListener(completionListener);
	}

	// ���Ž���������
	OnCompletionListener completionListener = new OnCompletionListener() {
		@Override
		public void onCompletion(MediaPlayer player) {
			if (player.isLooping()) {
				replay();
			} else {
				sendBroadcastOnStatusChanged(MusicService.STATUS_COMPLETED);
			}
		}
	};
	
	/** ѡ����һ�� */
	private void moveNumberToNext() {
		// �ж��Ƿ񵽴����б�׶�
		if ((number ) == MusicList.getMusicList().size()-1) {
				Toast.makeText(MusicService.this,"�Ѿ������б�ײ�",Toast.LENGTH_SHORT).show();
		} else {
				++number;
				play(number);
		}
	}

	/** ѡ����һ�� */
	private void moveNumberToPrevious() {
		// �ж��Ƿ񵽴����б���
		if (number == 0) {
			Toast.makeText(MusicService.this,"�Ѿ������б���",Toast.LENGTH_SHORT).show();
		} else {
			--number;
			play(number);
		}
	}
	//�������
	private void moveRandomNumber() {
		// �ж��Ƿ񵽴����б�׶�
		number=randoms.nextInt()%musicLists.size();
		play(number);
	}

	/** �������� */
	private void play(int number) {
		// ֹͣ��ǰ����
		if (player != null && player.isPlaying()) {
			player.stop();
		}
		load(number);
		player.start();
		status = MusicService.STATUS_PLAYING;
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING); 
	}

	/** ��ͣ���� */
	private void pause() {
		if (player.isPlaying()) {
			player.pause();
			status = MusicService.STATUS_PAUSED;
			sendBroadcastOnStatusChanged(MusicService.STATUS_PAUSED);
		}
	}

	/** ֹͣ���� */
	private void stop() {
		if (status != MusicService.STATUS_STOPPED) {
			player.stop();
			sendBroadcastOnStatusChanged(MusicService.STATUS_STOPPED);
		}
	}

	/** �ָ����ţ���֮ͣ�� */
	private void resume() {
		player.start();
		status = MusicService.STATUS_PLAYING;
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}

	/** ���²��ţ��������֮�� */
	private void replay() {
		player.start();
		status = MusicService.STATUS_PLAYING;
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}

	/** ��ת������λ�� */
	private void seekTo(int time) {
			player.seekTo(time);
			status = MusicService.STATUS_PLAYING;
			sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}
}