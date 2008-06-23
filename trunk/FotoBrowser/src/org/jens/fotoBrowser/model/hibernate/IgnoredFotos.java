package org.jens.fotoBrowser.model.hibernate;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class IgnoredFotos {
	private String imgUrl;
	private String md5;

	@Id
	public String getImgUrl() {
		return imgUrl;
	}
	public void setImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
	}
	public String getMd5() {
		return md5;
	}
	public void setMd5(String md5) {
		this.md5 = md5;
	}
}
