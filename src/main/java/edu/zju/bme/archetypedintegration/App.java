package edu.zju.bme.archetypedintegration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.hibernate.internal.util.ReflectHelper;
import org.openehr.am.archetype.Archetype;
import org.openehr.rm.binding.DADLBinding;
import org.openehr.rm.common.archetyped.Locatable;
import org.openehr.rm.util.GenerationStrategy;
import org.openehr.rm.util.SkeletonGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import se.acode.openehr.parser.ADLParser;
import edu.zju.bme.geo.soft.GplParse;
import edu.zju.bme.hibernarm.service.AQLExecute;

/**
 * Hello world!
 * 
 */
public class App {

	private static Logger logger = Logger.getLogger(App.class.getName());

	public static void main(String[] args) throws InterruptedException {

		try {
			@SuppressWarnings("resource")
			ApplicationContext context = new ClassPathXmlApplicationContext(
					"/beans.xml", App.class);
			AQLExecute client = (AQLExecute) context.getBean("wsclient");
			
			while (true) {
				String dadl = generateDADL();
				List<String> dadls = new ArrayList<String>();
				dadls.add(dadl);
				client.insert(dadls);
				Thread.sleep(5000);
			}
		} catch (Exception e) {
			logger.error(e);
		}

	}

	protected static Map<String, String> parseXml(File f)
			throws DocumentException {
		SAXReader sr = new SAXReader();
		Document d = sr.read(f);
		Element e = d.getRootElement();
		Iterator<Element> it = e.elements().iterator();
		Map<String, String> mp = new HashMap<>();
		while (it.hasNext()) {
			Element el = it.next();
			Element id = el.element("id");
			Element v = el.element("value");
			mp.put(id.getText(), v.getText());
		}
		return mp;
	}

	protected String generateDADL(File mappingFile, File dataFile,
			String archetypeString) throws Exception {
		GplParse.parseFile(dataFile);
		Map<String, String> m = GplParse.getM();
		Map<String, String> mp = parseXml(mappingFile);
		Map<String, Object> hm = new HashMap<String, Object>();
		for (String s : m.keySet()) {
			for (String t : mp.keySet()) {
				if (s.equals(t)) {
					hm.put(mp.get(t), m.get(s));
					break;
				}
			}
		}

		String dadl = "";
		DADLBinding binding = new DADLBinding();
		SkeletonGenerator generator = SkeletonGenerator.getInstance();
		ADLParser parser = new ADLParser(archetypeString);
		Archetype archetype = parser.parse();
		Object result = generator.create(archetype,
				GenerationStrategy.MAXIMUM_EMPTY);

		if (result instanceof Locatable) {
			Locatable loc = (Locatable) result;
			ReflectHelper.setArchetypeValues(loc, hm, null);
			dadl = binding.toDADLString(loc);
		}

		return dadl;
	}

}
