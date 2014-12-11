package com.ds.IrDA;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

/**
 * ����ģ��ͻ���������Service
 * @author GuoDong
 *
 */
public class BluetoothClientService extends Service {
	
	//��������Զ���豸����
	private List<BluetoothDevice> discoveredDevices = new ArrayList<BluetoothDevice>();
		//����������
	private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	//����ͨѶ�߳�
	private BluetoothCommunThread communThread;
	//������Ϣ�㲥�Ľ�����
	private BroadcastReceiver controlReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if (BluetoothTools.ACTION_START_DISCOVERY.equals(action)) {
				//��ʼ����
				discoveredDevices.clear();	//��մ���豸�ļ���
				bluetoothAdapter.enable();	//������
				bluetoothAdapter.startDiscovery();	//��ʼ����
				
			} else if (BluetoothTools.ACTION_SELECTED_DEVICE.equals(action)) {
				//ѡ�������ӵķ������豸
				BluetoothDevice device = (BluetoothDevice)intent.getExtras().get(BluetoothTools.DEVICE);
				
				//�����豸�����߳�
				new BluetoothClientConnThread(handler, device).start();
				
			} else if (BluetoothTools.ACTION_STOP_SERVICE.equals(action)) {
				//ֹͣ��̨����
				if (communThread != null) {
					communThread.isRun = false;
				}
				stopSelf();
				
			}			
			else if (BluetoothTools.ACTION_DATA_TO_SERVICE.equals(action)) {
				//��ȡ����
				if (communThread != null) {
					if(ClientActivity.sort==0)
					{	byte[] nothing={1};
						communThread.write(nothing);
					}
					else
					{	int obj=ClientActivity.obj;
				        int []out_num=ClientActivity.out_num;
				        int []out_order=ClientActivity.out_order;
				        byte[]buffer_in=new byte[out_num[obj]];
				        String	filename_in="study_model"+ClientActivity.sort+".txt";
				        try {
							FileInputStream in=openFileInput(filename_in);
							try {
								in.skip(out_order[obj]);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							try {
								in.read(buffer_in, 0, out_num[obj]);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							try {
								in.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				        communThread.write(buffer_in);
					}
				}
				
			}
		}
	};
	
	//���������㲥�Ľ�����
	private BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			//��ȡ�㲥��Action
			String action = intent.getAction();

			if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				//��ʼ����
			} else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				//����Զ�������豸
				//��ȡ�豸
				BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);//��EXTRA_DEVICE���Ϊ����
				discoveredDevices.add(bluetoothDevice);

				//���ͷ����豸�㲥
				Intent deviceListIntent = new Intent(BluetoothTools.ACTION_FOUND_DEVICE);
				deviceListIntent.putExtra(BluetoothTools.DEVICE, bluetoothDevice);
				sendBroadcast(deviceListIntent);
				
			} 
		}
	};
	
	//���������߳���Ϣ��Handler
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//������Ϣ
			switch (msg.what) {
			case BluetoothTools.MESSAGE_CONNECT_ERROR:
				//���Ӵ���
				//�������Ӵ���㲥
				Intent errorIntent = new Intent(BluetoothTools.ACTION_CONNECT_ERROR);
				sendBroadcast(errorIntent);
				break;
			case BluetoothTools.MESSAGE_CONNECT_SUCCESS:
				//���ӳɹ�
				
				//����ͨѶ�߳�
				communThread = new BluetoothCommunThread(handler, (BluetoothSocket)msg.obj);
				communThread.start();
				
				//�������ӳɹ��㲥
				Intent succIntent = new Intent(BluetoothTools.ACTION_CONNECT_SUCCESS);
				sendBroadcast(succIntent);
				break;
			case BluetoothTools.MESSAGE_READ_OBJECT:
				//��ȡ������
				//�������ݹ㲥���������ݶ���
				
				Intent dataIntent = new Intent(BluetoothTools.ACTION_DATA_TO_GAME);
                byte[] readBuf = (byte[]) msg.obj;//��ȡ�õ��ı�����
                int a=msg.arg1;//��ȡ������
                String readmessage="";//�����õ���ʾ����
                int temp;
                for(int i=0;i<a;i++)
                {
                	if(readBuf[i]<0)
                	{
                		temp=256+readBuf[i];
                	}
                	else
                	temp=readBuf[i];
                	readmessage=readmessage+String.valueOf(temp)+" ";
                }
                //�ж��Ƿ����ѧϰ״̬
                if(ClientActivity.flag==3)
                {
                if((readBuf[0]==(byte)0xAA)&(readBuf[1]==(byte)0x08))
                {	
                	int obj=ClientActivity.obj;//��ð�������ֵ
                	ClientActivity.study_num[obj]=a;//�洢������Ϣ
                	ClientActivity.study_order[obj]=ClientActivity.b;//�洢λ����Ϣ
                	ClientActivity.b +=a;
                	byte[]study_get =new byte[a];
                    System.arraycopy(readBuf,0,study_get,0,a);
                    study_get[1]=0x02;
            		String filename=ClientActivity.filename;
            		try {
    					FileOutputStream outstream=openFileOutput(filename,MODE_APPEND);//���ļ���
    					try {
    						outstream.write(study_get);
    					} catch (IOException e) {
    						// TODO Auto-generated catch block
    						e.printStackTrace();
    					}
    					try {
    						outstream.close();
    					} catch (IOException e) {
    						// TODO Auto-generated catch block
    						e.printStackTrace();
    					}
    				} catch (FileNotFoundException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				} 
    				Intent study_protol = new Intent(BluetoothTools.ACTION_STUDY_PROTOL);
    				//sendDataIntent.putExtra(BluetoothTools.DATA, message);
    				sendBroadcast(study_protol);
                }
                }
                if(ClientActivity.flag==1)
                {
                if((readBuf[0]==(byte)0xAA)&(readBuf[1]==(byte)0xF5))
                {
                	ClientActivity.flag=0;
                }
                }
               // String readMessage = new String(readBuf, 0, msg.arg1);
                ClientActivity.message= readmessage;
				sendBroadcast(dataIntent);
				break;
			}
			super.handleMessage(msg);
		}
		
	};
	
	/**
	 * ��ȡͨѶ�߳�
	 * @return
	 */
	public BluetoothCommunThread getBluetoothCommunThread() {
		return communThread;
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		
		super.onStart(intent, startId);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	/**
	 * Service����ʱ�Ļص�����
	 */
	@Override
	public void onCreate() {
		//discoveryReceiver��IntentFilter
		IntentFilter discoveryFilter = new IntentFilter();
		discoveryFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		discoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);//��������ϵͳ���͵�
		
		//controlReceiver��IntentFilter
		IntentFilter controlFilter = new IntentFilter();
		controlFilter.addAction(BluetoothTools.ACTION_START_DISCOVERY);
		controlFilter.addAction(BluetoothTools.ACTION_SELECTED_DEVICE);
		controlFilter.addAction(BluetoothTools.ACTION_STOP_SERVICE);
		controlFilter.addAction(BluetoothTools.ACTION_DATA_TO_SERVICE);
		
		//ע��BroadcastReceiver
		registerReceiver(discoveryReceiver, discoveryFilter);
		registerReceiver(controlReceiver, controlFilter);
		super.onCreate();
	}
	
	/**
	 * Service����ʱ�Ļص�����
	 */
	@Override
	public void onDestroy() {
		if (communThread != null) {
			communThread.isRun = false;
		}
		//�����
		unregisterReceiver(discoveryReceiver);
		super.onDestroy();
	}

}
