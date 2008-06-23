package org.jens.fotoBrowser;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.ImageIcon;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.JDOMException;
import org.jens.Shorthand.xml.SimpleRss;
import org.jens.Shorthand.xml.SimpleRssItem;
import org.jens.fotoBrowser.model.hibernate.Foto;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;

/**
 * Servlet implementation class for Servlet: ImageServlet
 *
 */
public class ImageServlet extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet, Job {
	static final long serialVersionUID = 1L;
	static final int SCREENX2=2560;
	static final int SCREENX=1280;
	static final int SCREENY=1024;

	private static Log logger = LogFactory.getLog(Model.class);
	private Model model;
	Random r;
	@PersistenceUnit
	private EntityManagerFactory factory=Persistence.createEntityManagerFactory("fotobrowser");

	public ImageServlet() {
		super();
		logger.info("Init Hibernate");
		model = new Model(factory);
		r = new Random((new Date()).getTime());
	}

	@Override
	public void init() {
		r = new Random((new Date()).getTime());
		try {
			SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
			Scheduler sched = schedFact.getScheduler();
			sched.start();
			JobDetail testJob = sched.getJobDetail("updateRss", "defaultGroup");
			if (testJob != null) {
				log("updateRss");
			} else {
				JobDetail jobDetail = new JobDetail("updateRss",
						"defaultGroup",
						ImageServlet.class);
				Trigger trigger = TriggerUtils.makeHourlyTrigger(2);
				trigger.setStartTime(new Date());
				trigger.setName("updateRss");
				sched.scheduleJob(jobDetail, trigger);
			}
		} catch (SchedulerException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}


	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
			model.update(new ProgressEvent() {
				public void info(String value) {}
				public void next(String info) {}
				public void setMax(int value) {}
			});
		} catch (URISyntaxException e) {
			throw new JobExecutionException(e.getCause());
		} catch (IOException e) {
			throw new JobExecutionException(e.getCause());
		} catch (JDOMException e) {
			throw new JobExecutionException(e.getCause());
		} catch (ParseException e) {
			throw new JobExecutionException(e.getCause());
		}

	}


	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


		// fullView
		String test = request.getParameter("id");
		Long id = 0L;
		if (test != null) {
			id = Long.parseLong(test);
		}

		String url  = request.getRequestURL().toString();

		String rssUrl = url;
		rssUrl = rssUrl.substring(0,rssUrl.lastIndexOf('/')) + "/rss";

		String rssTxt = "<link rel=\"alternate\" type=\"application/rss+xml\" title=\"Alle Fotos\" href=\""+rssUrl+"\" />";

		if (url.endsWith("thumb")) {
			//logger.info("view-thumb");
			Foto foto = model.getFotoById(id);
			if (foto == null) {
				return;
			}
			response.setContentType("image/jpeg");
			OutputStream out = response.getOutputStream();

			out.write(foto.getThumbImgAsBytes());
			return;
		}
		if (url.endsWith("viewHTML")) {
			Foto foto = model.getFotoById(id);
			if (foto == null) {
				return;
			}
			PrintWriter out = response.getWriter();
			out.println("<html>");
			out.println("<head>"+rssTxt+"</head>");
			out.println("<body style='background-color:black;'>");
			out.println("<table border='0' width='100%'><tr>");
			out.println("<td><a href='delete?id=" + foto.getId() + "'><img border='0' src='images/delete.gif'></a></td>");
			out.println("<td><a href='viewscaled.jpg?id=" + foto.getId() + "'><img border='0' src='images/view-fullscreen.png'></a></td>");
			out.println("</tr></table>");
			out.println("<a href='view?download=1&id=" + id + "'>");
			out.println("<img border='0' src='view?id=" + id + "'><br>");
			out.println("</a>");
			out.println("</body></html>");
			return;
		}

		if (url.endsWith("view")) {
			//logger.info("view-view");
			Foto foto = model.getFotoById(id);
			if (request.getParameter("download") != null) {
				response.setHeader("Content-Disposition", "attachment; filename=\"" + foto.getTitle() + "\"");
			}
			if (foto == null) {
				return;
			}
			OutputStream out = response.getOutputStream();
			response.setContentType("image/jpeg");
			byte[] res = foto.getImgAsBytes();
			if (res == null) {
				try {
					foto.fetchImg();
					model.saveFoto(foto);
				} catch (URISyntaxException e) {
					e.printStackTrace();
					response.getWriter().print("Fehler beim download<br>\n");
					response.getWriter().print(e.getMessage());
					return;
				}
			}
			out.write(foto.getImgAsBytes());
			return;
		}
		if (url.endsWith("viewscaled.jpg")) {
			Foto foto = model.getFotoById(id);
			getPictureScaled(response, foto, SCREENX, SCREENY);
		}

		if (url.endsWith("grid")) {
			displayGrid(response, rssTxt);
			return;
		}
		if (url.endsWith("delete")) {
			Foto foto = model.getFotoById(id);

			if (foto != null) {
				model.removeFoto(foto);
			}
			if (request.getParameter("prg") == null) {
				response.sendRedirect("grid");
			}
			PrintWriter out = response.getWriter();
			out.print("OK");
			out.close();
			return;
		}

		if (url.endsWith("refresh")) {
			doRefresh(response);
			return;
		}
		if (url.endsWith("rss")) {
			String tmpUrl = url;
			tmpUrl = tmpUrl.substring(0,tmpUrl.length()-4);
			displayRss(response,tmpUrl);
			return;
		}
		if (url.endsWith("jar")) {
			response.setContentType("application/java-archive");
			response.setHeader("Content-Disposition", "attachment; filename=\"allImages.jar\"");
			model.jarAllFiles(response.getOutputStream());
			return;
		}
		if (url.endsWith("bigdaily.jpg")) {
			if (request.getParameter("count") != null) {
				int count = Integer.parseInt(request.getParameter("count"));
				getScaledDaily2(response, count,SCREENX2, SCREENY);
				return;
			}
			getScaledDaily2(response,2,SCREENX2, SCREENY);
			return;
		}
		// bigdaily4.jpg
		if (url.endsWith("bigdaily4.jpg")) {
			getScaledDaily2(response,4,SCREENX2, SCREENY);
			return;
		}
		if (url.endsWith("dailyscaled.jpg")) {
			getRandomPictureScaled(response,SCREENX, SCREENY);
			return;
		}
		if (url.endsWith("daily.jpg")) {
			getRandomPicture(response);
			return;
		}
	}

	private void getPictureScaled(HttpServletResponse response,Foto foto, int allwidth, int allheight) throws IOException {
		BufferedImage tp = new BufferedImage(allwidth,allheight,BufferedImage.TYPE_INT_RGB);
		Graphics2D canvas = tp.createGraphics();
		canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		BufferedImage img;
		img = ImageIO.read(foto.getImgAsFile());
		System.out.print(img.getWidth() + "x" + img.getHeight() + " => ");
		//System.out.println("StartX : " + img.getWidth());
		//System.out.println("StartY : " + img.getHeight());
		int left = 0;
		int freespaceX = allwidth - img.getWidth();

		int top = 0;
		int freespaceY = allheight - img.getHeight();

		AffineTransform tx = new AffineTransform();
		//int diff = Math.abs(freespaceX - freespaceY);
		//int diff = 0;
		double scale=0D;
		if (freespaceX< freespaceY) {
			scale = (double) (img.getWidth() +freespaceX) / img.getWidth();
		} else {
			scale = (double) (img.getHeight()+freespaceY) / img.getHeight();
		}


		//double scalex = (double) (img.getWidth() +diff) / img.getWidth();
		//double scaley = (double) (img.getHeight()+diff) / img.getHeight();

		tx.scale(scale, scale);


		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
		img= op.filter(img, null);
		//System.out.println("DannX : " + img.getWidth());
		//System.out.println("DannY : " + img.getHeight());
		// Neues Zentrieren
		freespaceX = allwidth - img.getWidth();
		freespaceY = allheight - img.getHeight();

		// Centered
		left = left + freespaceX / 2;
		top = freespaceY / 2;

		canvas.drawImage(new ImageIcon(img).getImage(),left,top,null);
		canvas.dispose();
		response.setContentType("image/jpeg");
		ImageIO.write(tp, "jpeg", response.getOutputStream());
	}

	private void getRandomPictureScaled(HttpServletResponse response,int allwidth, int allheight) throws IOException {
		List<Foto> fotos = model.getAllFotos();
		int id = r.nextInt(fotos.size());
		getPictureScaled(response,fotos.get(id),allwidth,allheight);
	}

	private void getRandomPicture(HttpServletResponse response) throws IOException {
		response.setContentType("image/jpeg");
		List<Foto> fotos = model.getAllFotos();
		int id = r.nextInt(fotos.size());
		OutputStream out = response.getOutputStream();
		out.write(fotos.get(id).getImgAsBytes());
		out.flush();
		out.close();
	}

	@SuppressWarnings("unchecked")
	private void displayRss(HttpServletResponse response, String url) throws IOException {
		SimpleRss rss = new SimpleRss();
		rss.setDescription("Aktuelle Bilder im FotoBrowser");
		rss.setTitle("FotoBrowser");
		rss.setLink(url);

		List<Foto> list = model.getAllFotos();
		for(Foto foto : list) {
			SimpleRssItem item = new SimpleRssItem();
			item.setTitle(foto.getTitle());
			item.setLink(url + "/viewHTML?id=" + foto.getId());
			item.setDescription("<a href=\""+url+"/viewHTML?id="+foto.getId()+"\"><img src=\""+url+"/thumb?id=" + foto.getId() + "\"></img></a>");
			//item.setDescription("<p><img src='"+url+"/thumb?id=" + foto.getId() + "'></p>");
			item.setEnclosureUrl(url + "/view?id=" + foto.getId());
			item.setEnclosureMimeType("image/jpeg");
			item.setPubDate(foto.getDate());
			rss.addItem(item);
		}
		response.setHeader("Content-Type", "text/xml");
		rss.writeRss(response.getOutputStream());
	}

	PrintWriter progressout =null;
	public void doRefresh(HttpServletResponse response) throws IOException {
		Model mdl = new Model(factory);
		progressout = response.getWriter();
		progressout.println("<html><body>");
		progressout.println("<H1>Updating RSS</H1>");
		progressout.flush();
		response.flushBuffer();
		try {
			mdl.update(new ProgressEvent() {

				@Override
				public void info(String value) {
					progressout.println(value);
					progressout.println("<br>\n");
				}

				@Override
				public void next(String info) {
					progressout.println(info);
					progressout.println("<br>\n");
				}

				@Override
				public void setMax(int value) {
				}

			});
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		progressout.println("<a href='grid'>Finished</a>");

	}


	@SuppressWarnings("unchecked")
	private void displayGrid(HttpServletResponse response, String rsshtml) throws IOException {
		//displayGrid
		int maxX = 5;
		List<Foto> list = model.getAllFotos();
		int counter=0;
		PrintWriter out = response.getWriter();

		out.println("<html>");
		out.println("<head>" + rsshtml + "</head>");
		out.println("<body style='background-color:black;'>");
		out.print("<table border='0' width='100%'><tr style='text-align:center;'>");
		out.print("<td><a href='http://www.jens.org/~hacker/jnlp/FotoBrowser.jnlp'><img border='0' src='images/Java-Logo.png' alt='Open WebSrtart' title='Open WebStart'/></a></td>");
		out.print("<td><a href='jar'><img border='0' src='images/document-save.png' alt='get Jar' title='get Jar'/><!-- get Jar --></a></td>");
		// out.print("<td><a href='refresh'><img border='0' src='images/view-refresh.png' alt='Refresh RSSData' title='Refresh RSSData'/>Refresh RSSData</a></td>");
		out.print("<td><a href='rss'><img border='0' src='images/rss.png' alt='RSS-Feed' title='RssFeed'/></a></td>");
		out.print("<td><a href='daily.jpg'><img border='0' src='images/image-loading.png' alt='Daily Pic' title='Daily Pic'/><!-- Daily Pic --></a></td>");
		out.print("<td><a href='dailyscaled.jpg'><img border='0' src='images/view-fullscreen.png' alt='Daily Scaled' title='Daily Scaled'/><!-- Daily Scaled  --></a></td>");
		out.print("<td><a href='bigdaily.jpg'><img border='0' src='images/view-dualhead.png' alt='Daily BigPic' title='Daily BigPic'/><!-- Daily BigPic --></a></td>");
		out.print("<td><a href='bigdaily4.jpg'><img border='0' src='images/view-dualhead4.png' alt='Daily BigPic4' title='Daily BigPic4'/><!-- Daily BigPic4 --></a></td>");

		out.println("</tr></table>");
		out.println("<table border='0'>");
		String todayRGB= "#777777";
		String yesterdayRGB = "#555555";
		Calendar todayC = Calendar.getInstance();
		Calendar yesterdayC = Calendar.getInstance();
		yesterdayC.add(Calendar.DAY_OF_YEAR, -1);

		Date today = todayC.getTime();
		Date yesterday = yesterdayC.getTime();
		for(Foto foto : list) {
			if (counter % maxX == 0) {
				out.println("</tr><tr>");
				out.flush();
			}
			String color = "black";
			if (DateUtils.isSameDay(foto.getDate(), today)) {
				color = todayRGB;
			}
			if (DateUtils.isSameDay(foto.getDate(), yesterday)) {
				color =yesterdayRGB;
			}
			out.print("<td align='center' style='background-color: "+color+";'>");
			out.print("<a href='viewHTML?id=" + foto.getId() + "'>");
			out.print("<img border='0' src='thumb?id="+foto.getId()+"'>");
			out.println("</a><br>");
			out.print("<div style='color:white'>");
			out.print(foto.getTitle());
			out.print("</div>");
			out.print("<div style='font-size:8px; color:white'>");
			out.print("<a href='delete?id=" + foto.getId() + "'><img border='0' src='images/delete.gif'></a>");
			out.print(foto.getDateAsString());
			out.print("</div>");
			out.print("</td>");
			counter++;
		}
		out.println("</table></body></html>");
		out.flush();
	}

	private Foto[] getRandomFoto(int count) {
		List<Foto> fotos = model.getAllFotos();
		Foto[] lst = new Foto[count];
		for(int i = 0; i< count; i++) {
			lst[i] = fotos.get(r.nextInt(fotos.size()-1));
		}
		return lst;
	}

	private Foto getRandomFoto(List<Foto> fotos, int what) {
		if (what == 0) {
			return fotos.get(r.nextInt(fotos.size()-1));
		}
		List<Foto> tmpList = new ArrayList<Foto>();
		if (what == 1) {
			// hochkant
			for(Foto foto : fotos) {
				if (!foto.isWidescreen()) {
					tmpList.add(foto);
				}
			}
		}
		if (what == 2) {
			// breitkant
			for(Foto foto : fotos) {
				if (foto.isWidescreen()) {
					tmpList.add(foto);
				}
			}
		}
		if (tmpList.size() == 0) {
			System.out.println("No sufficient Fotos ");
			return getRandomFoto(fotos,0); // undef zurückgeben
		}
		return tmpList.get(r.nextInt(tmpList.size()-1));
	}

	/* Hat noch ein Scaling Problem */
	public void getScaledDaily2(HttpServletResponse response, int imageCount, int allwidth, int allheight) throws IOException {
		if (imageCount % 2 != 0 ){
			throw new IOException("ImageCount muss durch 2 teilbar sein");
		}
		int borderX=5;
		int borderY=5;

		BufferedImage tp = new BufferedImage(allwidth,allheight,BufferedImage.TYPE_INT_RGB);
		Graphics2D canvas = tp.createGraphics();
		canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		List<Foto> fotos = model.getAllFotos();




		//Foto[] fotos = getRandomFoto(imageCount);
		BufferedImage img;
		int einzelWidth = Math.round(allwidth / imageCount);
		int imageCountWidescreenMode = 2;
		int einzelWidthWidescreen = Math.round(allwidth / ( imageCount / imageCountWidescreenMode));


		int einzelHeight = Math.round(allheight / imageCountWidescreenMode);
		for(int i = 0; i<imageCount; i = i + imageCountWidescreenMode) {
			Foto[] tmpList = new Foto[imageCountWidescreenMode];

			tmpList[0] = getRandomFoto(fotos,0);
			int a = 0;
			if (tmpList[0].isWidescreen()) {
				a = 2;
			} else {
				a = 1;
			}
			tmpList[1] = getRandomFoto(fotos, a);
			if (tmpList[1] == tmpList[0]) {
				tmpList[1] = getRandomFoto(fotos, a);
			}
			//System.out.println(tmpList[0].isWidescreen());
			//System.out.println(tmpList[1].isWidescreen());
			for(int j = 0; j<imageCountWidescreenMode; j++) {
				if (tmpList[0].isWidescreen() && tmpList[1].isWidescreen() && imageCount>2) {
					// breit-wand-modus !
					//logger.info("Breitkant");
					img = ImageIO.read(tmpList[j].getImgAsFile());
					//System.out.println("StartX : " + img.getWidth());
					//System.out.println("StartY : " + img.getHeight());
					int left = i * einzelWidth;
					int freespaceX = einzelWidthWidescreen - (img.getWidth() + borderX);

					int top = einzelHeight * j; // j muss immer bei 0 beginnen !
					int freespaceY = einzelHeight - (img.getHeight() + borderY);

					//System.out.println("FeeX : " + freespaceX);
					//System.out.println("FeeY : " + freespaceY);

					AffineTransform tx = new AffineTransform();
					double scale=0D;
					if (freespaceX< freespaceY) {
						scale = (double) (img.getWidth() +freespaceX) / img.getWidth();
					} else {
						scale = (double) (img.getHeight()+freespaceY) / img.getHeight();
					}

					//double scalex = (double) (img.getWidth() +diff) / img.getWidth();
					//double scaley = (double) (img.getHeight()+diff) / img.getHeight();

					tx.scale(scale, scale);
					AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
					img= op.filter(img, null);

					freespaceX = einzelWidthWidescreen - (img.getWidth() + borderX);
					freespaceY = einzelHeight - (img.getHeight() +borderY);

					//System.out.println("FeeX : " + freespaceX);
					//System.out.println("FeeY : " + freespaceY);

					// Centered
					left = left + freespaceX / 2;
					top = top + freespaceY / 2;

					canvas.drawImage(new ImageIcon(img).getImage(),left,top,null);

				} else {
					//logger.info("Hochkant");
					img = ImageIO.read(tmpList[j].getImgAsFile());
					//System.out.println("StartX : " + img.getWidth());
					//System.out.println("StartY : " + img.getHeight());
					int left = (i+j) * einzelWidth;
					int freespaceX = einzelWidth - (img.getWidth() + borderX);

					int top = 0;
					int freespaceY = allheight - (img.getHeight() + borderY);

					AffineTransform tx = new AffineTransform();
					//int diff = Math.abs(freespaceX - freespaceY);
					double scale=0D;
					if (freespaceX< freespaceY) {
						scale = (double) (img.getWidth() +freespaceX) / img.getWidth();
					} else {
						scale = (double) (img.getHeight()+freespaceY) / img.getHeight();
					}


					//double scalex = (double) (img.getWidth() +diff) / img.getWidth();
					//double scaley = (double) (img.getHeight()+diff) / img.getHeight();

					tx.scale(scale, scale);

					AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
					img= op.filter(img, null);
					//System.out.println("DannX : " + img.getWidth());
					//System.out.println("DannY : " + img.getHeight());
					// Neues Zentrieren
					freespaceX = einzelWidth - (img.getWidth() + borderX);
					freespaceY = allheight - (img.getHeight() + borderY);

					// Centered
					left = left + freespaceX / 2;
					top = freespaceY / 2;

					canvas.drawImage(new ImageIcon(img).getImage(),left,top,null);
				}
			}
		}
		canvas.dispose();
		response.setContentType("image/jpeg");
		ImageIO.write(tp, "jpeg", response.getOutputStream());
	}

	/* Simple - f. 2 Fotos -- Ohne sonderbehandlung f. Hoch/Breitkant */
	public void getScaledDaily(HttpServletResponse response) throws IOException {
		int allheight = 1024;
		int allwidth = 2560;
		int imageCount = 4;

		BufferedImage tp = new BufferedImage(allwidth,allheight,BufferedImage.TYPE_INT_RGB);
		Graphics2D canvas = tp.createGraphics();
		canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		Foto[] fotos = getRandomFoto(imageCount);
		BufferedImage img;
		int einzelWidth = Math.round(allwidth / imageCount);

		for(int i = 0; i<imageCount; i++) {
			img = ImageIO.read(fotos[i].getImgAsFile());
			//System.out.println("StartX : " + img.getWidth());
			//System.out.println("StartY : " + img.getHeight());
			int left = i * einzelWidth;
			int freespaceX = einzelWidth - img.getWidth(null);

			int top = 0;
			int freespaceY = allheight - img.getHeight(null);

			AffineTransform tx = new AffineTransform();
			//int diff = Math.abs(freespaceX - freespaceY);
			double scale=0D;
			if (freespaceX< freespaceY) {
				scale = (double) (img.getWidth() +freespaceX) / img.getWidth();
			} else {
				scale = (double) (img.getHeight()+freespaceY) / img.getHeight();
			}


			//double scalex = (double) (img.getWidth() +diff) / img.getWidth();
			//double scaley = (double) (img.getHeight()+diff) / img.getHeight();

			tx.scale(scale, scale);


			AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
			img= op.filter(img, null);
			//System.out.println("DannX : " + img.getWidth());
			//System.out.println("DannY : " + img.getHeight());
			// Neues Zentrieren
			freespaceX = einzelWidth - img.getWidth(null);
			freespaceY = allheight - img.getHeight(null);

			// Centered
			left = left + freespaceX / 2;
			top = freespaceY / 2;

			canvas.drawImage(new ImageIcon(img).getImage(),left,top,null);
		}
		canvas.dispose();
		response.setContentType("image/jpeg");
		ImageIO.write(tp, "jpeg", response.getOutputStream());
	}
}
