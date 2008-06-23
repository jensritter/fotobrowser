package org.jens.fotoBrowser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.jens.fotoBrowser.model.hibernate.Foto;

public class CLIJar {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		EntityManagerFactory emf = javax.persistence.Persistence.createEntityManagerFactory("fotobrowser");
		Model a = new Model(emf);

		File[] images = Foto.getImgPath().listFiles();
		File[] thumbs = Foto.getThumbPath().listFiles();

		HashMap<File, Boolean> hit = new HashMap<File, Boolean>();
		for(File img : images) {
			hit.put(img,false);
		}
		for(File tmb : thumbs) {
			hit.put(tmb,false);
		}

		for(Foto foto : a.getAllFotos() ) {
			if (hit.containsKey(foto.getImgAsFile())) {
				// alles OK
				hit.put(foto.getImgAsFile(),true);
			} else {
				System.out.println("No Image : " + foto.getId() + " -" + foto.getImgAsFile().toString());
			}
			if (hit.containsKey(foto.getThumbAsFile())) {
				hit.put(foto.getThumbAsFile(),true);
			} else {
				System.out.println("No Thumb : " + foto.getId() + " -" + foto.getThumbAsFile().toString());
			}
		}
		for(Map.Entry<File, Boolean> it : hit.entrySet()) {
			if (it.getValue().booleanValue() == false) {
				// kein Treffer für diese Datei !
				System.out.println("Unbekannte Datei : " + it.getKey().toString());
			}
		}


	}
}
