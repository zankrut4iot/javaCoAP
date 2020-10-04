package openSDC.chipOx;

import java.util.ArrayList;
import java.util.List;

import com.draeger.medical.biceps.common.model.PausableActivation;

import realDeviceConnector.RealDevice;

public class ChipOxDevice extends RealDevice{
	
	private static ChipOxDevice chipOxDevice = null;
	
	//public static final String PATH_TO_XML_FILE = "description/ChipOx.xml";
	public static final String PATH_TO_XML_FILE = "description/ChipOx_with_Alerts.xml";
	
	public final static int PULSE_LOWER = 50;
	public final static int PULSE_UPPER = 70;
	public final static int SPO2_LOWER = 95;
	public final static int SPO2_UPPER = 100;
	
	public final static String HANDLE_SPO2_UPPER = "handle_SpO2_upper_limit";
	public final static String HANDLE_SPO2_LOWER = "handle_SpO2_lower_limit";
	public final static String HANDLE_PULSE_UPPER = "handle_pulse_upper_limit";
	public final static String HANDLE_PULSE_LOWER = "handle_pulse_lower_limit";
	public final static String HANDLE_PLETHYSMOGRAM_STREAM = "handle_plethysmogram_stream";
	
	public final static String HANDLE_PULSE = "handle_pulse";
	public final static String HANDLE_SPO2 = "handle_SpO2";
	
	public final static String HANDLE_ALSIG_PULSE = "alSig_pulse";
	public final static String HANDLE_ALSIG_SPO2 = "alSig_SpO2";
	public final static String HANDLE_ALSIG_SENSOR_DEFECTIVE = "alSig_sensor_defective";
	public final static String HANDLE_ALSIG_SENSOR_OFF = "alSig_sensor_off";
	public final static String HANDLE_ALSIG_FINGER_OUT = "alSig_finger_out";
	
	public final static int STREAM_FRAME_CAPACITY = 20;
	
	
	private ChipOxState chipOxState = null;
	private List<ChipOxStateAlertInfo> stateAlertInfoList = null;
	
	private ChipOxDevice() {
		super(PATH_TO_XML_FILE);
		setChipOxState(ChipOxState.getInstance());
		
		//initialize state alter infos
		setStateAlertInfoList(new ArrayList<ChipOxStateAlertInfo>());
		ChipOxStateAlertInfo tmp = 
				new ChipOxStateAlertInfo(HANDLE_ALSIG_SENSOR_DEFECTIVE, PausableActivation.PAUSED, 0, 0x04);
		stateAlertInfoList.add(tmp);
		tmp = new ChipOxStateAlertInfo(HANDLE_ALSIG_SENSOR_OFF, PausableActivation.PAUSED, 1, 0x01);
		stateAlertInfoList.add(tmp);
		tmp = new ChipOxStateAlertInfo(HANDLE_ALSIG_FINGER_OUT, PausableActivation.PAUSED, 1, 0x02);
		stateAlertInfoList.add(tmp);
	}
	
	public static synchronized ChipOxDevice getInstance(){
		if ( chipOxDevice == null){
			chipOxDevice = new ChipOxDevice();
		}
		return chipOxDevice;
	}

	
	
	public ChipOxState getChipOxState() {
		return chipOxState;
	}

	public void setChipOxState(ChipOxState chipOxState) {
		this.chipOxState = chipOxState;
	}

	public List<ChipOxStateAlertInfo> getStateAlertInfoList() {
		return stateAlertInfoList;
	}

	public void setStateAlertInfoList(List<ChipOxStateAlertInfo> stateAlertInfoList) {
		this.stateAlertInfoList = stateAlertInfoList;
	}

}
