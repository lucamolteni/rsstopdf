package io.triode.rsstopdf;

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class Outline {
	@XmlAttribute
	public String text;

	@XmlAttribute
	public String title;

	@XmlAttribute
	public String xmlUrl;

	@XmlAttribute
	public String htmlUrl;

	@XmlAttribute
	public String type;

	@XmlElement(name = "outline")
	public List<Outline> subOutlines;

	@Override
	public String toString() {
		return "\n\tOutline{" +
				"text='" + text + '\'' +
				", title='" + title + '\'' +
				", xmlUrl='" + xmlUrl + '\'' +
				", htmlUrl='" + htmlUrl + '\'' +
				", type='" + type + '\'' +
				( subOutlines != null ? ", subOutlines=" + subOutlines : "" ) +
				'}';
	}
}