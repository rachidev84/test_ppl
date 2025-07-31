package nemosofts.streambox.item;

import java.io.Serializable;

public class ItemMoviesData implements Serializable {

	private final String streamID;
	private final String name;
	private final String containerExtension;
	Boolean is_download = false;

	public ItemMoviesData(String streamID, String name, String containerExtension) {
		this.streamID = streamID;
		this.name = name;
		this.containerExtension = containerExtension;
	}

	public String getStreamID() {
		return streamID;
	}

	public String getName() {
		return name;
	}

	public String getContainerExtension() {
		return containerExtension;
	}

	public void setDownload(Boolean isDownload) {
		is_download = isDownload;
	}
	public Boolean IsDownload() {
		return is_download;
	}
}
