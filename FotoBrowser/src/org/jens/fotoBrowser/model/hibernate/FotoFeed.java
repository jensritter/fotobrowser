package org.jens.fotoBrowser.model.hibernate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


@Entity
public class FotoFeed {
	private Long id;
	private String bezeichnung;
	private String rssUrl;
	private String basisUrl;
	private String caption;


	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	@Id
	@GeneratedValue()
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getBezeichnung() {
		return bezeichnung;
	}

	public void setBezeichnung(String bezeichnung) {
		this.bezeichnung = bezeichnung;
	}

	public String getRssUrl() {
		return rssUrl;
	}

	public void setRssUrl(String rssUrl) {
		this.rssUrl = rssUrl;
	}

	public String getBasisUrl() {
		return basisUrl;
	}

	public void setBasisUrl(String basisUrl) {
		this.basisUrl = basisUrl;
	}

}
