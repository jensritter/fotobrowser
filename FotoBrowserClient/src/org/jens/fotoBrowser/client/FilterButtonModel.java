package org.jens.fotoBrowser.client;

import javax.swing.DefaultButtonModel;

import org.jens.fotobrowser.FotoFeed;

public class FilterButtonModel extends DefaultButtonModel {
	private FotoFeed feed;

	public FilterButtonModel(FotoFeed value) {
		feed = value;
	}

	public FotoFeed getFotoFeed() {
		return feed;
	}
}

