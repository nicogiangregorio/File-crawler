package it.nicogiangregorio.filecrawler;

/**
 * 
 * Class representing a pair [file path - hash]
 * 
 * @author Nico Giangregorio
 * 
 */
public final class FileEntry implements Comparable<FileEntry> {

	private final String hashCode;
	private final String filePath;

	public FileEntry(String filePath, String hashCode) {

		this.filePath = filePath;
		this.hashCode = hashCode;
	}

	public String getHashCode() {
		return hashCode;
	}

	public String getFilePath() {
		return filePath;
	}

	@Override
	public String toString() {
		return "{hashcode: " + hashCode + ",filepath : " + filePath + "}";
	}

	@Override
	public int compareTo(FileEntry entry) {
		return String.CASE_INSENSITIVE_ORDER.compare(filePath, entry.filePath);
	}
}
