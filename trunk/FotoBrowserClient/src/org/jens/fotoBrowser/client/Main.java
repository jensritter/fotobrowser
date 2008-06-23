package org.jens.fotoBrowser.client;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Dialog.ModalExclusionType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jens.Shorthand.IO;
import org.jens.fotobrowser.Foto;
import org.jens.fotobrowser.FotoBrowserService;
import org.jens.fotobrowser.FotoFeed;
import org.jens.fotobrowser.IOException_Exception;
import org.jens.fotobrowser.Model;

public class Main extends NetbeansView implements EventInterface{

	protected static final String CONFIG_WANTRESIZEOUTPUT = "wantResizedFileOutput";
	protected static final String CONFIG_MANUALSIZE = "manualSize";
	protected static final String CONFIG_MANUALHEIGHT = "manuelHeight";
	protected static final String CONFIG_MANUALWIDTH = "manualWidth";
	protected static final String CONFIG_SELECTEDSIZE="selectedSize";
	protected static final String CONFIG_VIEWRESIZED = "viewResized";

	private static Log logger = LogFactory.getLog(Main.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		boolean ok = false;
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			ok = true;
		} catch (ClassNotFoundException e1) {
			ok = false;
		} catch (InstantiationException e1) {
			ok = false;
		} catch (IllegalAccessException e1) {
			ok = false;
		} catch (UnsupportedLookAndFeelException e1) {
			ok = false;
		}
		try {
			if (!ok) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new Main().setVisible(true);
			}
		});
	}

	Model model;
	PanelImage panelImage;
	HashMap<Long, byte[]> imageCache = new HashMap<Long, byte[]>();
	Main globalThis;
	JFileChooser fc = null;
	ResourceBundle bundle = ResourceBundle.getBundle("org/jens/fotoBrowser/client/i18n"); // NOI18N
	protected Preferences userConfig;
	boolean blocked=false;
	ThumbRenderer renderer;
	HashMap<String, Boolean> filter;

	public Main() {
		super();
		userConfig=Preferences.userNodeForPackage(this.getClass());
		globalThis = this;

		txtStatus.setText("Init");

		logger.info("WS: Init");
		FotoBrowserService service = new FotoBrowserService();
		model = service.getModelPort();
		logger.info("WS: Done");
		panelImage = new PanelImage(globalThis);
		panelImage.setComponentPopupMenu(listThumbs.getComponentPopupMenu());
		panelmageBackground.add(panelImage,BorderLayout.CENTER);

		/* Maus + Tastatur Capture */
		panelImage.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent evt) {
				panelImageUserMakesABlip(evt);
			}
		});

		panelImage.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getButton() == 1) {
					KeyEvent evt2 = new KeyEvent(evt.getComponent(), WIDTH, 0L, 0, 0, '0');
					panelImageUserMakesABlip(evt2);
				}
			}
		});

		panelImage.addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e){
				if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
					if (e.getUnitsToScroll() < 0 ) {
						hopPrev();
					} else {
						hopNext();
					}
				}
			}
		});

		DefaultListModel mdl = new DefaultListModel();
		renderer = new ThumbRenderer(model,globalThis);
		listThumbs.setCellRenderer(renderer);
		listThumbs.setModel(mdl);

		txtStatus.setText("");

		checkWantAutoResizeAsSaved.getModel().setSelected(userConfig.getBoolean(CONFIG_WANTRESIZEOUTPUT, false));
		checkManualSize.getModel().setSelected(userConfig.getBoolean(CONFIG_MANUALSIZE,false));
		if (checkManualSize.isEnabled()) {
			// manuelle Größe :
			txtHeight.setText(userConfig.get(CONFIG_MANUALHEIGHT, "1024"));
			txtWidth.setText(userConfig.get(CONFIG_MANUALWIDTH, "800"));
		} else {
			comboSized.setSelectedIndex(userConfig.getInt(CONFIG_SELECTEDSIZE, 1));
		}
		toggleAllPref();
		onToggleResize(userConfig.getBoolean(CONFIG_VIEWRESIZED, false));

		int toolbari = toolbar.getComponentIndex(separatorCheckboxes) +1 ;
		filter = new HashMap<String, Boolean>();
		for(FotoFeed feed : model.getAllFeeds()) {
			JCheckBox box = new JCheckBox(feed.getCaption());
			filter.put(feed.getRssUrl(), true);
			box.setModel(new FilterButtonModel(feed));
			box.setSelected(true);
			box.setFocusable(false);
			box.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JCheckBox box = (JCheckBox)e.getSource();
					box.getModel().setSelected(!box.getModel().isSelected());
					onAlterFilter((JCheckBox)e.getSource());
				}

			});
			toolbar.add(box, toolbari);
		}
		panelImage.requestFocusInWindow(); 
		this.pack();

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				logger.info("Wait");
				Thread.sleep(500);
				logger.info("Wait done");
				loadAllFotosToList();
				return null;
			}

			@Override
			public void done() {
				try {
					get();
				} catch (InterruptedException e) {
					e.printStackTrace();
					fehler(e.getMessage());
				} catch (ExecutionException e) {
					e.printStackTrace();
					fehler(e.getMessage());
				}
			}

		};
		worker.execute();
	}

	private void loadAllFotosToList() throws IOException{
		mntReload.setEnabled(false);
		openMenuItem.setEnabled(false);
		logger.info("Query Fotos");
		DefaultListModel md = new DefaultListModel();
		List<Foto> fotos = model.getAllFotos();


		int counter=0;
		for(Foto foto : fotos) {
			counter++;
			try {
				if (!renderer.hasThumb(foto.getId())) {
					renderer.addThumb(foto.getId(),model.getThumbFromFotoById(foto.getId()));
				} else {
				}
				if (filter.get(foto.getFeed().getRssUrl()).booleanValue()) {
					md.addElement(foto);
				}

				/*if (counter == 1) {
					listThumbs.setSelectedIndex(0);
				}*/
			} catch (IOException_Exception e) {
				throw new IOException(e.getMessage());
			}
			globalThis.doEvent("" + counter + "/" + fotos.size());
		}
		listThumbs.setModel(md);
		this.setTitle(bundle.getString("FotoBrowser") + " (" + md.size() + ")");
		renderer.saveCache(fotos);
		mntReload.setEnabled(true);
		openMenuItem.setEnabled(true);
		listThumbs.setSelectedIndex(0);
		listThumbs.updateUI();
	}

	private void updateImagePanel() {
		if (blocked) {
			return; // nix da !
		}
		SwingWorker<Foto, Void> worker = new SwingWorker<Foto, Void>() {

			@Override
			protected Foto doInBackground() throws Exception {
				blocked=true;
				Foto foto = (Foto)listThumbs.getSelectedValue();
				if (foto == null) {
					panelImage.setImage(null);
					labelName.setText("");
					labelInfo.setText("");
					return null;
				}
				byte[] img = imageCache.get(foto.getId());
				try {
					if (img == null) {
						logger.debug("Fetching IMG");
						img = model.getImgFromFotoById(foto.getId());
						if (img == null) {
							throw new FileNotFoundException("" + foto.getId());
						}
						imageCache.put(foto.getId(), img);
						// detached fotos können noh nicht gespeichert werden . ..
						// foto.setSeen(true);
						//model.saveFoto(foto);
					}
					panelImage.setImage(img);
					labelName.setText(foto.getTitle());
					String txt = foto.getWidth() + "x" + foto.getHeight();
					if (checkResize.getModel().isSelected()) {
						txt = txt + " (" + panelImage.getWidth() + "x" + panelImage.getHeight() + " )";
					}
					labelInfo.setText(txt);
				} catch (IOException_Exception e) {
					e.printStackTrace();
					fehler(e.getMessage());
					panelImage.setImage(null);
					labelName.setText(e.getMessage());
					labelInfo.setText("");
				}

				return foto;
			}

			@Override
			public void done() {
				try {
					blocked=false;
					Foto oldFoto = get();

					panelImage.setCursor(Cursor.getDefaultCursor());
					if (oldFoto != null) {
						Foto foto = (Foto)listThumbs.getSelectedValue();
						if (!foto.getId().equals(oldFoto.getId())) {
							logger.info("Nachsitzen "+ foto.getId());
							// Es hat sich in der zwischenzeit was getan -- also nochmal . . .
							updateImagePanel();
						}
					}
				} catch (InterruptedException e) {
					System.out.println("X");
					fehler(e.getMessage());
					e.printStackTrace();
				} catch (ExecutionException e) {
					System.out.println("Y");
					fehler(e.getMessage());
					e.printStackTrace();
				}


			}
		};
		worker.execute();
	}

	private void fehler(String msg) {
		JOptionPane.showMessageDialog(this, msg);
	}

	public void onAlterFilter(JCheckBox box) {
		FilterButtonModel md = (FilterButtonModel) box.getModel();
		filter.put(md.getFotoFeed().getRssUrl(),md.isSelected());
		doReload();
	}

	@Override
	protected void onChangeListItem(ListSelectionEvent evt) {
		if (evt.getValueIsAdjusting()) {
			return;
		}
		updateImagePanel();
	}

	@Override
	public void doEvent(String msg) {
		panelImage.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		txtStatus.setText(msg);
		if (msg.equals("")) {
			panelImage.setCursor(Cursor.getDefaultCursor());
		}
	}

	private JFileChooser getFC() {
		if (fc == null) {
			fc = new JFileChooser();
		}
		fc.resetChoosableFileFilters();
		fc.setAcceptAllFileFilterUsed(true);
		fc.addChoosableFileFilter(new FileNameExtensionFilter("JPEG file", "jpg", "jpeg"));

		return fc;
	}

	private void hopNext() {
		DefaultListModel mdl = (DefaultListModel) listThumbs.getModel();
		int id = listThumbs.getSelectedIndex();
		id++;
		if (id>= mdl.getSize()) {
			id = mdl.getSize()-1;
			if (id == 0) {
				logger.fatal("WAS IST HIER ? ");
				// ??
			}
		}
		listThumbs.setSelectedIndex(id);
		Rectangle scrollPos = listThumbs.getCellBounds(id, id);
		listThumbs.scrollRectToVisible(scrollPos);
		updateImagePanel();
	}

	private void hopPrev() {
		int id = listThumbs.getSelectedIndex();
		id--;
		if (id<0) {
			id = 0;
		}
		listThumbs.setSelectedIndex(id);
		Rectangle scrollPos = listThumbs.getCellBounds(id, id);
		listThumbs.scrollRectToVisible(scrollPos);
		updateImagePanel();
	}

	private static final int CURSOR_RIGHT = 39;
	private static final int CURSOR_LEFT = 37;
	//private static final int CURSOR_UP = 38;
	//private static final int CURSOR_DOWN=40;
	private static final int SPACE = 32;
	private static final int MOUSE_CLICK1=0;
	@Override
	protected void panelImageUserMakesABlip(KeyEvent evt) {
		//System.out.println(evt.getKeyCode());
		// || evt.getKeyCode() == CURSOR_DOWN
		// || evt.getKeyCode() == CURSOR_UP
		// Werden schon durch die List-Box abgefangen . . .
		if (evt.getKeyCode() == SPACE || evt.getKeyCode() == MOUSE_CLICK1 || evt.getKeyCode() == CURSOR_RIGHT ) {
			hopNext();
		}
		if (evt.getKeyCode() == CURSOR_LEFT ) {
			hopPrev();
		}
	}

	@Override
	protected void onToggleResize(boolean value) {
		checkResize.getModel().setSelected(value);
		mntCheckAutoResize.getModel().setSelected(value);
		mntCheckAutoResize2.getModel().setSelected(value);
		panelImage.setResize(value);
		userConfig.putBoolean(CONFIG_VIEWRESIZED, value);
	}

	@Override
	protected void doSaveAs() {
		Foto foto = (Foto) listThumbs.getSelectedValue();
		String filename = foto.getTitle();
		filename = filename + ".jpg";
		getFC().setSelectedFile(new File(filename));


		if ( getFC().showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File datei = fc.getSelectedFile();

		try {
			FileOutputStream out = new FileOutputStream(datei);
			if (userConfig.getBoolean(CONFIG_WANTRESIZEOUTPUT, false)) {
				int x = 0;
				int y = 0;
				if (userConfig.getBoolean(CONFIG_MANUALSIZE, false)) {
					x = userConfig.getInt(CONFIG_MANUALWIDTH, 100);
					y = userConfig.getInt(CONFIG_MANUALHEIGHT, 100);
				} else {
					String item = (String)comboSized.getSelectedItem();
					String[] a = item.split("x");
					x = Integer.parseInt(a[0]);
					y = Integer.parseInt(a[1]);
				}
				out.write(model.getImgFromFotoByIdScaled(foto.getId(),x,y));
				JOptionPane.showMessageDialog(this, "Scaled Image saved");
			} else {
				out.write(model.getImgFromFotoById(foto.getId()));
				JOptionPane.showMessageDialog(this, "Original Saved");
			}




			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			fehler(e.getMessage());
		} catch (IOException_Exception e) {
			e.printStackTrace();
			fehler(e.getMessage());
		}
	}

	@Override
	protected void doDeleteThis() {
		Foto foto = (Foto) listThumbs.getSelectedValue();
		hopNext();
		DefaultListModel md = (DefaultListModel) listThumbs.getModel();
		md.removeElement(foto);
		model.removeFoto(foto);
	}

	@Override
	protected void updateOutputSize() {
		userConfig.putBoolean(CONFIG_WANTRESIZEOUTPUT, checkWantAutoResizeAsSaved.getModel().isSelected());
		String text = bundle.getString("Save_As")+ " ";
		if (checkWantAutoResizeAsSaved.getModel().isSelected()) {
			text = text + bundle.getString("(Scaled)");
			userConfig.putBoolean(CONFIG_MANUALSIZE, checkManualSize.getModel().isSelected());
			if (!checkManualSize.getModel().isSelected()) {
				userConfig.putInt(CONFIG_SELECTEDSIZE, comboSized.getSelectedIndex());
			} else {
				userConfig.putInt(CONFIG_MANUALHEIGHT, Integer.parseInt(txtHeight.getText()));
				userConfig.putInt(CONFIG_MANUALWIDTH, Integer.parseInt(txtWidth.getText()));
			}
		}
		mnSaveAs.setText(text);
		btnSaveAs.setText(text);
	}

	@Override
	protected void doUpload() {
		txtUpload.setText("");
		labelPreview.setIcon(null);
		txtTitle.setText("");
		dlgUpload.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
		dlgUpload.pack();
		dlgUpload.setVisible(true);

	}

	@Override
	protected void doSelectUploadFile() {
		JFileChooser fc = getFC();
		if ( getFC().showOpenDialog(dlgUpload) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File datei = fc.getSelectedFile();
		txtUpload.setText(datei.toString());

		if (txtTitle.getText().equals("")) {
			txtTitle.setText(datei.getName().toString());
		}
		ImageIcon ico = new ImageIcon(datei.toString());
		int x = ico.getIconWidth();
		int y = ico.getIconHeight();
		Float dy = 200F / y;
		x = (int) (x * dy);
		labelPreview.setIcon(new ImageIcon(ico.getImage().getScaledInstance(x, 200, java.awt.Image.SCALE_FAST)));
		labelPreview.setText("");
	}

	@Override
	protected void doUploadDone() {
		File file = new File(txtUpload.getText());
		String title = txtTitle.getText();
		if (title.equals("")) {
			fehler(bundle.getString("Enter_a_title"));
			return;
		}
		if (!file.exists()) {
			fehler(bundle.getString("No_such_file") + " : " + file.toString());
			return;
		}
		dlgUpload.setVisible(false);
		try {
			byte[] data = IO.getBytesFromFile(file);
			Foto f = model.uploadFoto(data,title);
			renderer.addThumb(f.getId(),model.getThumbFromFotoById(f.getId()));

			DefaultListModel m = (DefaultListModel) listThumbs.getModel();
			DefaultListModel m2 = new DefaultListModel();
			m2.addElement(model.getFotoById(f.getId()));
			for(int i = 0; i < m.getSize(); i++) {
				m2.addElement(m.get(i));
			}
			listThumbs.setModel(m2);
			listThumbs.setSelectedIndex(0);
		} catch (IOException e) {
			e.printStackTrace();
			fehler(e.getMessage());
		} catch (IOException_Exception e) {
			e.printStackTrace();
			fehler(e.getMessage());
		}
	}

	@Override
	protected void doReload() {
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				loadAllFotosToList();
				return null;
			}

			@Override
			public void done() {
				try {
					get();
				} catch (InterruptedException e) {
					fehler(e.getMessage());
				} catch (ExecutionException e) {
					fehler(e.getMessage());
				}
			}
		};
		worker.execute();
	}

	@Override
	protected void doSaveAll() {
		JFileChooser fc = getFC();
		fc.addChoosableFileFilter(new FileNameExtensionFilter("ComicBook", "cbz"));
		if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File output = fc.getSelectedFile();
		List<Foto> fotos = new ArrayList<Foto>();
		for(int i = 0; i< listThumbs.getModel().getSize(); i++) {
			fotos.add((Foto)listThumbs.getModel().getElementAt(i));
		}
		try {
			FileOutputStream out = new FileOutputStream(output);
			doEvent("Packing");
			Thread.sleep(100);
			out.write(model.buildZip(fotos));
			out.close();
			doEvent("");
			JOptionPane.showMessageDialog(this, "All Saved at " + output.toString());
		} catch (IOException e) {
			e.printStackTrace();
			fehler(e.getMessage());
		} catch (IOException_Exception e) {
			e.printStackTrace();
			fehler(e.getMessage());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
