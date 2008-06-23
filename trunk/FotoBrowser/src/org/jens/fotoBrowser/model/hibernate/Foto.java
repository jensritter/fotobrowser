package org.jens.fotoBrowser.model.hibernate;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PreRemove;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jens.Shorthand.IO;
import org.jens.Shorthand.OS;
import org.jens.Shorthand.Wget;

import com.twmacinta.util.MD5;


@Entity
public class Foto {
	@Transient
	private static Log logger = LogFactory.getLog(Foto.class);
	private Long id;
	private String title; // title
	private Date date; // pubDate
	private FotoFeed feed; // foreignKey
	private int height;
	private int width;
	private String md5Thumb;

	private String thumbUrl; // url media:thumbnail xmlns:media="http://search.yahoo.com/mrss/"
	private String imgUrl; //  url media:content  xmlns:media="http://search.yahoo.com/mrss/"

	private String thumbImg;
	private String img;
	private boolean seen;
	private boolean manual = false;

	public static final String REMOTEURL = "http://www.jens.org:8080/FotoBrowser/";
	private static final SimpleDateFormat FMT = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z",Locale.US);
	private static final SimpleDateFormat FMTSMALL = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");


	@Transient
	public String calcThumbMd5() throws IOException {
		String md = MD5.asHex(MD5.getHash(getThumbAsFile()));
		this.setMd5Thumb(md);
		return md;
	}

	@Transient
	public String getDateAsString() {
		return FMTSMALL.format(date);
	}

	@Transient
	public static final String getBasePath() {
		if (OS.isWindows()) {
			return "x:\\data\\media\\fotocommunity";
		}
		if (OS.isLinux() ) {
			return "/home/ftp/pub/data/media/fotocommunity";
		}
		if (OS.isMac()) {
			return "/tmp/X";
		}
		return null;
	}

	@Transient
	public static File getImgPath() throws IOException {
		File path = new File(getBasePath() + File.separator + "Images");
		if (!path.isDirectory()) {
			if (!path.mkdirs()) {
				throw new IOException("Can't create " + path.getAbsolutePath());
			}
		}
		return path;
	}

	@Transient
	public static File getThumbPath() throws IOException {
		File path = new File(getBasePath() + File.separator + "Thumb");
		if (!path.isDirectory()) {
			if (!path.mkdirs()) {
				throw new IOException("Can't create " + path.getAbsolutePath());
			}
		}
		return path;
	}


	@Transient
	private File buildThumbFile() throws IOException {
		File path = getThumbPath();
		if (id != null) {
			return new File(path.getAbsoluteFile() + File.separator + id + ".jpg");
		} else {
			return File.createTempFile("ThumbImages", ".jpg", path);
		}
	}

	@Transient
	private File buildImgFile() throws IOException {
		File path = getImgPath();

		if (id != null) {
			return new File(path.getAbsoluteFile() + File.separator + id + ".jpg");
		} else {
			return File.createTempFile("TmpImages", ".jpg", path);
		}
	}

	@Transient
	private void atomicRename(File inFile, File outFile) throws IOException {
		if (!inFile.renameTo(outFile)) {
			// versuchen datei zu kopieren . . .
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(inFile));
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
			IO.copyStream(in, out);
			out.close();
			in.close();
			inFile.delete();
		}
	}

	@Transient
	public void fetchImg() throws IOException, URISyntaxException {
		File tmpFile = File.createTempFile("XXX", "yy");
		Wget.get(getImgUrl(),tmpFile.toString());
		BufferedImage testImage = ImageIO.read(tmpFile);
		if (testImage == null) {
			throw new IOException("Leeres Bild");
		}
		this.setHeight(testImage.getHeight());
		this.setWidth(testImage.getWidth());
		File image= getImgAsFile();
		if (image == null) {
			image = buildImgFile();
		}
		atomicRename(tmpFile, image);
		setImg(image.getName());
	}

	@Transient
	public void setImgManual(byte[] imageBytes) throws IOException {
		File tmpFile = File.createTempFile("XXX", "yy");
		FileOutputStream out = new FileOutputStream(tmpFile);
		out.write(imageBytes);
		out.close();
		BufferedImage testImage = ImageIO.read(tmpFile);
		if (testImage == null) {
			throw new IOException("Leeres Bild");
		}
		this.setHeight(testImage.getHeight());
		this.setWidth(testImage.getWidth());
		File image= getImgAsFile();
		if (image == null) {
			image = buildImgFile();
		}
		atomicRename(tmpFile, image);
		setImg(image.getName());
		setManual(true);
	}

	@Transient
	public void fetchThumb() throws IOException, URISyntaxException {
		File tmpFile = File.createTempFile("XXX", "yy");
		Wget.get(getThumbUrl(),tmpFile.getAbsolutePath());
		BufferedImage testImage = ImageIO.read(tmpFile);
		this.setHeight(testImage.getHeight());
		this.setWidth(testImage.getWidth());
		File image= getThumbAsFile();
		if (image == null) {
			image = buildThumbFile();
		}
		atomicRename(tmpFile, image);
		setThumbImg(image.getName());
	}

	@Transient
	public void setThumbManual(File fileToThumb) throws IOException {
		File tmpFile = File.createTempFile("XXX", "yy");
		FileOutputStream out = new FileOutputStream(tmpFile);
		out.write(IO.getBytesFromFile(fileToThumb));
		out.close();
		BufferedImage testImage = ImageIO.read(tmpFile);
		this.setHeight(testImage.getHeight());
		this.setWidth(testImage.getWidth());
		File image= getThumbAsFile();
		if (image == null) {
			image = buildThumbFile();
		}
		atomicRename(tmpFile, image);
		setThumbImg(image.getName());
		setManual(true);
	}

	@Transient
	public void deleteThumb() throws IOException {
		File a = getThumbAsFile();
		if (a != null) {
			a.delete();
		}
		setThumbImg(null);
	}

	@Transient
	public void deleteImg() throws IOException {
		File a = getImgAsFile();
		if (a != null) {
			a.delete();
		}
		setImg(null);
	}


	@Transient
	public InputStream getImgAsStream() throws IOException {
		FileInputStream in = new FileInputStream(getImgAsFile());
		return in;
	}

	@Transient
	public InputStream getThumbImgAsString() throws IOException {
		FileInputStream in = new FileInputStream(getThumbAsFile());
		return in;
	}

	@Transient
	public byte[] getImgAsBytes() throws IOException {
		return IO.getBytesFromFile(buildImgFile());
	}

	@Transient
	public byte[] getThumbImgAsBytes() throws IOException {
		return IO.getBytesFromFile(buildThumbFile());
	}

	@PreRemove
	public void preRemove() {
		try {
			File a = getImgAsFile();
			if (a != null && a.exists()) {
				a.delete();
			}
		} catch (IOException e) {
			logger.warn(this.getId() + " : removeImage " + e.getMessage());
		}
		try {
			File a = getThumbAsFile();
			if (a != null && a.exists()) {
				a.delete();
			}
		} catch (IOException e) {
			logger.warn(this.getId() + " : removeThumb " + e.getMessage());
		}
	}

	@Transient
	public boolean isWidescreen() {
		return this.getWidth() > this.getHeight();
	}

	/*
	@Transient
	public void setImg(InputStream inStream) throws IOException  {
		this.img = IO.getBytesFromStream(inStream,1024);
	}

	@Transient
	public void setThumbImg(InputStream inStream) throws IOException {
		this.thumbImg = IO.getBytesFromStream(inStream, 1024);
	}
	@Transient
	public InputStream getThumbImgAsStream() {
		return new ByteArrayInputStream(thumbImg);
	}

	@Transient
	public InputStream getImgAsStream() {
		return new ByteArrayInputStream(img);
	}
	 */
	public Date getDate() {
		return date;
	}

	public void setDate(String date) throws ParseException {
		this.date = FMT.parse(date);
	}

	public void setDate(Date value) {
		this.date = value;
	}

	@Id
	@GeneratedValue()
	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
		/* Rename the TMP-Files !!*/
		if (img != null) {
			try {
				File neu = new File(getImgPath() + File.separator + id + ".jpg");
				File alt = getImgAsFile();
				atomicRename(alt, neu);
				setImg(neu.getName());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (thumbImg != null) {
			File neu;
			try {
				neu = new File(getThumbPath() + File.separator + id + ".jpg");
				File alt = getThumbAsFile();
				atomicRename(alt, neu);
				setThumbImg(neu.getName());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@Transient
	public File getImgAsFile() throws IOException {
		if (img == null) { return null; }
		return new File(getImgPath().getAbsolutePath() + File.separator + img);
	}

	@Transient
	public File getThumbAsFile() throws IOException {
		if (thumbImg == null) { return null; }
		return new File(getThumbPath().getAbsoluteFile() + File.separator + thumbImg);
	}


	@Transient
	public URL getThumbAsUrl() throws MalformedURLException {
		if (thumbImg == null) { return null; }
		// http://www.jens.org:8080/FotoBrowser/view?id=11707
		// thumb?id=12244
		URL url =  new URL(REMOTEURL + "thumb?id=" + getId());
		//System.out.println(url);
		return url;
	}

	@Transient
	public URL getImgAsUrl() throws MalformedURLException {
		if (img== null) { return null; }
		 // http://www.jens.org:8080/FotoBrowser/view?id=11707
		URL url =  new URL(REMOTEURL + "view?id=" + getId());
		//System.out.println(url);
		return url;
	}


	public String getTitle() {
		return title;
	}


	public void setTitle(String title) {
		this.title = title;
	}


	public String getThumbUrl() {
		return thumbUrl;
	}


	public void setThumbUrl(String thumbUrl) {
		this.thumbUrl = thumbUrl;
	}


	public String getImgUrl() {
		return imgUrl;
	}


	public void setImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
	}

	@ManyToOne
	public FotoFeed getFeed() {
		return feed;
	}

	public void setFeed(FotoFeed feed) {
		this.feed = feed;
	}

	public String getThumbImg() {
		return thumbImg;
	}



	public void setThumbImg(String thumbImg) {
		this.thumbImg = thumbImg;
	}



	public String getImg() {
		return img;
	}



	public void setImg(String img) {
		this.img = img;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public String getMd5Thumb() {
		return md5Thumb;
	}

	public void setMd5Thumb(String md5Thumb) {
		this.md5Thumb = md5Thumb;
	}

	public void setSeen(boolean value) {
		this.seen = value;
	}

	public boolean isSeen() {
		return seen;
	}

	public boolean isManual() {
		return manual;
	}

	public void setManual(boolean manual) {
		this.manual = manual;
	}

}
