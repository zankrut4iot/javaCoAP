package openSDC.openSDCDevice.chipOx;

import java.util.concurrent.BlockingQueue;

import openSDC.chipOx.ChipOxDevice;
import openSDC.openSDCDevice.MyDeviceNodeInterface;
import openSDC.openSDCDevice.MySetValueHandler;
import openSDC.openSDCDevice.MyWaveformGenerator;

import com.draeger.medical.biceps.device.mdi.BICEPSDeviceNodeInterfaceDescriptionProvider;
import com.draeger.medical.biceps.device.mdi.BICEPSDeviceNodeInterfaceStateProvider;
import com.draeger.medical.biceps.device.mdi.impl.DefaultMedicalDeviceCommunicationInterface;
import com.draeger.medical.biceps.device.mdi.interaction.MDINotification;

public class MyChipOxMedicalCommunicationInterface extends DefaultMedicalDeviceCommunicationInterface{

		private final MyDeviceNodeInterface medicalDeviceNodeInterface=new MyDeviceNodeInterface();
		private final MyChipOxCommandProcessingTask cmdProcTask=new MyChipOxCommandProcessingTask(this);
	
		@Override
		public BICEPSDeviceNodeInterfaceDescriptionProvider getInterfaceDescriptionProvider() {
			return medicalDeviceNodeInterface;
		}
		
		@Override
		public BICEPSDeviceNodeInterfaceStateProvider getInterfaceStateProvider() {
			return medicalDeviceNodeInterface;
		}

		@Override
		protected void addHandlers() {
			addHandler(new MySetValueHandler(cmdProcTask));
		}

		@Override
		protected void addWaveformGenerators() {
			addWaveformGenerator(new MyWaveformGenerator(ChipOxDevice.HANDLE_PLETHYSMOGRAM_STREAM, ChipOxDevice.STREAM_FRAME_CAPACITY ));
		}

		@Override
		public void connect() 
		{
			System.out.println("Connected");
		}

		@Override
		public void disconnect() {
			System.out.println("Disconnected");
		}

		@Override
		public void startupComplete() {
			super.startupComplete();
		}

		@Override
		public void setMDINotificationQueue(
				BlockingQueue<MDINotification> notificationQueue) {
			super.setMDINotificationQueue(notificationQueue);
		}


}
