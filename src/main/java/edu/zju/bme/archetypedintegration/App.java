package edu.zju.bme.archetypedintegration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import edu.zju.bme.geo.soft.GseParse;
import edu.zju.bme.geo.soft.GsmParse;
import edu.zju.bme.hibernarm.service.AQLExecute;

/**
 * Hello world!
 * 
 */
public class App {

	private static Logger logger = Logger.getLogger(App.class.getName());
	protected static Map<String, String> archetypes = new LinkedHashMap<String, String>();
	protected static String archetypeString = "";
	protected Map<String, String> dataMap = new HashMap<String, String>();

	public void setDataMap(Map<String, String> m) {
		dataMap = m;
	}

	public Map<String, String> getDataMap() {
		return dataMap;
	}

	public static void main(String[] args) throws InterruptedException {

		try {
			@SuppressWarnings("resource")
			ApplicationContext context = new ClassPathXmlApplicationContext(
					"/beans.xml", App.class);
			AQLExecute client = (AQLExecute) context.getBean("wsclient");
			File oldFile = new File("H:/GSM");
			File newFile = new File("H:/MSG");
			File map;

			while (true) {
				File[] fArray = oldFile.listFiles();
				if (fArray.length != 0) {
					for (File file : fArray) {
						if (file.getName().startsWith("GSM")) {
							map = new File("src/main/resources/gsm.xml");
							String dadl = generateDADL(map, file,
									App.getArchetypeString("GSM"));
							List<String> dadls = new ArrayList<String>();
							dadls.add(dadl);
							client.insert(dadls);
							file.renameTo(new File(newFile, file.getName()));
						}
					}
				} else {
					break;
				}
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

	protected static String getArchetypeString(String dataType)
			throws IOException {
		archetypes
				.put("openEHR-EHR-OBSERVATION.gpl.v1",
						readLines("F:/Create Database/document/knowledge/ZJU/archetype/omics/openEHR-EHR-OBSERVATION.gpl.v1.adl"));
		archetypes
				.put("openEHR-EHR-OBSERVATION.gse.v1",
						readLines("../document/knowledge/ZJU/archetype/omics/openEHR-EHR-OBSERVATION.gse.v1.adl"));
		archetypes
				.put("openEHR-EHR-OBSERVATION.gsm.v1",
						readLines("../document/knowledge/ZJU/archetype/omics/openEHR-EHR-OBSERVATION.gsm.v1.adl"));
		switch (dataType) {
		case "GPL":
			return archetypeString = archetypes
					.get("openEHR-EHR-OBSERVATION.gpl.v1");
		case "GSE":
			return archetypeString = archetypes
					.get("openEHR-EHR-OBSERVATION.gse.v1");
		case "GSM":
			return archetypeString = archetypes
					.get("openEHR-EHR-OBSERVATION.gsm.v1");
		default:
			return null;
		}
	}

	protected static String generateDADL(File mappingFile, File dataFile,
			String archetypeString) throws Exception {
		App app = new App();

		if (mappingFile.getName().startsWith("gpl")) {
			GplParse gpl = new GplParse();
			gpl.parseFile(dataFile);
			app.setDataMap(gpl.getM());
		}
		if (mappingFile.getName().startsWith("gse")) {
			GseParse gse = new GseParse();
			gse.parseFile(dataFile);
			app.setDataMap(gse.getM());
		}
		if (mappingFile.getName().startsWith("gsm")) {
			GsmParse gsm = new GsmParse();
			gsm.parseFile(dataFile);
			app.setDataMap(gsm.getM());
		}
		Map<String, String> m = app.getDataMap();
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

		System.out.print(hm.size());
		String dadl = "";
		DADLBinding binding = new DADLBinding();
		SkeletonGenerator generator = SkeletonGenerator.getInstance();
		ADLParser parser = new ADLParser(archetypeString);
		Archetype archetype = parser.parse();
		Object result = generator.create(archetype,
				GenerationStrategy.MAXIMUM_EMPTY);

		if (result instanceof Locatable) {
			Locatable loc = (Locatable) result;
			ReflectHelper.setArchetypeValues(loc, hm, archetype);
			dadl = binding.toDADLString(loc);
		}

		return dadl;
	}

	protected static String readLines(String name) throws IOException {
		StringBuilder result = new StringBuilder();
		File file = new File(name);
		InputStream is = new FileInputStream(file);
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));

		String line = reader.readLine();
		while (line != null) {
			result.append(line);
			result.append("\n");
			line = reader.readLine();
		}
		reader.close();
		return result.toString();
	}
}
