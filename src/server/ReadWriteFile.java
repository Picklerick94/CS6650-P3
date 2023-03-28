package server;

import java.io.*;
import java.util.*;

/**
 * This class reads and wrties the newly received
 * key value store to txt files according to different
 * request types
 */
public class ReadWriteFile {
	private String fileName;

	public ReadWriteFile(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Write new key value store to txt files
	 * @param key key from client
	 * @param value value from client
	 * @return whether PUT succeeded
	 * @throws IOException exception while accessing txt files
	 */
	public boolean putInStore(String key, String value) throws IOException {
		boolean res = false;

		String file = new File("").getAbsolutePath();
		file = file + File.pathSeparator + fileName;
		File f = new File(file);

		if (!f.exists()) f.createNewFile();

		String line;
		List list = new ArrayList();
		int count = 0;
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

		while ((line = bufferedReader.readLine()) != null) {
			if (line.contains(key)) {
				line += ", " + value;
				count++;
			}
			list.add(line);
		}

		if (count == 0) list.add(key + "=" + value);
		bufferedReader.close();

		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
		Iterator iterator = list.iterator();
		while (iterator.hasNext()) {
			line = (String) iterator.next();
			bufferedWriter.write(line);
			bufferedWriter.newLine();
		}
		bufferedWriter.close();

		String returnValue = getKvStore(key);

		if (returnValue.contains(value)) res = true;

		return res;
	}

	/**
	 * Get key value store from corresponding files
	 * @param key search key
	 * @return key value store
	 * @throws IOException exception while accessing txt files
	 */
	public String getKvStore(String key) throws IOException {
		Properties configProperties = new Properties();
		String file = new File("").getAbsolutePath();
		file = file + File.pathSeparator + fileName;
		File f = new File(file);

		if (!f.exists()) f.createNewFile();

		FileInputStream fileStream = new FileInputStream(file);
		configProperties.load(fileStream);
		String value = configProperties.getProperty(key);
		fileStream.close();

		return value;
	}

	/**
	 * Delete key value store from corresponding files
	 * @param key search key
	 * @return Deletion status
	 * @throws IOException exception while accessing txt files
	 */
	public String deleteKeyValue(String key) throws IOException {
		String value = getKvStore(key);
		String message;

		if (value.isEmpty()) {
			message = "Key not found";
		} else {
			String file = new File("").getAbsolutePath();
			file = file + File.pathSeparator + fileName;
			File f = new File(file);

			if (!f.exists()) f.createNewFile();

			BufferedReader bufferedReader = new BufferedReader(new FileReader(
					file));
			String line;
			List list = new ArrayList();
			while ((line = bufferedReader.readLine()) != null) {
				if (!(line.contains(key))) {
					list.add(line);
				}
			}
			bufferedReader.close();

			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
			Iterator iterator = list.iterator();

			while (iterator.hasNext()) {
				line = (String) iterator.next();
				bufferedWriter.write(line);
				bufferedWriter.newLine();
			}
			bufferedWriter.close();

			message = "Key " + key + " is deleted";
		}

		return message;
	}
}