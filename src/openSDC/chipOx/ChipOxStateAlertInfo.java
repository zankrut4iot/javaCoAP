package openSDC.chipOx;

import com.draeger.medical.biceps.common.model.PausableActivation;

public class ChipOxStateAlertInfo{
	private String alSigDescriptorHandle;
	private PausableActivation currentAlSig;
	private int byteIndex;
	private int mask;
	
	public ChipOxStateAlertInfo(String alSigDescriptorHandle,
			PausableActivation currentAlSig, int byteIndex, int mask) {
		super();
		this.setAlSigDescriptorHandle(alSigDescriptorHandle);
		this.setCurrentAlSig(currentAlSig);
		this.setByteIndex(byteIndex);
		this.setMask(mask);
	}

	public String getAlSigDescriptorHandle() {
		return alSigDescriptorHandle;
	}

	public void setAlSigDescriptorHandle(String alSigDescriptorHandle) {
		this.alSigDescriptorHandle = alSigDescriptorHandle;
	}

	public PausableActivation getCurrentAlSig() {
		return currentAlSig;
	}

	public void setCurrentAlSig(PausableActivation currentAlSig) {
		this.currentAlSig = currentAlSig;
	}

	public int getByteIndex() {
		return byteIndex;
	}

	public void setByteIndex(int byteIndex) {
		this.byteIndex = byteIndex;
	}

	public int getMask() {
		return mask;
	}

	public void setMask(int mask) {
		this.mask = mask;
	}
	
}