package com.graceplayer.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.graceplayer.data.Music;
import com.graceplayer.data.MusicList;
import com.graceplayer.graceplayer.R;
import com.graceplayer.model.PropertyBean;

public class MainActivity extends Activity {
	
	// ���������Ƴ���
	private static final int PROGRESS_INCREASE = 0;
	private static final int PROGRESS_PAUSE = 1;
	private static final int PROGRESS_RESET = 2;
	
	//����ģʽ����
	private static final int MODE_LIST_SEQUENCE= 0;//˳�򲥷�
	private static final int MODE_SINGLE_CYCLE = 1;//����ѭ��
	private static final int MODE_LIST_CYCLE = 2;//�б�ѭ��
	public static final int MODE_RANDOM_CYCLE = 3;//�������
	private int playmode;
	
	// ��ʾ���
	private TextView tv_current_time;
	private TextView tv_total_time;
	private ImageButton imgBtn_Previous;
	private ImageButton imgBtn_PlayOrPause;
	private ImageButton imgBtn_Stop;
	private ImageButton imgBtn_Next;
	private SeekBar seekBar;
	private ListView listView;
	private RelativeLayout root_Layout;
	private TextView tv_vol;
	private SeekBar seekbar_vol;

	// ��ǰ�����ĳ���ʱ��͵�ǰλ�ã������ڽ�����
	private int total_time;
	private int curent_time;
	// ��ǰ��������ţ��±��0��ʼ
	private int number;
	// ����״̬
	private int status;
	// �㲥������
	private StatusChangedReceiver receiver;
	// ���½�������Handler
	private Handler seekBarHandler;
	
	//�����б����
	private ArrayList<Music> musicArrayList;
	Random random = new Random();
	//�˳��жϱ��
		private static Boolean isExit = false;

    //˯��ģʽ����������ʶ����
	private ImageView iv_sleep;
	private Timer timer_sleep ;
	private static final boolean NOTSLEEP = false;
	private static final boolean ISSLEEP = true;
	//Ĭ�ϵ�˯��ʱ��
	private int sleepminute = 20;
	//����Ƿ��˯��ģʽ
	private static boolean sleepmode;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		findViews();
		registerListeners();
		initMusicList();
		initListView();
		checkMusicfile();
		
		startService(new Intent(this, MusicService.class));
		// �󶨹㲥�����������Խ��չ㲥
		bindStatusChangedReceiver();
		sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
		// ��ʼ����������Handler
		initSeekBarHandler();
		status = MusicService.COMMAND_STOP;
		
		//Ĭ�ϲ���ģʽ��˳�򲥷�
		playmode = MainActivity.MODE_LIST_SEQUENCE;
		sleepmode = MainActivity.NOTSLEEP;
	}

	void findViews() {
		listView = (ListView) findViewById(R.id.main_listview);
		tv_current_time = (TextView) findViewById(R.id.main_tv_curtime);
		tv_total_time = (TextView) findViewById(R.id.main_tv_totaltime);
		imgBtn_Previous = (ImageButton) findViewById(R.id.main_ibtn_pre);
		imgBtn_PlayOrPause = (ImageButton) findViewById(R.id.main_ibtn_play);
		imgBtn_Previous = (ImageButton) findViewById(R.id.main_ibtn_pre);
		imgBtn_Next = (ImageButton) findViewById(R.id.main_ibtn_next);
		imgBtn_Stop = (ImageButton) findViewById(R.id.main_ibtn_stop);
		seekBar = (SeekBar) findViewById(R.id.main_seekBar);
		root_Layout = (RelativeLayout) findViewById(R.id.relativeLayout1);
		tv_vol=(TextView)findViewById(R.id.main_tv_volumeText);
		seekbar_vol=(SeekBar)findViewById(R.id.main_sb_volumebar);
		iv_sleep=(ImageView)findViewById(R.id.main_iv_sleep);
	}

	/** Ϊ��ʾ���ע������� */
	private void registerListeners() {
		imgBtn_Previous.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);
			}
		});
		imgBtn_PlayOrPause.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				switch (status) {
				case MusicService.STATUS_PLAYING:
					sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
					break;
				case MusicService.STATUS_PAUSED:
					sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
					break;
				case MusicService.COMMAND_STOP:
					sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
				default:
					break;
				}
			}
		});
		imgBtn_Stop.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				sendBroadcastOnCommand(MusicService.COMMAND_STOP);
			}
		});
		imgBtn_Next.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
			}
		});
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				number = position;
				sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
			}
		});
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if (status == MusicService.STATUS_PLAYING) {
					// ���͹㲥��MusicService��ִ����ת
					sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
					// �������ָ��ƶ�
					seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE,
							1000);
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// ��������ͣ�ƶ�
				seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
			}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (status != MusicService.STATUS_STOPPED) {
					curent_time = progress;
					// �����ı�
					tv_current_time.setText(formatTime(curent_time));
				}
			}
		});
	}
	/**��ʼ�������б����*/
	private void initMusicList() {
		musicArrayList = MusicList.getMusicList();
		//�����ظ��������
		if(musicArrayList.isEmpty())
		{
			Cursor mMusicCursor = this.getContentResolver().query(
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
					MediaStore.Audio.AudioColumns.TITLE);
			int indexTitle = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE);
			int indexArtist = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
			int indexTotalTime = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION);
			int indexPath = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);

			/**ͨ��mMusicCursor�α�������ݿ⣬����Music�������ش�ArrayList��*/
			for (mMusicCursor.moveToFirst(); !mMusicCursor.isAfterLast(); mMusicCursor
					.moveToNext()) { 
				String strTitle = mMusicCursor.getString(indexTitle);
				String strArtist = mMusicCursor.getString(indexArtist);
				String strTotoalTime = mMusicCursor.getString(indexTotalTime);
				String strPath = mMusicCursor.getString(indexPath);

				if (strArtist.equals("<unknown>"))
					strArtist = "��������";
				Music music = new Music(strTitle, strArtist, strPath, strTotoalTime);
				musicArrayList.add(music);
			}
		}
	}
	/**��������������ʼ��listView*/
	private void initListView() {
		List<Map<String, String>> list_map = new ArrayList<Map<String, String>>();
		HashMap<String, String> map;
		SimpleAdapter simpleAdapter;
		for (Music music : musicArrayList) {
			map = new HashMap<String, String>();
			map.put("musicName", music.getmusicName());
			map.put("musicArtist", music.getmusicArtist());
			list_map.add(map);
		}

		String[] from = new String[] { "musicName", "musicArtist" };
		int[] to = { R.id.listview_tv_title_item, R.id.listview_tv_artist_item };

		simpleAdapter = new SimpleAdapter(this, list_map, R.layout.listview,from, to);
		listView.setAdapter(simpleAdapter);
	}
	
	/**����б�û�и������򲥷Ű�ť�����ã��������û�*/
	private void checkMusicfile()
	{
		if (musicArrayList.isEmpty()) {
			imgBtn_Next.setEnabled(false);
			imgBtn_PlayOrPause.setEnabled(false);
			imgBtn_Previous.setEnabled(false);
			imgBtn_Stop.setEnabled(false);
			Toast.makeText(getApplicationContext(), "��ǰû�и����ļ�",Toast.LENGTH_SHORT).show();
		} else {
			imgBtn_Next.setEnabled(true);
			imgBtn_PlayOrPause.setEnabled(true);
			imgBtn_Previous.setEnabled(true);
			imgBtn_Stop.setEnabled(true);
		}
	}
	/** �󶨹㲥������ */
	private void bindStatusChangedReceiver() {
		receiver = new StatusChangedReceiver();
		IntentFilter filter = new IntentFilter(
				MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
		registerReceiver(receiver, filter);
	}

	private void initSeekBarHandler() {
		seekBarHandler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);

				switch (msg.what) {
				case PROGRESS_INCREASE:
					if (seekBar.getProgress() < total_time) {
						// ������ǰ��1��
						seekBar.incrementProgressBy(1000);
						seekBarHandler.sendEmptyMessageDelayed(
								PROGRESS_INCREASE, 1000);
						// �޸���ʾ��ǰ���ȵ��ı�
						tv_current_time.setText(formatTime(curent_time));
						curent_time += 1000;
					}
					break;
				case PROGRESS_PAUSE:
					seekBarHandler.removeMessages(PROGRESS_INCREASE);
					break;
				case PROGRESS_RESET:
					// ���ý���������
					seekBarHandler.removeMessages(PROGRESS_INCREASE);
					seekBar.setProgress(0);
					tv_current_time.setText("00:00");
					break;
				}
			}
		};
	}
	/** ��������������ֲ��š�����������MusicService���� */
	private void sendBroadcastOnCommand(int command) {

		Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
		intent.putExtra("command", command);
		// ���ݲ�ͬ�����װ��ͬ������
		switch (command) {
		case MusicService.COMMAND_PLAY:
			intent.putExtra("number", number);
			break;
		case MusicService.COMMAND_SEEK_TO:
			intent.putExtra("time", curent_time);
			break;
		case MusicService.COMMAND_PREVIOUS:
		case MusicService.COMMAND_NEXT:
		case MusicService.COMMAND_PAUSE:
		case MusicService.COMMAND_STOP:
		case MusicService.COMMAND_RESUME:
		default:
			break;
		}
		sendBroadcast(intent);
	}

	/** �ڲ��࣬���ڲ�����״̬���µĽ��չ㲥 */
	class StatusChangedReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			// ��ȡ������״̬
			status = intent.getIntExtra("status", -1);
			switch (status) {
			case MusicService.STATUS_PLAYING:
				String musicName = intent.getStringExtra("musicName");
				String musicArtist = intent.getStringExtra("musicArtist");

				seekBarHandler.removeMessages(PROGRESS_INCREASE);
				curent_time = intent.getIntExtra("time", 0);
				total_time = intent.getIntExtra("duration", 0);
				number = intent.getIntExtra("number", number);
				listView.setSelection(number);

				seekBar.setProgress(curent_time);
				seekBar.setMax(total_time);
				seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);

				tv_total_time.setText(formatTime(total_time));
				imgBtn_PlayOrPause.setBackgroundResource(R.drawable.pause);
				
				// ����Activity�ı��������֣���ʾ���ڲ��ŵĸ���
				MainActivity.this.setTitle("���ڲ���:" + musicName + " "+ musicArtist);
				break;
			case MusicService.STATUS_PAUSED:
				seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
				String string = MainActivity.this.getTitle().toString().replace("���ڲ���:", "����ͣ:");
				MainActivity.this.setTitle(string);
				imgBtn_PlayOrPause.setBackgroundResource(R.drawable.play);
				break;
			case MusicService.STATUS_STOPPED:
				curent_time = 0;
				total_time = 0;
				tv_current_time.setText(formatTime(curent_time));
				tv_total_time.setText(formatTime(total_time));
				seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
				MainActivity.this.setTitle("GracePlayer");
				imgBtn_PlayOrPause.setBackgroundResource(R.drawable.play);
				break;
			case MusicService.STATUS_COMPLETED:
				number = intent.getIntExtra("number", 0);
				if(playmode == MainActivity.MODE_LIST_SEQUENCE)										//˳��ģʽ�������б�ĩ��ʱ����ֹͣ������򲥷���һ��
				{
					if(number == MusicList.getMusicList().size()-1) 											
						sendBroadcastOnCommand(MusicService.STATUS_STOPPED);
					else
						sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
				}
				else if(playmode == MainActivity.MODE_SINGLE_CYCLE)								//����ѭ��
					sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
				else if(playmode == MainActivity.MODE_LIST_CYCLE)										//�б�ѭ���������б�ĩ��ʱ����Ҫ���ŵ���������Ϊ��һ�ף�
				{																															//					Ȼ���Ͳ������			
					if(number == musicArrayList.size()-1)
					{
						number = 0;
						sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
					}
					else sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
				}
				else if(playmode == MainActivity.MODE_RANDOM_CYCLE)										//
				{	
					
				    sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
				}
				seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
				MainActivity.this.setTitle("GracePlayer");
				imgBtn_PlayOrPause.setBackgroundResource(R.drawable.play);
				break;
			default:
				break;
			}
		}
	}

	/** ��ʽ�������� -> "mm:ss" */
	private String formatTime(int msec) {
		int minute = (msec / 1000) / 60;
		int second = (msec / 1000) % 60;
		String minuteString;
		String secondString;
		if (minute < 10) {
			minuteString = "0" + minute;
		} else {
			minuteString = "" + minute;
		}
		if (second < 10) {
			secondString = "0" + second;
		} else {
			secondString = "" + second;
		}
		return minuteString + ":" + secondString;
	}

	/** ����Activity�����⣬�����޸ı���ͼƬ�� */
	private void setTheme(String theme) {
		if ("��ɫ".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_color);
		} else if ("����".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_digit_flower);
		} else if ("Ⱥɽ".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_mountain);
		} else if ("С��".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_running_dog);
		} else if ("��ѩ".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_snow);
		} else if ("Ů��".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_music_girl);
		} else if ("����".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_blur);
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		// ��鲥�����Ƿ����ڲ��š�������ڲ��ţ����ϰ󶨵Ľ�������ı�UI
		sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
		PropertyBean property = new PropertyBean(MainActivity.this);
		String theme = property.getTheme();
		// ����Activity������
		setTheme(theme);
		audio_Control();
		//˯��ģʽ������ʾͼ�꣬�ر�ʱ����ͼ��
		if(sleepmode == MainActivity.ISSLEEP) iv_sleep.setVisibility(View.VISIBLE);
		else iv_sleep.setVisibility(View.INVISIBLE);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (status == MusicService.STATUS_STOPPED) {
			stopService(new Intent(this, MusicService.class));
		}
		super.onDestroy();
	}

	/** �����˵� */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/** ����˵�����¼� */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_theme:
			// ��ʾ�б�Ի���
			new AlertDialog.Builder(this)
					.setTitle("��ѡ������")
					.setItems(R.array.theme,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// ��ȡ��array.xml�ж������������
									String theme = PropertyBean.THEMES[which];
									// ����Activity������
									setTheme(theme);
									// ����ѡ�������
									PropertyBean property = new PropertyBean(
											MainActivity.this);
									property.setAndSaveTheme(theme);
								}
							}).show();
			break;
		case R.id.menu_about:
			// ��ʾ�ı��Ի���
			new AlertDialog.Builder(MainActivity.this).setTitle("GracePlayer")
					.setMessage(R.string.about).show();
			break;
		case R.id.menu_quit:
			//�˳�����
			new AlertDialog.Builder(MainActivity.this).setTitle("��ʾ")
			.setMessage(R.string.quit_message).setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
					System.exit(0);
				}
			}).setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
					
				}
			}).show();
			break;
		case R.id.menu_playmode:
			String[] mode = new String[] { "˳�򲥷�", "����ѭ��", "�б�ѭ��","�������" };
			AlertDialog.Builder builder = new AlertDialog.Builder(
					MainActivity.this);
			builder.setTitle("����ģʽ");
			builder.setSingleChoiceItems(mode, playmode,						//���õ�ѡ�����ڶ���������Ĭ��ѡ�����ţ��������playmode��ֵ��ȷ��
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							// TODO Auto-generated method stub
							playmode = arg1;
						}
					});
			builder.setPositiveButton("ȷ��",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							// TODO Auto-generated method stub
							switch (playmode) {
							case 0:
								playmode = MainActivity.MODE_LIST_SEQUENCE;
								Toast.makeText(getApplicationContext(), R.string.sequence, Toast.LENGTH_SHORT).show();
								break;
							case 1:
								playmode = MainActivity.MODE_SINGLE_CYCLE;
								Toast.makeText(getApplicationContext(), R.string.singlecycle, Toast.LENGTH_SHORT).show();
								break;
							case 2:
								playmode = MainActivity.MODE_LIST_CYCLE;
								Toast.makeText(getApplicationContext(), R.string.listcycle, Toast.LENGTH_SHORT).show();
								break;
							case 3:
								playmode = MainActivity.MODE_RANDOM_CYCLE;
								Toast.makeText(getApplicationContext(), R.string.randomcycle, Toast.LENGTH_SHORT).show();
								break;
							default:
								break;
							}
						}
					});
			builder.create().show(); 
			break;
		case R.id.menu_sleep:
			showSleepDialog();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	//��дonkeyDown����
		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
			int progress;
			switch(keyCode)
			{
			case KeyEvent.KEYCODE_BACK:
				exitByDoubleClick();
				break;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				progress = seekbar_vol.getProgress();
				if(progress != 0)
					seekbar_vol.setProgress(progress-1);
				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:
				progress = seekbar_vol.getProgress();
				if(progress != seekbar_vol.getMax())
					seekbar_vol.setProgress(progress+1);
				return true;
			default:
					break;
			}
			return false;
		}

		private void exitByDoubleClick()
		{
			Timer timer = null;
			if(isExit == false)
			{
				isExit = true;		//׼���˳�
				Toast.makeText(this, "�ٰ�һ���˳�����", Toast.LENGTH_SHORT).show();
				timer = new Timer();
				timer.schedule(new TimerTask() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						isExit = false;
					}
				}, 2000);   //2 ����ִ�� run���������ݣ����2����û�а��·��ؼ�����������ʱ���޸�isExit��ֵ
			}
			else
			{
				System.exit(0);
			}
		}
		private void audio_Control()
		{
			//��ȡ����������
				final AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
				//���õ�ǰ����������Сֻ�����ý������
				this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
				//���û��������ֵ
				final int max_progress = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
				seekbar_vol.setMax(max_progress); 
				//��ȡ��ǰ����
				int progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
				seekbar_vol.setProgress(progress);
				
				tv_vol .setText("������ "+(progress*100/max_progress)+"%"); 
				
				seekbar_vol.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onStopTrackingTouch(SeekBar arg0) {
						// TODO Auto-generated method stub
					}
					@Override
					public void onStartTrackingTouch(SeekBar arg0) {
						// TODO Auto-generated method stub
					}
					@Override
					public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
						// TODO Auto-generated method stub
						tv_vol .setText("������ "+(arg1*100)/(max_progress)+"%");
						audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, AudioManager.FLAG_PLAY_SOUND);
					}
				});
		}
			
		private void showSleepDialog()   
		{
			//����getLayoutInflater().inflate������ȡ���֣�������ʼ��һ��View�����
			final View userview = this.getLayoutInflater().inflate(R.layout.dialog, null);
			
		    //ͨ��View���findViewById������ȡ���������
			final TextView tv_minute = (TextView)userview.findViewById(R.id.dialog_tv);
			final Switch switch1 = (Switch)userview.findViewById(R.id.dialog_switch);
			final SeekBar seekbar = (SeekBar)userview.findViewById(R.id.dialog_seekbar);
			
			tv_minute.setText("˯����:"+sleepminute+"����");
			//���ݵ�ǰ��˯��״̬��ȷ��Switch��״̬
			if(sleepmode == MainActivity.ISSLEEP) switch1.setChecked(true);
			seekbar.setMax(60);
			seekbar.setProgress(sleepminute);
			seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				
				@Override
				public void onStopTrackingTouch(SeekBar arg0) {
					// TODO Auto-generated method stub
				
				}
				@Override
				public void onStartTrackingTouch(SeekBar arg0) {
					// TODO Auto-generated method stub
				}
				@Override
				public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
					// TODO Auto-generated method stub
					sleepminute = arg1;
					tv_minute.setText("˯����:"+sleepminute+"����");
					
				}
			});
			switch1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
					// TODO �Զ����ɵķ������
					sleepmode = arg1;
				}
			});
			//���嶨ʱ������
			final TimerTask timerTask = new TimerTask() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					System.exit(0);
				}
			};
			//����Ի����Լ���ʼ��
			final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle("ѡ��˯��ʱ��(0~60����)");
			//���ò���
			dialog.setView(userview);
			//����ȡ����ť��Ӧ�¼�
			dialog.setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
					arg0.dismiss();
				}
			});
			//�������ð�ť��Ӧʱ��
			dialog.setNeutralButton("����", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
					sleepmode = MainActivity.NOTSLEEP;
					sleepminute = 20;
					timerTask.cancel();
					timer_sleep.cancel();
					iv_sleep.setVisibility(View.INVISIBLE);
				}
			});
			//����ȷ����ť��Ӧ�¼�
			dialog.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
					if(sleepmode == MainActivity.ISSLEEP)
					{
						timer_sleep = new Timer();
						int time =seekbar.getProgress();
						//��������time*60*1000�����ִ��
						timer_sleep.schedule(timerTask, time*60*1000);
						iv_sleep.setVisibility(View.VISIBLE);
					}
					else
					{
						//ȡ������
						timerTask.cancel();
						timer_sleep.cancel();
						arg0.dismiss();
						iv_sleep.setVisibility(View.INVISIBLE);
					}
				}
			});
			
			dialog.show();
		}
	
}
