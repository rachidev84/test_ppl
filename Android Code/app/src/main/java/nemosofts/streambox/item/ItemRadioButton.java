package nemosofts.streambox.item;

import java.io.Serializable;

public class ItemRadioButton implements Serializable {

	private final int id;
	private final String name;

	public ItemRadioButton(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
