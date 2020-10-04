package openSDC.chipOx;

import java.util.Date;
import java.util.Random;

public class ChipOxSimulation extends Thread {
	
	public static final int SPO2_BASE_VALUE = 92;
	public static final int PULSE_BASE_VALUE = 60;
	public static final int PULSE_MAX_VALUE = 120;
	public static final int PULSE_MIN_VALUE = 35;
	

	private volatile boolean running = true;
	
	public void run(){
		boolean useCoap = ChipOxState.getInstance().useCoap();
		
		ChipOxConnector connector = ChipOxConnector.getInstance();
		ChipOxState chipOxState= ((ChipOxDevice) connector.getRealDevice()).getChipOxState();
		chipOxState.setDeviceState((short)0); //no error messages
		
		int randSpO2 = 0;
		int randPulse = 0;
		chipOxState.setPulse(PULSE_BASE_VALUE);
		
		int degrees = 0; //for pseudo Plethysmogram 
		int cycles = 0;
		
		Date date = new Date();
		long seed = date.getTime();
		Random randomizer = new Random(seed);
		
		while(running){
			
			//pulse and SpO2 only every two seconds
			if (cycles % 20 == 0){
				cycles = 0; //reset
				
				
				randSpO2 =  SPO2_BASE_VALUE + randomizer.nextInt(100 - SPO2_BASE_VALUE);
				if(randomizer.nextBoolean()){
					randPulse = chipOxState.getPulse() + randomizer.nextInt(3);
				}
				else{
					randPulse = chipOxState.getPulse() - randomizer.nextInt(3);
				}
				
				//avoid abnormal values
				if(randPulse < PULSE_MIN_VALUE 
						|| randPulse > PULSE_MAX_VALUE
						|| randomizer.nextInt(50) == 1){
					randPulse = PULSE_BASE_VALUE;
				}

				
				System.err.println("Current Simulated Value for SpO2: " + randSpO2 );
				System.err.println("Current Simulated Value for Pulse: " + randPulse );
				
				chipOxState.setPulseWithOSCPPropagation(randPulse);
				chipOxState.setSpO2WithOSCPPropagation(randSpO2);
				chipOxState.evaluateAndPropagateDeviceState();
				chipOxState.evaluateAndPropagateDeviceStateAlerts();
			}
			
			if(!useCoap){
				//plethysmogram RT samples
				double[] tmpDataDouble = new double[10];
				for(int i = 0; i < 10; i++){
					tmpDataDouble[i] = 128 + (Math.sin(degrees + (double)i/10) * 128);
				}
				
				connector.pushStreamValues(tmpDataDouble);
			}
			degrees+=1;
			cycles++;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
