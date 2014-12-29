package lib

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;


public class PomUpdater {

  private DocumentBuilder docbuilder;
  private Transformer transformer;

  public PomUpdater() throws Exception {
    docbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    transformer = TransformerFactory.newInstance().newTransformer();
  }

  public void update(String pomPath, Map<String, String> artifactVersions){
    File file = new File(pomPath);
    if (!file.exists()){
      throw new IllegalArgumentException("File not exists: " + file.getAbsolutePath());
    }
    if (file.isDirectory()){
        updateAll(file, artifactVersions);
    }else {
        update(file, artifactVersions);
    }
  }

  private void updateAll(File baseDir, Map<String, String> artifactVersions) throws Exception {
    List<String> files = new LinkedList<String>();
    findPoms(baseDir, files);
    for (String file : files) {
      doUpdate(file, artifactVersions);
    }
  }

  private void update(File file, Map<String, String> artifactVersions) throws Exception {
    doUpdate(file.getAbsolutePath(), artifactVersions);
  }

  private void doUpdate(String pom, Map<String, String> artifactVersions) throws Exception {
    println sprintf ("Processing %s file", pom)
    Document doc = loadPom(pom);
    List<Node> dependencies = getDependencies(doc);
    for (Node dependency : dependencies){
      process(dependency, artifactVersions);
    }
    savePom(doc, pom);
  }

  private Document loadPom(String pom) throws Exception{
    docbuilder.reset();
    Document doc = docbuilder.parse(pom);
    return doc;
  }

  private void savePom(Document doc, String file) throws Exception{
    transformer.reset();
    Source source = new DOMSource(doc);
    Result result = new StreamResult(file);
    transformer.transform(source, result);
  }

  public void findPoms(File root, List<String> result) {
    File[] list = root.listFiles();
    if (list == null) return;

    for ( File f : list ) {
      if ( f.isDirectory() ) {
        findPoms( f, result );
      } else if (f.getName().equals("pom.xml") || f.getName().equals("user-pom.xml")){
          result.add(f.getAbsolutePath());
      }
    }
  }

  private List<Node> getDependencies(Document doc) throws Exception{
    final Element rootElement = doc.getDocumentElement();
    final NodeList list = rootElement.getElementsByTagName("dependency");

    List<Node> result = new ArrayList<Node>();
    for(int i = 0; i < list.getLength(); i++ ){
       result.add(list.item(i));
    }
    return result;
  }

  private void process(Node dependency, Map<String, String> artifactVersions){

    Node groupId = getFirst(dependency, "groupId");
    Node artifactId = getFirst(dependency, "artifactId");
    Node version = getFirst(dependency, "version");

    String fullName = String.format("%s.%s", groupId.getTextContent(), artifactId.getTextContent())


    for (Map.Entry<String, String> entry: artifactVersions.entrySet()){
      def gId = entry.getKey().getGroupId();
      def aId = entry.getKey().getArtifactId();
      def ver = entry.getValue();

      if (gId.equals(groupId.getTextContent()) && aId.equals(artifactId.getTextContent())){
        if (version == null) {
          System.out.println("====> Warn: version tag not found for " + fullName);
          return
        }
        if (!ver.equals(version.getTextContent())){
          System.out.format("====> Changing version: %s %s => %s\n", fullName, version.getTextContent(), ver);
          version.setTextContent (ver);
        }
      }
    }
  }

  private Node getFirst(Node node, String childName) {
    NodeList childs = node.getChildNodes();
    for(int i=0; i < childs.getLength(); i++) {
      Node current = childs.item(i);
      if(current.getNodeType() == Node.ELEMENT_NODE && childName.equals(current.getNodeName()))
        return current;
    }
    return null;
  }
}
