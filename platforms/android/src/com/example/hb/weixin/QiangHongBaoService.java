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
	
	//�������������
	private KeyguardManager km;
	private KeyguardLock kl;
	private boolean enableKeyguard = true;//Ĭ������Ļ��
	
	//������Ļ���
	private PowerManager pm;
	private PowerManager.WakeLock wl = null;
	
	private static final String WEINXIN_HONGBAO_TEXT_KEY = "΢�ź��";//��ʱ��һ������һ����
	private static final String WEINXIN_HONGBAO_TEXT_GET = "��ȡ���";//��ʱ��һ������һ����
	private static final String WEINXIN_HONGBAO_TEXT_OPEN = "����";//��ʱ��һ������һ����,Ŀǰ��û��
	
	
	
	/** �б����촰������id */ 
	private static final String WEINXIN_TEXT_ID_KEY = "com.tencent.mm:id/gr";
	/** �б�����Դid */ 
	private static final String WEINXIN_HONGBAO_ID_KEY = "com.tencent.mm:id/cd"; 
	/** ��ȡ�����Դid */ 
	private static final String WEINXIN_HONGBAO_ID_GET = "com.tencent.mm:id/a10"; 
	/** �㿪�����Դid */ 
	private static final String WEINXIN_HONGBAO_ID_OPEN = "com.tencent.mm:id/b9m"; 
	

	private static int lastHongbaoNum =0; //��ʼ��û�к��
	private static int speHongbaoNum =5; //����������������������ֵ������Ȼ��ȡ
	
	private static long lastHongbaoTime =System.currentTimeMillis(); //���һ�����������ʱ��
	private static int maxNearTime = 3; //�����ʱ����  ��
	/** 
	 * �������صķ��� 
	 * ����ϵͳ������AccessbilityEvent���Ѿ����������ļ����� 
	 * typeNotificationStateChanged|typeWindowStateChanged|typeViewScrolled   
	 * typeAllMask    
	 */  

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
	    //�����¼�,�紥����֪ͨ���仯������仯��   
		final int eventType = event.getEventType();  
		// ֪ͨ���¼�  
		System.out.println("�¼�����:"+eventType+",�¼���:"+event.getClassName());
/*		List<CharSequence> texts1 = event.getText();  
		if (!texts1.isEmpty()) {  
			for (CharSequence t : texts1) {  
				String text = String.valueOf(t);  
				System.out.println("��Ϣ����:"+text);
			}  
		}*/  
		if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {  //64
		// ֪ͨ����������Ϣ  
		// ��ȡ֪ͨ����Ϣ����  
			List<CharSequence> texts = event.getText();  
			// ����Ƿ��к����Ϣ  
			if (!texts.isEmpty()) {  
				for (CharSequence t : texts) {  
					String text = String.valueOf(t);  
					System.out.println("��Ϣ����:"+text);
					if (text.contains(WEINXIN_HONGBAO_TEXT_KEY)) {  
						wake(true);//����Ļ--ò�Ʋ���ȡ������
						unlock(true);//����--Ŀǰ�е�����,�ظ������������쳣
						openNotify(event); // ���֪ͨ  ����Ҫ�ǳ�����ȡ����¼�
						wake(false);
//						unlock(false);
						break;  
					}  
				}  
			} else{
				System.out.println("��Ϣ����Ϊ��"); 
			}
		}else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) { //32
			// ���ڸı䣬�����������棬������򿪺��   
			System.out.println("���ڸı�");
			openHongBao(event);
		}else if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) { //2048 �������ݸı�
			System.out.println("���촰�����ݱ仯,������������Ϣ,��ʱ������");
//			talking(event);
		}else if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) { //4096  ��Ļ������������ʾ����
			//��������ʱ�յ���Ϣ�������Ǻ��
			System.out.println("���촰����Ļ����,����������Ϣ");
			talking(event);
		} 

	}

	private void talking(AccessibilityEvent event) {
		//���ô���������Ϣ����Ŀǰ�ͻ�ȡ��Ļ���µ� ��ȡ�����
		openPacket();
	}
		
	@Override
	protected boolean onKeyEvent(KeyEvent event) {
	    //���հ����¼�
	    return super.onKeyEvent(event);
	}

	@Override
	public void onInterrupt() {
	  //�����жϣ�����Ȩ�رջ��߽�����ɱ��
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		System.out.println("demo�ر�");
		wake(false);
		unlock(false);
		
	}
	@Override
	protected void onServiceConnected() {
		try {
			super.onServiceConnected();
		    //���ӷ����,һ��������Ȩ�ɹ������յ�
		    System.out.println("onServiceConnected����");
		    //��ȡ��Դ����������
		    pm=(PowerManager)getSystemService(Context.POWER_SERVICE);
		    System.out.println("��ȡ��Դ����������:"+pm.toString());
		    //�õ�����������������
		    km= (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
		    //��ʼ��һ������������������
		    kl = km.newKeyguardLock("unLock");
		} catch (Exception e) {
			System.out.println("onServiceConnected error:"+e);
		}
	}
	/**  
	 * ��֪ͨ����Ϣ  
	 */  
	private void openNotify(AccessibilityEvent event){  
//		System.out.println("�Զ����֪ͨ:"+(event.getParcelableData() ==null?"��":event.getParcelableData().getClass().getName()));
	    if (event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {  
	        return;  
	    }  
	    // ��΢�ŵ�֪ͨ����Ϣ��  
	    // ��ȡNotification����   
	    Notification notification = (Notification) event.getParcelableData();  
	    // �������е�PendingIntent����΢�Ž���  
	    PendingIntent pendingIntent = notification.contentIntent;  
	    try { 
	    	System.out.println("�Զ����֪ͨ����Ϣ����������״̬�仯");
	        pendingIntent.send(); // ���֪ͨ����Ϣ 
	    } catch (CanceledException e) {  
	        e.printStackTrace();  
	    }  
	} 
	/** 
	 * ��΢�ź��ж���ʲô���棬������Ӧ�Ķ��� 
	 */  
	private void openHongBao(AccessibilityEvent event) { 
	    if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(event.getClassName())) {  
	        // ��������  
	    	System.out.println("����");
	        getPacket(); 
	        unlock(false);
	    } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName())) {  
	    	System.out.println("�Ѿ�����������");
	        // �������󣬿�������Ľ���  
	    	// ����������ϸ�ļ�¼����  
	    } else if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {  
	        // �������  
	    	System.out.println("�������");
//	    	openPacket();
	    } 
	}  
	
	/** 
	 * ���� 
	 */  
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)  
	private void getPacket() {  
	    AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();  
	    if (nodeInfo == null) {  
	        return;  
	    }  
	    List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(WEINXIN_HONGBAO_TEXT_OPEN); 
	    System.out.println("�������ֻ�ò�������:"+list.size());
	    if(list.isEmpty()){
	    	list = nodeInfo.findAccessibilityNodeInfosByViewId(WEINXIN_HONGBAO_ID_OPEN);
	    	System.out.println("����ID��ò�������:"+list.size());
	    }
	    for (AccessibilityNodeInfo n :list) { 
	    	System.out.println("�Զ�����:");
	        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);  
	    }  
	}  
	
	/** 
	 * ����������е��� 
	 * �����ظ����һ�����
	 * ��ҪΪ�˽�����������ػᴥ�����ڱ仯
	 */  
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)  
	private void openPacket() {  
	    AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();  
	    if (nodeInfo == null) {  
	        return;  
	    }  
	    // �ҵ���ȡ����ĵ���¼�  
	    List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(WEINXIN_HONGBAO_TEXT_GET); //Ŀǰ�汾���ֺ�IDһ�� 
//	    list = nodeInfo.findAccessibilityNodeInfosByViewId(WEINXIN_HONGBAO_ID_GET);
	    // ���µĺ������   
	    System.out.println("����������������:"+list.size());//һ����ʱע�ͣ�Ŀǰ�汾���ֺ�IDһ��
	    if(!list.isEmpty() &&list.size() == lastHongbaoNum){//�˴β�Ϊ�գ��Һ���һ����ͬ������Ҫ�ж���һ�β�Ϊ�յ�ʱ��
	    	if((System.currentTimeMillis()- lastHongbaoTime)/1000 > maxNearTime){//����5��,�����һ�κ������
	    		lastHongbaoNum = 0;
	    	}
	    }
	    if(list.isEmpty() ||( list.size() < speHongbaoNum && list.size()==lastHongbaoNum )){//���ܻ�©�������Ϊ��ȡ���ǵ�ǰ���ڵĺ�������������ᣬ
	    	System.out.println("��ȡ�ĺ������С��"+speHongbaoNum+"��,��û��");
	    	lastHongbaoNum = list.size();
	    	lastHongbaoTime = System.currentTimeMillis();
	    	return;  
	    }
	    lastHongbaoNum = list.size();
	    lastHongbaoTime = System.currentTimeMillis();
		for (int i = list.size() - 1 ; i >= 0; i--) {  
		        // ͨ�����Կ�֪[��ȡ���]��text�������ɱ��������getParent()��ȡ�ɱ�����Ķ���  
	        AccessibilityNodeInfo parent = list.get(i).getParent();  
	        // �ȸ���д��toString()����������������ȡClassName@hashCode��  
	        if ( parent != null ) { 
	        	System.out.println("��ȡ���µ�һ�����");
	            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);  
	            break; // ֻ�����µ�һ�����  
	        }  
	    }  
	}
	//������Ļ�ͽ���
	private void wake(boolean unLock){
		try {
			if(unLock){
				//��Ϊ����״̬������Ļ
				if(!pm.isScreenOn()) {
					//��ȡ��Դ����������ACQUIRE_CAUSES_WAKEUP��������ܴӺ���������Ļ
					wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "bright");
					//������Ļ
					wl.acquire();
					System.out.println("demo����");
				}
			}else{
				//��֮ǰ���ѹ���Ļ���ͷ�֮ʹ��Ļ�����ֳ���
				if(wl != null) {
					wl.release();
					wl = null;
					System.out.println("demo�ص�");
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
					//���ý�����־�����ж��������ܷ�����
					enableKeyguard = false;
					//����
					kl.disableKeyguard();
					System.out.println("demo����");
				}
			}else{
				//���֮ǰ�����������Իָ�ԭ��
				if(!enableKeyguard) {
				//����
				enableKeyguard= true;
				kl.reenableKeyguard();
				System.out.println("demo����");
				}
			}
		} catch (Exception e) {
			System.out.println("unlock error"+e);
		}
	}
}

