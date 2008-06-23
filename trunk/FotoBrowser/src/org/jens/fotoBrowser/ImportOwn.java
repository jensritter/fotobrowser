package org.jens.fotoBrowser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.jens.fotoBrowser.model.hibernate.FotoFeed;
import org.jens.fotoBrowser.model.hibernate.Foto;
import java.io.File;
import java.io.IOException;

import org.jens.Shorthand.JDBC;
import org.jens.Shorthand.IO;

public class ImportOwn {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ImportOwn a = new ImportOwn();
		try {
			a.run();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() throws SQLException, IOException {
			Model mdl = new Model();

			java.sql.Connection con = JDBC.openPGConnection("matrix", "ilife","hacker","linux");
			Statement stm = con.createStatement();

			/* Alle Album */
			ResultSet rs = stm.executeQuery("select id,albumname from iphoto_album where archive_id = 149");
			HashMap<Integer, String> albums = new HashMap<Integer, String>();
			while (rs.next()) {
				int id = rs.getInt("id");
				String name = rs.getString("albumname");
				if (name.equals("Fotos")) {
					continue;
				}
				if (name.equals("Markierte Fotos")) {
					continue;
				}
				if (name.equals("Letzte 12 Monate")) {
					continue;
				}
				if (name.equals("Letzter Import")) {
					continue;
				}
				if (name.startsWith("AUTO")) {
					continue;
				}

				System.out.println(name);
				albums.put(id,name);

			}
			rs.close();

			for(Map.Entry<Integer,String> it : albums.entrySet()) {
				String feedname = it.getValue();
				int albumid = it.getKey();
				FotoFeed feed = mdl.getFeed(feedname,feedname,feedname,feedname);

				rs = stm.executeQuery("select * from iphoto_photo where id in (select fotos_id from iphoto_album_iphoto_photo where fotos_archive_id = 149 and iphoto_album_id = "+albumid+" ) and archive_id = 149 ");
				while (rs.next()) {
					String title = rs.getString("caption");
					String image = rs.getString("imagepath");
					Date date = rs.getDate("dateastimerintervalasdate");
					
					File img = new File("/Users/mac/media/iPhoto/" + image);
					System.out.println(img.toString());
					byte[] value = org.jens.Shorthand.IO.getBytesFromFile(img);
					Foto foto = mdl.uploadFoto(feed,value,title);
					foto.setDate(date);
					mdl.saveFoto(foto);
				}
				rs.close();
			}
			stm.close();
			con.close();

			//Foto foto = mdl.uploadFoto(new File("/tmp/bild.jpg"),"**TEST**");
			//System.out.println(foto.getHeight());
			//mdl.removeFoto(foto);
	}

}
