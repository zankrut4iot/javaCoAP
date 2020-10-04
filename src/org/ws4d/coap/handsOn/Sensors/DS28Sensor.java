package org.ws4d.coap.handsOn.Sensors;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DS28Sensor {

	static String w1DevicesBase = "/sys/bus/w1/devices";
	static String w1DeviceAttribute = "w1_slave";
	static String w1SlavePattern = "/sys/bus/w1/devices/%s/w1_slave";

	/**
	 * This function is used to get all one-wire devices available at the system
	 * @return all available one-wire bus devices
	 * @throws IOException
	 */
	public static List<Path> listDS28Sensors() throws IOException {
		List<Path> result = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(w1DevicesBase), "28-*")) {
			for (Path entry : stream) {
				result.add(entry.resolve(w1DeviceAttribute));
			}
		} catch (DirectoryIteratorException ex) {
			// I/O error encounted during the iteration, the cause is an IOException
			throw ex.getCause();
		}
		return result;
	}

	/**
	 * 
	 * @param index - The index of the sensor in the list of available one-wire sensors. To get the list use {@link #listDS28Sensors()}
	 * @return The float value of the temperature measurement from the selected one-wire temperature sensor
	 * @throws IOException
	 */
	public static float readDS18B20(int index) throws IOException {
		List<Path> lps = listDS28Sensors();
		if (index >= lps.size())
			throw new IOException("Sensor not found.");
		Path externalTemperaturePath = lps.get(index);
		List<String> valueText;
		valueText = Files.readAllLines(externalTemperaturePath);
		if (2 != valueText.size()) {
			throw new IOException("Unexpected value length <> 2 lines");
		}
		if (!(valueText.get(0).contains("YES"))) {
			throw new IOException("Bad CRC");
		}
		// 29 is the fixed index of the drivers output
		return Float.parseFloat(valueText.get(1).substring(29)) / 1000;
	}

	/**
	 * 
	 * @param w1_slaveID The id of the sensor to be read
	 * @return The float value of the temperature measurement from the selected one-wire temperature sensor
	 * @throws IOException
	 */
	public static float readDS18B20(String w1_slaveID) throws IOException {
		Path externalTemperaturePath = Paths.get(String.format(w1SlavePattern, w1_slaveID));
		List<String> valueText;
		valueText = Files.readAllLines(externalTemperaturePath);
		if (2 != valueText.size()) {
			throw new IOException("Unexpected value length <> 2 lines");
		}
		if (!(valueText.get(0).contains("YES"))) {
			throw new IOException("Bad CRC");
		}
		// 29 is the fixed index of the drivers output
		return Float.parseFloat(valueText.get(1).substring(29)) / 1000;
	}
}
