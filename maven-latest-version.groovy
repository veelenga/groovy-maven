package com.genesyslab.platform.tests.functional.util

import lib.RepoStruct;
import lib.PomUpdater;
import lib.MavenArtifactNewestVersion;

import org.eclipse.aether.version.Version;
import org.eclipse.aether.artifact.DefaultArtifact;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

@Grab(group='commons-cli', module = 'commons-cli', version = '1.2')
@Grab(group='org.eclipse.aether', module = 'aether-api', version = '1.0.0.v20140518')
@Grab(group='org.eclipse.aether', module = 'aether-impl', version = '1.0.0.v20140518')
@Grab(group='org.eclipse.aether', module = 'aether-connector-basic', version = '1.0.0.v20140518')
@Grab(group='org.eclipse.aether', module = 'aether-transport-file', version = '1.0.0.v20140518')
@Grab(group='org.eclipse.aether', module = 'aether-transport-http', version = '1.0.0.v20140518')
@Grab(group='org.apache.maven', module='maven-aether-provider', version='3.1.0')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.6.1')
public class Cli {
    private String[] args;
    private Options options = new Options();

    private List<String> artifactList;
    private List<RepoStruct> repositoryList;
    private String pomPath;

    public Cli(String[] args) {
        this.args = args;
        this.artifactList = new LinkedList<String>();
        this.repositoryList = new LinkedList<RepoStruct>();

        options.addOption("h", "help", false, "prints this help");
        options.addOption("a", "artifacts", true, "[REQUIRED] list of artifacts to get latest version. " +
                "Format: groupId:artifactId:version;groupId:artifactId:version. Example: log4j:log4j:[1,).");
        options.addOption("r", "repositories", true,
                "[REQUIRED] list of repositories where to search artifact. Format: id1=url1;id2=url2. " +
                "Example: central=http://central.maven.org/maven2/");
        options.addOption("u", "update", true, "path to pom.xml file to update version of specified artifact. " +
                "May be directory to search pom.xml files recursively.");

        parse();
    }

    private void parse() {
        CommandLineParser parser = new BasicParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                help();
            }
            if (cmd.hasOption("a")) {
                artifactList.addAll(Arrays.asList(cmd.getOptionValue("a").split(";")));
            } else {
                System.out.println("Error: -a option required.");
                help();
            }
            if (cmd.hasOption("r")) {
                String[] repos = cmd.getOptionValue("r").split(";");
                for (String r : repos) {
                    String[] sp = r.split("=");
                    if (sp.length != 2) {
                        System.out.println("Error: value of parameter 'r' has wrong format");
                        help();
                    }
                    repositoryList.add(new RepoStruct(sp[0], sp[1]));
                }
            } else {
                System.out.println("Error: -r option required.");
                help();
            }
            if (cmd.hasOption("u")) {
                pomPath = cmd.getOptionValue("u");
            }
        } catch (ParseException e) {
            System.out.println("Error: Unable to parse command line arguments.");
            help();
        }
    }
    private void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("maven-latest-version", options);
        System.exit(0);
    }

    public List<String> getArtifactList() {
        return new LinkedList<String>(artifactList);
    }

    public String getPomPath() {
        return pomPath;
    }

    public List<RepoStruct> getRepositoryList() {
        return new LinkedList<RepoStruct>(repositoryList);
    }

    public static void main(String[] args) throws Exception {

        final Cli cli = new Cli(args);

        final List<String> artifactNames = cli.getArtifactList();
        final List<RepoStruct> repositoryList = cli.getRepositoryList();
        final String pomPath = cli.getPomPath();

        final Map<String, String> artifactVersions = new HashMap<String, String>();

        // find versions in repos
        println "Querying repos..."
        MavenArtifactNewestVersion findNewestVersion = new MavenArtifactNewestVersion(repositoryList);
        for (String artifactName : artifactNames) {
            final DefaultArtifact artifact = new DefaultArtifact(artifactName);
            Version version = findNewestVersion.getNewestVersion(artifact);
            if (version != null) {
                artifactVersions.put(artifact, version.toString());
            }
        }

        if (artifactVersions.isEmpty()){
          println "Error: versions to provided artifacts not found. Exiting..."
          System.exit(-1)
        }

        // update pom.xml files
        if (pomPath != null){
          println "Updating files..."
          PomUpdater updater = new PomUpdater();
          updater.update(pomPath, artifactVersions);
        }
    }
}
