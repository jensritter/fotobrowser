package org.jens.fotoBrowser.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileCache extends HashMap<Long, ImageIcon> implements Serializable {
	private static Log logger = LogFactory.getLog(FileCache.class);
	private static final long serialVersionUID = 1L;


	String dir;
	File cacheFile;
	public FileCache() throws IOException {
		super();
		dir = System.getProperty("user.home") + File.separator + ".FotoBrowserClient" + File.separator;
		File test = new File(dir);
		if (!test.exists()) {
			test.mkdir();
		}
		cacheFile = new File(dir + "imageCache");
		if (cacheFile.exists()) {
			loadCache();
		}
	}

	@SuppressWarnings("unchecked")
	public void loadCache() throws IOException{
		logger.info("Load Images from Cache");
		try {
			ObjectInputStream in = new ObjectInputStream( new FileInputStream(cacheFile));
			HashMap<Long, ImageIcon> fileData = (HashMap<Long, ImageIcon>) in.readObject();
			in.close();

			this.clear();
			for(Map.Entry<Long, ImageIcon> item : fileData.entrySet()) {
				this.put(item.getKey(), item.getValue());
			}
		} catch (FileNotFoundException e) {
			throw new IOException(e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			cacheFile.delete();
			return;
		}
	}

	public void saveCache(List<Long> ids) throws IOException {

		HashMap<Long, Boolean> unused = new HashMap<Long, Boolean>();
		for(Long id : this.keySet()) {
			unused.put(id,true);
		}
		for(Long id : ids) {
			unused.remove(id);
		}
		for(Long id : unused.keySet()) {
			this.remove(id);
		}

		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cacheFile));
		out.writeObject(this);
		out.close();
	}

	public void removeCache() {
		cacheFile.delete();
	}
}
