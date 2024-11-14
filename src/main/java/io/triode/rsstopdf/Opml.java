package io.triode.rsstopdf;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "opml")
@XmlAccessorType(XmlAccessType.FIELD)
public class Opml {
	@XmlElement(name = "head")
	public Head head;

	@XmlElement(name = "body")
	public Body body;

	@Override
	public String toString() {
		return "Opml{" +
				"head=" + head +
				", body=" + body +
				'}';
	}
}

class Head {
	@XmlElement(name = "title")
	public String title;

	@Override
	public String toString() {
		return "Head{" +
				"title='" + title + '\'' +
				'}';
	}
}

class Body {
	@XmlElement(name = "outline")
	public List<Outline> outlines;

	@Override
	public String toString() {
		return "Body{" +
				"outlines=" + outlines +
				'}';
	}
}

