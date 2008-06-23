package org.jens.fotoBrowser;

public interface ProgressEvent {
	void next(String info);
	void setMax(int value);
	void info(String value);

}
