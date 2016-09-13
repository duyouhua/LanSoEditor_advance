package com.example.lansongeditordemo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;



import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageFilter;

import com.example.lansong.animview.JumpingBeans;
import com.example.lansong.animview.NumberProgressBar;
import com.example.lansong.animview.OnProgressBarListener;
import com.example.lansong.animview.PaintConstants;
import com.example.lansongeditordemo.view.GLRelativeLayout;
import com.example.lansongeditordemo.view.MediaPoolView;
import com.lansoeditor.demo.R;
import com.lansosdk.box.ViewSprite;
import com.lansosdk.box.ISprite;
import com.lansosdk.box.onMediaPoolSizeChangedListener;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKDir;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Layout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 *  演示: 视频和UI界面的 实时叠加.
 *  
 *  流程: 请参考VViewCommonWidgetActivity.java
 *
 */
public class VViewViewPageDemoActivity extends Activity implements OnSeekBarChangeListener {
    private static final String TAG = "VideoActivity";

    private String mVideoPath;

    private MediaPoolView mPlayView;
    
    private MediaPlayer mplayer=null;
    private MediaPlayer mplayer2=null;
    
    private ISprite  mSpriteMain=null;
    private ViewSprite mViewSprite=null;
    
//    
    private String editTmpPath=null;
    private String dstPath=null;

    private GLRelativeLayout mGLRelativeLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
		 Thread.setDefaultUncaughtExceptionHandler(new snoCrashHandler());
        setContentView(R.layout.vview_viewpage_demo_layout);
        
        
        mVideoPath = getIntent().getStringExtra("videopath");
        mPlayView = (MediaPoolView) findViewById(R.id.id_vview_realtime_mediapool_view);
    
        
        mGLRelativeLayout=(GLRelativeLayout)findViewById(R.id.id_vview_realtime_gllayout);
     
        
        findViewById(R.id.id_vview_realtime_saveplay).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				 if(SDKFileUtils.fileExist(dstPath)){
		   			 	Intent intent=new Intent(VViewViewPageDemoActivity.this,VideoPlayerActivity.class);
			    	    	intent.putExtra("videopath", dstPath);
			    	    	startActivity(intent);
		   		 }else{
		   			 Toast.makeText(VViewViewPageDemoActivity.this, "目标文件不存在", Toast.LENGTH_SHORT).show();
		   		 }
			}
		});
    	findViewById(R.id.id_vview_realtime_saveplay).setVisibility(View.GONE);
    	

        //在手机的/sdcard/lansongBox/路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        editTmpPath=SDKFileUtils.newMp4PathInBox();
        dstPath=SDKFileUtils.newMp4PathInBox();
            
	
	    initViewPager();
	}
    private void startNumberBar()
    {
    	numberBarTimer = new Timer();
        numberBarTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mNumberBar.incrementProgressBy(1);
                    }
                });
            }
        }, 1000, 100);
    }
    private Timer numberBarTimer;
    private NumberProgressBar mNumberBar;
    
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
    	findViewById(R.id.id_vview_realtime_saveplay).setVisibility(View.GONE);
    	new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				 startPlayVideo();
			}
		}, 100);
    }
    private void initSeekBar(int resId,int maxvalue)
    {
    		SeekBar   skbar=(SeekBar)findViewById(resId);
           skbar.setOnSeekBarChangeListener(this);
           skbar.setMax(maxvalue);
    }
    private void startPlayVideo()
    {
          if (mVideoPath != null)
          {
        	  mplayer=new MediaPlayer();
        	  try {
				mplayer.setDataSource(mVideoPath);
				
			}  catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	  mplayer.setOnPreparedListener(new OnPreparedListener() {
				
				@Override
				public void onPrepared(MediaPlayer mp) {
					// TODO Auto-generated method stub
					start(mp);
				}
			});
        	  mplayer.setOnCompletionListener(new OnCompletionListener() {
				
				@Override
				public void onCompletion(MediaPlayer mp) {
					// TODO Auto-generated method stub
					
					Log.i(TAG,"media player is completion!!!!");
					if(mPlayView!=null && mPlayView.isRunning()){
						mPlayView.stopMediaPool();
						
						toastStop();
						
						if(SDKFileUtils.fileExist(editTmpPath)){
							boolean ret=VideoEditor.encoderAddAudio(mVideoPath,editTmpPath,SDKDir.TMP_DIR, dstPath);
							if(!ret){
								dstPath=editTmpPath;
							}else
								SDKFileUtils.deleteFile(editTmpPath);
							
					    	findViewById(R.id.id_vview_realtime_saveplay).setVisibility(View.VISIBLE);
						}
					}
				}
			});
        	  mplayer.prepareAsync();
          }
          else {
              Log.e(TAG, "Null Data Source\n");
              finish();
              return;
          }
    }
    private void start(MediaPlayer mp)
    {
    	MediaInfo info=new MediaInfo(mVideoPath,false);
    	info.prepare();
    	
    	if(DemoCfg.ENCODE){
    		mPlayView.setRealEncodeEnable(480,480,1000000,(int)info.vFrameRate,editTmpPath);
    	}
    	mPlayView.setMediaPoolSize(480,480,new onMediaPoolSizeChangedListener() {
			
			@Override
			public void onSizeChanged(int viewWidth, int viewHeight) {
				// TODO Auto-generated method stub
				mPlayView.startMediaPool(null,null);
				
				mSpriteMain=mPlayView.obtainMainVideoSprite(mplayer.getVideoWidth(),mplayer.getVideoHeight());
				if(mSpriteMain!=null){
					mplayer.setSurface(new Surface(mSpriteMain.getVideoTexture()));
				}
				mplayer.start();
				addViewSprite();
//				startPlayer2();
			}
		});
    }
    private void addViewSprite()
    {
    	mViewSprite=mPlayView.obtainViewSprite();
        mGLRelativeLayout.setViewSprite(mViewSprite);
        mGLRelativeLayout.invalidate();
        
        ViewGroup.LayoutParams  params=mGLRelativeLayout.getLayoutParams();
        params.height=mViewSprite.getHeight();  //因为布局时, 宽度一致, 这里调整高度,让他们一致.
        
        mGLRelativeLayout.setLayoutParams(params);
    }
    private void toastStop()
    {
    	Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
    	if(mplayer!=null){
    		mplayer.stop();
    		mplayer.release();
    		mplayer=null;
    	}
    	
    	if(mplayer2!=null){
    		mplayer2.stop();
    		mplayer2.release();
    		mplayer2=null;
    	}
    	
    	if(mPlayView!=null){
    		mPlayView.stopMediaPool();
    		mPlayView=null;        		   
    	}
    	  if(SDKFileUtils.fileExist(dstPath)){
    		  SDKFileUtils.deleteFile(dstPath);
          }
          if(SDKFileUtils.fileExist(editTmpPath)){
        	  SDKFileUtils.deleteFile(editTmpPath);
          } 
          if(numberBarTimer!=null){
        	  numberBarTimer.cancel();
        	  numberBarTimer=null;
          }
    }
    private float xpos=0,ypos=0;

    /**
     * 提示:实际使用中没有主次之分, 只要是继承自ISprite的对象(FilterSprite除外),都可以调节,这里仅仅是举例
     */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		switch (seekBar.getId()) {
			case R.id.id_mediapool_skbar_rotate:
				if(mViewSprite!=null){
					mViewSprite.setRotate(progress);
				}
				break;
			case R.id.id_mediapool_skbar_move:
					if(mViewSprite!=null){
						 xpos+=10;
						 ypos+=10;
						 
						 if(xpos>mPlayView.getViewWidth())
							 xpos=0;
						 if(ypos>mPlayView.getViewWidth())
							 ypos=0;
						 mViewSprite.setPosition(xpos, ypos);
					}
				break;				
			case R.id.id_mediapool_skbar_scale:
				if(mViewSprite!=null){
					float scale=(float)progress/100;
					mViewSprite.setScale(scale);
				}
			break;		
			case R.id.id_mediapool_skbar_red:
					if(mViewSprite!=null){
						mViewSprite.setRedPercent(progress);  //设置每个RGBA的比例,默认是1
					}
				break;

			case R.id.id_mediapool_skbar_green:
					if(mViewSprite!=null){
						mViewSprite.setGreenPercent(progress);
					}
				break;

			case R.id.id_mediapool_skbar_blue:
					if(mViewSprite!=null){
						mViewSprite.setBluePercent(progress);
					}
				break;

			case R.id.id_mediapool_skbar_alpha:
					if(mViewSprite!=null){
						mViewSprite.setAlphaPercent(progress);
					}
				break;
				
			default:
				break;
		}
	}
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
	private ViewPager advPager = null;
	private boolean isContinue = true;
	private AtomicInteger what = new AtomicInteger(0);
	/**
	 * 设置广告栏的图片及切换效果
	 */
	private void initViewPager() {
		advPager = (ViewPager) findViewById(R.id.id_gllayout_pager);
		List<View> advPics = new ArrayList<View>();
		// 图片1
		ImageView img1 = new ImageView(this);
		img1.setBackgroundResource(R.drawable.advertising_default_1);
		advPics.add(img1);
		// 图片2
		ImageView img2 = new ImageView(this);
		img2.setBackgroundResource(R.drawable.advertising_default_2);
		advPics.add(img2);
		// 图片3
		ImageView img3 = new ImageView(this);
		img3.setBackgroundResource(R.drawable.advertising_default_3);
		advPics.add(img3);
		// 图片4
		ImageView img4 = new ImageView(this);
		img4.setBackgroundResource(R.drawable.advertising_default);
		advPics.add(img4);
  
		advPager.setAdapter(new AdvAdapter(advPics));
		advPager.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_MOVE:
					isContinue = false;
					break;
				case MotionEvent.ACTION_UP:
					isContinue = true;
					break;
				default:
					isContinue = true;
					break;
				}
				return false;
			}
		});
		// 定时滑动线程
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					if (isContinue) {
						viewHandler.sendEmptyMessage(what.get());
						whatOption();
					}
				}
			}

		}).start();
	}

	/**
	 * 操作圆点轮换变背景
	 */
	private void whatOption() {
		what.incrementAndGet();
		if (what.get() > 4 - 1) {  //共4张图片
			what.getAndAdd(-4);
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			
		}
	}

	/**
	 * 处理定时切换广告栏图片的句柄
	 */
	private final Handler viewHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			advPager.setCurrentItem(msg.what);
			super.handleMessage(msg);
		}

	};
	private final class AdvAdapter extends PagerAdapter {
		private List<View> views = null;

		public AdvAdapter(List<View> views) {
			this.views = views;
		}

		@Override
		public void destroyItem(View arg0, int arg1, Object arg2) {
			((ViewPager) arg0).removeView(views.get(arg1));
		}

		@Override
		public void finishUpdate(View arg0) {

		}

		@Override
		public int getCount() {
			return views.size();
		}

		@Override
		public Object instantiateItem(View arg0, int arg1) {
			((ViewPager) arg0).addView(views.get(arg1), 0);
			return views.get(arg1);
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {

		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View arg0) {

		}

	}
	
}
