package com.example.hb.weixin;

import java.util.List;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * 
 * @author bswsfhcw
 *
 */
@SuppressLint("NewApi")
public class QiangHongBaoService extends AccessibilityService{
	
	//锁屏、解锁相关
	private KeyguardManager km;
	private KeyguardLock kl;
	private boolean enableKeyguard = true;//默认有屏幕锁
	
	//唤醒屏幕相关
	private PowerManager pm;
	private PowerManager.WakeLock wl = null;
	
	private static final String WEINXIN_HONGBAO_TEXT_KEY = "微信红包";//临时顶一个，不一定是
	private static final String WEINXIN_HONGBAO_TEXT_GET = "领取红包";//临时顶一个，不一定是
	private static final String WEINXIN_HONGBAO_TEXT_OPEN = "拆红包";//临时顶一个，不一定是,目前就没有
	
	
	
	/** 列表聊天窗口内容id */ 
	private static final String WEINXIN_TEXT_ID_KEY = "com.tencent.mm:id/gr";
	/** 列表红包资源id */ 
	private static final String WEINXIN_HONGBAO_ID_KEY = "com.tencent.mm:id/cd"; 
	/** 领取红包资源id */ 
	private static final String WEINXIN_HONGBAO_ID_GET = "com.tencent.mm:id/a10"; 
	/** 点开红包资源id */ 
	private static final String WEINXIN_HONGBAO_ID_OPEN = "com.tencent.mm:id/b9m"; 
	

	private static int lastHongbaoNum =0; //初始化没有红包
	private static int speHongbaoNum =5; //特殊红包个数，如果超过这个值，则依然领取
	
	private static long lastHongbaoTime =System.currentTimeMillis(); //最近一次抢到红包的时间
	private static int maxNearTime = 3; //最大呢时间间隔  秒
	/** 
	 * 必须重载的方法 
	 * 接收系统发来的AccessbilityEvent，已经按照配置文件过滤 
	 * typeNotificationStateChanged|typeWindowStateChanged|typeViewScrolled   
	 * typeAllMask    
	 */  

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
	    //接收事件,如触发了通知栏变化、界面变化等   
		final int eventType = event.getEventType();  
		// 通知栏事件  
		System.out.println("事件类型:"+eventType+",事件名:"+event.getClassName());
/*		List<CharSequence> texts1 = event.getText();  
		if (!texts1.isEmpty()) {  
			for (CharSequence t : texts1) {  
				String text = String.valueOf(t);  
				System.out.println("信息内容:"+text);
			}  
		}*/  
		if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {  //64
		// 通知栏出现新信息  
		// 获取通知栏信息内容  
			List<CharSequence> texts = event.getText();  
			// 检查是否有红包信息  
			if (!texts.isEmpty()) {  
				for (CharSequence t : texts) {  
					String text = String.valueOf(t);  
					System.out.println("信息内容:"+text);
					if (text.contains(WEINXIN_HONGBAO_TEXT_KEY)) {  
						wake(true);//打开屏幕--貌似不能取消常亮
						unlock(true);//解锁--目前有点问题,重复解锁可能有异常
						openNotify(event); // 点击通知  ，主要是出发领取红包事件
						wake(false);
//						unlock(false);
						break;  
					}  
				}  
			} else{
				System.out.println("信息内容为空"); 
			}
		}else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) { //32
			// 窗口改变，如果是聊天界面，则调动打开红包   
			System.out.println("窗口改变");
			openHongBao(event);
		}else if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) { //2048 窗口内容改变
			System.out.println("聊天窗口内容变化,可能是最新消息,暂时不处理");
//			talking(event);
		}else if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) { //4096  屏幕滚动，卷起，显示文字
			//正在聊天时收到消息，可能是红包
			System.out.println("聊天窗口屏幕滚动,可能是新消息");
			talking(event);
		} 

	}

	private void talking(AccessibilityEvent event) {
		//不好处理，有新消息来，目前就获取屏幕最新的 领取红包吧
		openPacket();
	}
		
	@Override
	protected boolean onKeyEvent(KeyEvent event) {
	    //接收按键事件
	    return super.onKeyEvent(event);
	}

	@Override
	public void onInterrupt() {
	  //服务中断，如授权关闭或者将服务杀死
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		System.out.println("demo关闭");
		wake(false);
		unlock(false);
		
	}
	@Override
	protected void onServiceConnected() {
		try {
			super.onServiceConnected();
		    //连接服务后,一般是在授权成功后会接收到
		    System.out.println("onServiceConnected开启");
		    //获取电源管理器对象
		    pm=(PowerManager)getSystemService(Context.POWER_SERVICE);
		    System.out.println("获取电源管理器对象:"+pm.toString());
		    //得到键盘锁管理器对象
		    km= (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
		    //初始化一个键盘锁管理器对象
		    kl = km.newKeyguardLock("unLock");
		} catch (Exception e) {
			System.out.println("onServiceConnected error:"+e);
		}
	}
	/**  
	 * 打开通知栏消息  
	 */  
	private void openNotify(AccessibilityEvent event){  
//		System.out.println("自动点击通知:"+(event.getParcelableData() ==null?"空":event.getParcelableData().getClass().getName()));
	    if (event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {  
	        return;  
	    }  
	    // 将微信的通知栏消息打开  
	    // 获取Notification对象   
	    Notification notification = (Notification) event.getParcelableData();  
	    // 调用其中的PendingIntent，打开微信界面  
	    PendingIntent pendingIntent = notification.contentIntent;  
	    try { 
	    	System.out.println("自动点击通知栏信息，触发窗口状态变化");
	        pendingIntent.send(); // 点击通知栏消息 
	    } catch (CanceledException e) {  
	        e.printStackTrace();  
	    }  
	} 
	/** 
	 * 打开微信后，判断是什么界面，并做相应的动作 
	 */  
	private void openHongBao(AccessibilityEvent event) { 
	    if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(event.getClassName())) {  
	        // 拆红包界面  
	    	System.out.println("拆红包");
	        getPacket(); 
	        unlock(false);
	    } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName())) {  
	    	System.out.println("已经看到红包金额");
	        // 拆完红包后，看红包金额的界面  
	    	// 拆完红包后看详细的纪录界面  
	    } else if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {  
	        // 聊天界面  
	    	System.out.println("聊天界面");
//	    	openPacket();
	    } 
	}  
	
	/** 
	 * 拆红包 
	 */  
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)  
	private void getPacket() {  
	    AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();  
	    if (nodeInfo == null) {  
	        return;  
	    }  
	    List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(WEINXIN_HONGBAO_TEXT_OPEN); 
	    System.out.println("根据文字获得拆红包个数:"+list.size());
	    if(list.isEmpty()){
	    	list = nodeInfo.findAccessibilityNodeInfosByViewId(WEINXIN_HONGBAO_ID_OPEN);
	    	System.out.println("根据ID获得拆红包个数:"+list.size());
	    }
	    for (AccessibilityNodeInfo n :list) { 
	    	System.out.println("自动拆红包:");
	        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);  
	    }  
	}  
	
	/** 
	 * 在聊天界面中点红包 
	 * 避免重复点击一个红包
	 * 主要为了解决看完红包返回会触发窗口变化
	 */  
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)  
	private void openPacket() {  
	    AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();  
	    if (nodeInfo == null) {  
	        return;  
	    }  
	    // 找到领取红包的点击事件  
	    List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(WEINXIN_HONGBAO_TEXT_GET); //目前版本文字和ID一样 
//	    list = nodeInfo.findAccessibilityNodeInfosByViewId(WEINXIN_HONGBAO_ID_GET);
	    // 最新的红包领起   
	    System.out.println("根据文字领红包个数:"+list.size());//一下暂时注释，目前版本文字和ID一样
	    if(!list.isEmpty() &&list.size() == lastHongbaoNum){//此次不为空，且和上一次相同，则需要判断上一次不为空的时间
	    	if((System.currentTimeMillis()- lastHongbaoTime)/1000 > maxNearTime){//超过5秒,清空上一次红包个数
	    		lastHongbaoNum = 0;
	    	}
	    }
	    if(list.isEmpty() ||( list.size() < speHongbaoNum && list.size()==lastHongbaoNum )){//可能会漏红包，因为获取的是当前窗口的红包数，基本不会，
	    	System.out.println("领取的红包个数小于"+speHongbaoNum+"个,且没变");
	    	lastHongbaoNum = list.size();
	    	lastHongbaoTime = System.currentTimeMillis();
	    	return;  
	    }
	    lastHongbaoNum = list.size();
	    lastHongbaoTime = System.currentTimeMillis();
		for (int i = list.size() - 1 ; i >= 0; i--) {  
		        // 通过调试可知[领取红包]是text，本身不可被点击，用getParent()获取可被点击的对象  
	        AccessibilityNodeInfo parent = list.get(i).getParent();  
	        // 谷歌重写了toString()方法，不能用它获取ClassName@hashCode串  
	        if ( parent != null ) { 
	        	System.out.println("领取最新的一个红包");
	            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);  
	            break; // 只领最新的一个红包  
	        }  
	    }  
	}
	//唤醒屏幕和解锁
	private void wake(boolean unLock){
		try {
			if(unLock){
				//若为黑屏状态则唤醒屏幕
				if(!pm.isScreenOn()) {
					//获取电源管理器对象，ACQUIRE_CAUSES_WAKEUP这个参数能从黑屏唤醒屏幕
					wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "bright");
					//点亮屏幕
					wl.acquire();
					System.out.println("demo亮屏");
				}
			}else{
				//若之前唤醒过屏幕则释放之使屏幕不保持常亮
				if(wl != null) {
					wl.release();
					wl = null;
					System.out.println("demo关灯");
				}
			}
		} catch (Exception e) {
			System.out.println("wake error"+e);
		}
	}
	private void unlock(boolean unLock){
		try {
			if(unLock){
				if(km.inKeyguardRestrictedInputMode()) {
					//设置解锁标志，以判断抢完红包能否锁屏
					enableKeyguard = false;
					//解锁
					kl.disableKeyguard();
					System.out.println("demo解锁");
				}
			}else{
				//如果之前解过锁则加锁以恢复原样
				if(!enableKeyguard) {
				//锁屏
				enableKeyguard= true;
				kl.reenableKeyguard();
				System.out.println("demo加锁");
				}
			}
		} catch (Exception e) {
			System.out.println("unlock error"+e);
		}
	}
}

