package nemosofts.streambox.item;

import java.io.Serializable;

public class ItemEpgFull implements Serializable {

	private final String id;
	private final String start;
	private final String end;
	private final String title;
	private final String description;
	private final String start_timestamp;
	private final String stop_timestamp;
	private final int now_playing;
	private final int has_archive;

    public ItemEpgFull(String id, String start, String end, String title, String description, String startTimestamp, String stopTimestamp, int nowPlaying, int hasArchive) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.title = title;
        this.description = description;
		this.start_timestamp = startTimestamp;
		this.stop_timestamp = stopTimestamp;
		this.now_playing = nowPlaying;
		this.has_archive = hasArchive;
    }

	public String getId() {
		return id;
	}

	public String getStart() {
		return start;
	}

	public String getEnd() {
		return end;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getStartTimestamp() {
		return start_timestamp;
	}

	public String getStopTimestamp() {
		return stop_timestamp;
	}

	public int getNowPlaying() {
		return now_playing;
	}

	public int getHasArchive() {
		return has_archive;
	}
}
