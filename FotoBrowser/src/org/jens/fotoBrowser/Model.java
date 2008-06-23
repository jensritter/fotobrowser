package org.jens.fotoBrowser;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javax.imageio.ImageIO;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.swing.ImageIcon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jens.Shorthand.IO;
import org.jens.Shorthand.Wget;
import org.jens.fotoBrowser.model.hibernate.Foto;
import org.jens.fotoBrowser.model.hibernate.FotoFeed;
import org.jens.fotoBrowser.model.hibernate.IgnoredFotos;

@WebService(serviceName="FotoBrowserService")
public class Model {
	private static Log logger = LogFactory.getLog(Model.class);
	private EntityManagerFactory emf;

	public static void main(String[] args) throws URISyntaxException, IOException, JDOMException, ParseException {
		Model a = new Model(null);

		a.update(new ProgressEvent() {
			@Override
			public void info(String value) {}

			@Override
			public void next(String info) {}

			@Override
			public void setMax(int value) {}
		});
	}

	public Model() {
		logger.info("Init Hibernate");
		emf = javax.persistence.Persistence.createEntityManagerFactory("fotobrowser");
	}

	public Model(EntityManagerFactory value) {
		logger.info("Init Hibernate");
		EntityManagerFactory test = value;
		if (test == null) {
			test = javax.persistence.Persistence.createEntityManagerFactory("fotobrowser");
		}
		emf = test;
	}

	public void update(ProgressEvent evt) throws URISyntaxException, IOException, JDOMException, ParseException {
		update("http://www.fotocommunity.de/pc/pc/channel/10/cat/2428"
				,"Die neuesten Bilder aus der Sektion Gothic-Portraits"
				,"http://www.fotocommunity.de/rss/feed/params/Y2hhbm5lbD0xMCZzZWt0aW9uPTI0MjgmZ2VuYnk9LTEmZmNsYW5nPTEmdGl0ZWw9RGllK25ldWVzdGVuK0JpbGRlcithdXMrZGVyK1Nla3Rpb24rR290aGljLVBvcnRyYWl0cw.xml"
				,"Portraits"
				,evt);
		update("http://www.fotocommunity.de/pc/pc/channel/10/cat/5133"
				,"Menschen in mystischer Umgebung"
				,"http://www.fotocommunity.de/rss/feed/params/Y2hhbm5lbD0xMCZzZWt0aW9uPTUxMzMmZ2VuYnk9LTEmZmNsYW5nPTEmdGl0ZWw9RGllK25ldWVzdGVuK0JpbGRlcithdXMrZGVyK1Nla3Rpb24rTWVuc2NoK2luK215c3Rpc2NoZXIrVW1nZWJ1bmc.xml"
				,"Mystische Umgebung"
				,evt);
		update("http://www.fotocommunity.de/pc/pc/cat/5566"
				,"Menschen: Costume Play"
				,"http://www.fotocommunity.de/rss/feed/params/Y2hhbm5lbD0xMCZzZWt0aW9uPTU1NjYmZ2VuYnk9LTEmZmNsYW5nPTEmdGl0ZWw9RGllK25ldWVzdGVuK0JpbGRlcithdXMrZGVyK1Nla3Rpb24rQ29zcGxheQ.xml"
				,"Costume"
				,evt);
		update("http://www.fotocommunity.de/pc/pc/cat/12211"
				,"Menschen: Fantasy-Aufnahmen"
				,"http://www.fotocommunity.de/rss/feed/params/Y2hhbm5lbD0xMCZzZWt0aW9uPTEyMjExJmdlbmJ5PS0xJmZjbGFuZz0xJnRpdGVsPURpZStuZXVlc3RlbitCaWxkZXIrYXVzK2RlcitTZWt0aW9uK0ZhbnRhc3k.xml"
				,"Fantasy"
				,evt);
		update("http://www.fotocommunity.de/pc/pc/cat/10765"
				,"Menschen: Larp, Rollenspiel und Fantasy"
				,"http://www.fotocommunity.de/rss/feed/params/Y2hhbm5lbD0xMCZzZWt0aW9uPTEwNzY1JmdlbmJ5PS0xJmZjbGFuZz0xJnRpdGVsPURpZStuZXVlc3RlbitCaWxkZXIrYXVzK2RlcitTZWt0aW9uK1JvbGxlbnNwaWVs.xml"
				,"Rollenspiel"
				,evt);

		updateAllImages(evt);
	}

	@SuppressWarnings("unchecked")
	public FotoFeed getFeed(String basisUrl, String rssUrl, String desc, String caption) {
		EntityManager em2 = emf.createEntityManager();
		em2.getTransaction().begin();
		Query q = em2.createQuery("from FotoFeed as p where p.rssUrl=?1");
		q.setParameter(1, rssUrl);
		List<FotoFeed> is = q.getResultList();
		FotoFeed feed;
		if (is.size() == 0) {
			logger.info("Unknown Feed - creating new one");
			feed = new FotoFeed();
			feed.setBezeichnung(desc);
			feed.setBasisUrl(basisUrl);
			feed.setRssUrl(rssUrl);
			feed.setCaption(caption);
			em2.persist(feed); em2.flush();
		} else {
			feed = is.get(0);
		}
		em2.getTransaction().commit();
		return feed;
	}

	@SuppressWarnings("unchecked")
	public void update(String basisUrl, String desc, String rssUrl, String caption, ProgressEvent evt) throws URISyntaxException, IOException, JDOMException, ParseException {
		logger.info("update RSS-Feed");
		evt.info("update RSS-Feed");
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();

		FotoFeed feed = getFeed(basisUrl,rssUrl,desc,caption);

		List<Foto> fotos = loadRss(rssUrl, feed);
		Query q = em.createQuery("from Foto as p where imgUrl = ?1");
		Query q2 = em.createQuery("from IgnoredFotos where imgUrl = ?1");
		evt.setMax(fotos.size());
		for(Foto foto : fotos) {
			evt.next("Fetching Thumb "+foto.getThumbUrl());
			try {
				if (q.setParameter(1, foto.getImgUrl()).getSingleResult() != null) {
					// das gibt es schon . . .
					logger.debug("Ignore " + foto.getThumbUrl());
					continue;
				}
			} catch (javax.persistence.NoResultException e) {
				logger.debug("ignoring " + e.getMessage());
			}

			try {
				if (q2.setParameter(1, foto.getImgUrl()).getSingleResult() != null) {
					// das gibt es schon . . .
					logger.debug("Ignore " + foto.getThumbUrl());
					continue;
				}
			} catch (javax.persistence.NoResultException e) {
				logger.debug("ignoring " + e.getMessage());
			}

			logger.info("Fetching Thumb "+foto.getThumbUrl());
			foto.fetchThumb();
			foto.setSeen(false);
			String md5 = foto.calcThumbMd5();
			List<Foto> md5list = getFotoForMD(md5);
			List<IgnoredFotos> md5list2 = getFotoForMDFromIgnored(md5);
			em.persist(foto);

			if (md5list == null) {
				md5list = new ArrayList<Foto>();
			}
			if (md5list2 == null) {
				md5list2 = new ArrayList<IgnoredFotos>();
			}

			if (md5list.size() == 0 && md5list2.size() == 0) {
				continue;
			}
			if (md5list.size() != 0) {
				logger.info("Ignored because of, md5 in currentFotos");
			}
			if (md5list2.size() != 0) {
				logger.info("Ignored because of md5 in ignoredFotots");
			}
			// es gibt bereits irgendwo fotos . . . .
			em.remove(foto); // alles wieder löschen . . .
		}
		em.getTransaction().commit();
	}

	public Foto uploadFoto(File filename, String title) throws IOException {
		byte[] data = IO.getBytesFromFile(filename);
		return uploadFoto(data,title);
	}

	@WebMethod
	public Foto uploadFoto(byte[] value, String title) throws IOException {
		FotoFeed feed = getFeed("manual","manual","ManualFeed","Manual");
		return uploadFoto(feed,value,title);
	}

	public Foto uploadFoto(FotoFeed feed, byte[] value, String title) throws IOException {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Foto neu = new Foto();
		neu.setFeed(feed);

		File manual = File.createTempFile("XXXX","YYYY");
		FileOutputStream out = new FileOutputStream(manual);
		out.write(value);
		out.close();

		// Höhe : 90px f. alle Thumbs !
		BufferedImage img = ImageIO.read(manual);
		int x = img.getWidth();
		int y = img.getHeight();
		double dy = 90F / y;
		int xneu = (int) (x * dy);

		BufferedImage tp = new BufferedImage(xneu,90,BufferedImage.TYPE_INT_RGB);
		Graphics2D canvas = tp.createGraphics();
		canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		AffineTransform tx = new AffineTransform();
		tx.scale(dy, dy);
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
		img= op.filter(img, null);
		canvas.drawImage(new ImageIcon(img).getImage(),0,0,null);
		canvas.dispose();
		ByteArrayOutputStream resultArray = new ByteArrayOutputStream();
		ImageIO.write(tp, "jpeg", resultArray);
		File tmp = File.createTempFile("yyy","xxx");
		out = new FileOutputStream(tmp);
		out.write(resultArray.toByteArray());
		out.close();
		neu.setThumbManual(tmp);

		neu.setImgManual(value);
		neu.setTitle(title);
		neu.setDate(new Date());
		neu.setSeen(true);
		neu.calcThumbMd5();
		neu.setImgUrl(neu.getMd5Thumb());
		em.persist(neu);
		em.getTransaction().commit();
		manual.delete();
		return neu;
	}

	@SuppressWarnings("unchecked")
	@WebMethod
	public List<Foto> getFotoForMD(String md5) {
		EntityManager em = emf.createEntityManager();
		List<Foto> result = em.createQuery("from Foto p where p.md5Thumb = ?1").setParameter(1, md5).getResultList();
		return result;
	}

	@SuppressWarnings("unchecked")
	@WebMethod
	public List<IgnoredFotos> getFotoForMDFromIgnored(String md5) {
		EntityManager em = emf.createEntityManager();
		List<IgnoredFotos> result = em.createQuery("from IgnoredFotos p where p.md5 = ?1").setParameter(1, md5).getResultList();
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<Foto> loadRss(String rssUrl, FotoFeed feed) throws URISyntaxException, IOException, JDOMException, ParseException {
		logger.info("Fetching RSS : " + rssUrl);

		File tmpfile = File.createTempFile("XXXX", "YYYY");
		Wget.get(rssUrl, tmpfile.toString());

		//File tmpfile = new File("feed.xml");
		InputStreamReader in = new InputStreamReader(new FileInputStream(tmpfile),Charset.forName("ISO-8859-1"));
		ArrayList<Foto> fotos = new ArrayList<Foto>();

		Namespace ns = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");


		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(in);

		// Vorsicht, bei DEBUG !!
		in.close();
		tmpfile.delete();

		Element root = doc.getRootElement().getChild("channel");
		List<Element> list = root.getChildren("item");
		for(Element item : list) {
			Foto f = new Foto();
			f.setTitle(item.getChildTextNormalize("title"));

			f.setDate(item.getChildTextNormalize("pubDate"));
			f.setImgUrl(item.getChild("content",ns).getAttributeValue("url"));
			f.setThumbUrl(item.getChild("thumbnail",ns).getAttributeValue("url"));
			f.setFeed(feed);
			fotos.add(f);
		}
		return fotos;
	}


	@SuppressWarnings("unchecked")
	public void updateAllImages(ProgressEvent evt) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Query q = em.createQuery("from Foto as p where p.img is null");
		List<Foto> list = q.getResultList();
		List<Foto> toRemove = new ArrayList<Foto>();
		evt.setMax(list.size());
		for(Foto foto : list) {
			if (foto.getId() == 13100221L) {
				logger.info("MARK");
			}
			try {
				evt.next("Fetching image " + foto.getImgUrl());
				logger.info("Fetching image " + foto.getImgUrl());
				try {
					foto.fetchImg();
					em.persist(foto);
				} catch (IOException e) {
					logger.warn("Error beim Laden -- ignore Image " + foto.getId());
					toRemove.add(foto);
				}
				em.flush();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

		}
		logger.info(list.size());
		em.getTransaction().commit();
		for(Foto foto : toRemove) {
			removeFoto(foto);
		}
	}

	@SuppressWarnings("unchecked")
	@WebMethod
	public List<FotoFeed> getAllFeeds() {
		EntityManager em = emf.createEntityManager();
		Query q = em.createQuery("from FotoFeed as p");
		List<FotoFeed> result = q.getResultList();
		return result;

	}

	@SuppressWarnings("unchecked")
	@WebMethod
	public List<Foto> getAllFotos() {
		EntityManager em = emf.createEntityManager();
		Query q = em.createQuery("from Foto as p order by date desc");
		List<Foto> list = q.getResultList();
		return list;
	}

	@SuppressWarnings("unchecked")
	@WebMethod
	public List<Long> getAllFotoIds() {
		EntityManager em = emf.createEntityManager();
		Query q = em.createQuery("from Foto as p order by date desc");
		List<Foto> list = q.getResultList();

		ArrayList<Long> result = new ArrayList<Long>(list.size());
		for(Foto foto : list) {
			result.add(foto.getId());
		}
		return result;
	}

	@WebMethod
	public byte[] getImgFromFotoById(long id) throws IOException {
		Foto foto = getFotoById(id);
		return foto.getImgAsBytes();
	}

	@WebMethod
	public byte[] getImgFromFotoByIdScaled(long id, int lastWidth, int lastHeight) throws IOException {
		Foto foto = getFotoById(id);
		BufferedImage tp = new BufferedImage(lastWidth,lastHeight,BufferedImage.TYPE_INT_RGB);

		BufferedImage img;
		img = ImageIO.read(foto.getImgAsFile());

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
		img= op.filter(img, null);

		freespaceX = lastWidth - img.getWidth();
		freespaceY = lastHeight - img.getHeight();

		// Centered
		left = left + freespaceX / 2;
		top = freespaceY / 2;

		canvas.drawImage(new ImageIcon(img).getImage(),left,top,null);
		canvas.dispose();

		ByteArrayOutputStream resultArray = new ByteArrayOutputStream();
		ImageIO.write(tp, "jpeg", resultArray);

		return resultArray.toByteArray();
	}

	@WebMethod
	public byte[] getThumbFromFotoById(long id) throws IOException {
		Foto foto = getFotoById(id);
		return foto.getThumbImgAsBytes();
	}

	@WebMethod
	public Foto getFotoById(Long id) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Query q = em.createQuery("from Foto as p where p.id = ?1");
		Foto result = null;
		try {
			q.setParameter(1,id);
			Object res = q.getSingleResult();
			result = (Foto) res;
			 //result = (Foto)q.setParameter(1, id).getSingleResult();
		} catch (javax.persistence.NoResultException e) {
			result = null;
		}
		em.getTransaction().commit();
		return result;
	}

	@WebMethod
	public void removeFoto(Foto foto) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Foto toRemove = (Foto) em.createQuery("from Foto p where id = ?").setParameter(1, foto.getId()).getSingleResult();

		if (toRemove == null) {
			return; // kein Foto zu löschen . . .
		}
		if (!toRemove.isManual()) {
			IgnoredFotos ing = new IgnoredFotos();
			ing.setImgUrl(toRemove.getImgUrl());
			ing.setMd5(toRemove.getMd5Thumb());
			em.persist(ing);
		}
		em.remove(toRemove);
		em.getTransaction().commit();
	}

	public void jarAllFiles(OutputStream stream) throws IOException {
		List<Foto> fotos = getAllFotos();
		buildJar(fotos,stream);
	}

	private void buildJar(List<Foto> fotos, OutputStream stream) throws IOException {
		JarOutputStream jar = new JarOutputStream(stream);

		logger.info("Zipping");
		HashMap<String, String> filenames = new HashMap<String, String>();

		for(Foto foto : fotos) {
			logger.info("Zipping " + foto.getTitle());

			String title = foto.getTitle();
			title = title.replaceAll("\\\\","|");
			title = title.replaceAll("\\/","|");
			title = title.replaceAll("\\~","-");


			for(;filenames.containsKey(title);) {
				title = title + "_";
			}
			filenames.put(title, "");

			title = title + ".jpg";
			jar.putNextEntry(new ZipEntry(title));
			jar.write(foto.getImgAsBytes());

		}
		jar.close();
		logger.info("Zipping done");
	}

	@WebMethod
	public byte[] buildZip(List<Foto> fotos) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		buildJar(fotos,out);
		return out.toByteArray();
	}


	@WebMethod
	public void saveFoto(Foto foto) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.merge(foto);
		//eem.persist(foto);
		em.getTransaction().commit();
	}

}
