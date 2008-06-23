package org.jens.fotoBrowser.client;

import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jens.fotobrowser.Foto;
import org.jens.fotobrowser.Model;

public class ThumbRenderer implements  ListCellRenderer {

	private static Log logger = LogFactory.getLog(ThumbRenderer.class);

	//JLabel canvas;
	Model model;

	private FileCache imgCache;

	public ThumbRenderer(Model mdl, EventInterface evt) {
		model = mdl;
		try {
			imgCache = new FileCache();
		} catch (IOException e) {
			e.printStackTrace();
			evt.doEvent(e.getMessage());
		}
	}

	public void addThumb(long id, byte[] ico) {
		imgCache.put(id,new ImageIcon(ico));
	}

	public void saveCache(List<Foto> fotos) throws IOException {
		List<Long> ids = new ArrayList<Long>();
		for(Foto foto : fotos) {
			ids.add(foto.getId());
		}
		imgCache.saveCache(ids);
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		if (value instanceof Foto && value != null) {
			JLabel canvas = new JLabel();
			if (isSelected) {
				canvas.setBackground(// BugFix: Numbus
						new Color(
								javax.swing.UIManager.getDefaults().getColor("textHighlight").getRGB()
						));
			} else {
				canvas.setBackground(javax.swing.UIManager.getDefaults().getColor("text"));
			}

			canvas.setOpaque(true);
			canvas.setText("");
			canvas.setHorizontalAlignment(SwingConstants.CENTER);
			Foto foto = (Foto) value;
			ImageIcon ico = imgCache.get(foto.getId());
			if (ico == null) {
				/*try {
					logger.debug("Fetching THUMB from WebService");
					listener.doEvent("Fetching Image from WebService");
					ico = new ImageIcon(model.getThumbFromFotoById(foto.getId()));
					imgCache.put(foto.getId(), ico);
					listener.doEvent("");
				} catch (IOException_Exception e) {
					e.printStackTrace();
					System.out.println(e.getMessage());
				}
				 */
				canvas.setText("No photo yet");
				canvas.setIcon(null);
				return canvas;
			}
			canvas.setIcon(ico);
			return canvas;
		}
		logger.info("NULL OBJECT IN LISTVIEW");
		return null;
	}

	public boolean hasThumb(Long id) {
		return imgCache.containsKey(id);
	}

}
