package org.jens.fotoBrowser.client;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PanelImage extends JLabel {
	private static Log logger = LogFactory.getLog(PanelImage.class);

	private byte[] ico = null;
	private ImageIcon image = null;
	private int lastWidth = -1;
	private int lastHeight = -1;
	private EventInterface listener = null;
	private boolean resize=false;


	public PanelImage(EventInterface evt) {
		listener = evt;

		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent evt) {
				reinit();
			}

			/*@Override
			public void componentShown(ComponentEvent evt) {
				reinit();
			}*/
		});
		setDoubleBuffered(true);
		setVerticalAlignment(SwingConstants.TOP);
	}

	private void reinit() {
		if (image == null || lastHeight != getHeight() || lastWidth != getWidth()) {
			recalc();
		}
		this.setIcon(image);
	}

	public void setImage(byte[] value) {
		if (ico == value) {
			// Noop
			return;
		}
		ico = value;
		image = null;
		lastWidth = -1;
		lastHeight = -1;
		this.reinit();
	}

	public ImageIcon getImage() {
		return image;
	}

	/*@Override
	public void paintComponent(Graphics g)  {
		if (offScreen==null) {
			reinit();
		}
		if (image == null || lastHeight != getHeight() || lastWidth != getWidth()) {
			recalc();
		}
		offScreenBuffer.clearRect(0, 0, getWidth(), getWidth());
		if (image != null) {
			offScreenBuffer.drawImage(image,0,0, image.getWidth(this),image.getHeight(this),this);
		}
		g.drawImage(offScreen,0,0,this);
	}*/

	private void recalc()  {
		if (ico == null || ico.length == 0) {
			return; // noch kein bild
		}
		listener.doEvent("ReSize Image");
		if (!resize) {
			logger.debug("NO RESIZE");
			image = new ImageIcon(ico);
			lastHeight = getHeight();
			lastWidth = getWidth();
			listener.doEvent("");
			return;
		}
		try {
			lastHeight = getHeight();
			lastWidth = getWidth();
			BufferedImage tp = new BufferedImage(lastWidth,lastHeight,BufferedImage.TYPE_INT_RGB);

			BufferedImage img;
			ByteArrayInputStream in = new ByteArrayInputStream(ico);
			img = ImageIO.read(in);

			Graphics2D canvas = tp.createGraphics();
			canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

			int left = 0;
			int freespaceX = lastWidth - img.getWidth();

			int top = 0;
			int freespaceY = lastHeight - img.getHeight();

			AffineTransform tx = new AffineTransform();

			double scale=0D;
			if (freespaceX< freespaceY) {
				scale = (double) (img.getWidth() +freespaceX) / img.getWidth();
			} else {
				scale = (double) (img.getHeight()+freespaceY) / img.getHeight();
			}

			tx.scale(scale, scale);
			AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
			try {
				img= op.filter(img, null);
			} catch (RasterFormatException e) {
				System.out.println(img.getWidth());
				System.out.println(img.getHeight());
				System.out.println(lastHeight);
				System.out.println(lastWidth);
				System.out.println(getHeight());
				System.out.println(getWidth());
				System.out.println(scale);
				System.exit(0);
			}

			freespaceX = lastWidth - img.getWidth();
			freespaceY = lastHeight - img.getHeight();

			// Centered
			left = left + freespaceX / 2;
			top = freespaceY / 2;

			canvas.drawImage(new ImageIcon(img).getImage(),left,top,null);
			canvas.dispose();

			ByteArrayOutputStream resultArray = new ByteArrayOutputStream();
			ImageIO.write(tp, "jpeg", resultArray);
			image = new ImageIcon(resultArray.toByteArray());
			listener.doEvent("");
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

	}

	public void setResize(boolean selected) {
		this.resize= selected;
		recalc();
		reinit();
	}

	public boolean isResize() {
		return resize;
	}

}


