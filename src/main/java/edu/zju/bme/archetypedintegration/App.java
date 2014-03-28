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
import edu.zju.bme.geo.soft.GEOParse;
import edu.zju.bme.geo.soft.Platform;
import edu.zju.bme.geo.soft.Sample;
import edu.zju.bme.geo.soft.Series;
import edu.zju.bme.hibernarm.service.AQLExecute;

/**
 * Hello world!
 * 
 */
public class App {

	private static Logger logger = Logger.getLogger(App.class.getName());
	protected static Map<String, String> archetypes = new LinkedHashMap<String, String>();
	protected static String archetypeString = "";
	protected File mapFile;

	public void setMapFlie(File m) {
		mapFile = m;
	}

	public File getMapFlie() {
		return mapFile;
	}

	public static void main(String[] args) throws InterruptedException {

		try {
			@SuppressWarnings("resource")
			ApplicationContext context = new ClassPathXmlApplicationContext(
					"/beans.xml", App.class);
			AQLExecute client = (AQLExecute) context.getBean("wsclient");
			File oldFile = new File("H:/GEO");
			File newFile = new File("H:/OEG");
			App app = new App();
			while (true) {
				File[] fArray = oldFile.listFiles();
				if (fArray.length != 0) {
					for (File file : fArray) {
						GEOParse gp = new GEOParse();
						gp.parseFile(file);
						if(gp.getMapPlatForm()!=null){
							for (Platform plat : gp.getMapPlatForm().keySet()) {
								app.setMapFlie(new File(
										"src/main/resources/gpl.xml"));
								String dadl = generateDADL(app.getMapFlie(),
										plat.getGpl(), App.getArchetypeString(gp
												.getMapPlatForm().get(plat)));
								List<String> dadls = new ArrayList<String>();
								dadls.add(dadl);
								client.insert(dadls);
								dadls.clear();
							}
						}
						
						if(gp.getMapSeries()!=null){
							for (Series ser : gp.getMapSeries().keySet()) {
								app.setMapFlie(new File(
										"src/main/resources/gse.xml"));
								String dadl = generateDADL(app.getMapFlie(),
										ser.getGse(), App.getArchetypeString(gp
												.getMapSeries().get(ser)));
								List<String> dadls = new ArrayList<String>();
								dadls.add(dadl);
								client.insert(dadls);
								dadls.clear();

							}
						}
						
						if(gp.getMapSample()!=null){
							for (Sample sam : gp.getMapSample().keySet()) {
								app.setMapFlie(new File(
										"src/main/resources/gsm.xml"));
								String dadl = generateDADL(app.getMapFlie(),
										sam.getGsm(), App.getArchetypeString(gp
												.getMapSample().get(sam)));
								List<String> dadls = new ArrayList<String>();
								dadls.add(dadl);
								client.insert(dadls);
								dadls.clear();
							}
						}				
						file.renameTo(new File(newFile, file.getName()));
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

	protected static String generateDADL(File mappingFile,
			Map<String, String> dataFile, String archetypeString)
			throws Exception {
		Map<String, String> mp = parseXml(mappingFile);
		Map<String, Object> hm = new HashMap<String, Object>();
		for (String s : dataFile.keySet()) {
			for (String t : mp.keySet()) {
				if (s.equals(t)) {
					hm.put(mp.get(t), dataFile.get(s));
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
