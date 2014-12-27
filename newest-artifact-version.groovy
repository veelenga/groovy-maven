import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;

import java.nio.charset.Charset;
import org.apache.commons.io.FileUtils;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

@Grab(group='commons-cli', module = 'commons-cli', version = '1.2')
@Grab(group='commons-io', module = 'commons-io', version = '2.4')

@Grab(group='org.eclipse.aether', module = 'aether-api', version = '1.0.0.v20140518')
@Grab(group='org.eclipse.aether', module = 'aether-impl', version = '1.0.0.v20140518')
@Grab(group='org.eclipse.aether', module = 'aether-connector-basic', version = '1.0.0.v20140518')
@Grab(group='org.eclipse.aether', module = 'aether-transport-file', version = '1.0.0.v20140518')
@Grab(group='org.eclipse.aether', module = 'aether-transport-http', version = '1.0.0.v20140518')
@Grab(group='org.apache.maven', module='maven-aether-provider', version='3.1.0')

public class FindNewestVersion {

    public static void main(String[] args) throws Exception {

        final Cli cli = new Cli(args);

        final List<String> artifactNames = cli.getArtifactList();
        final List<RepoStruct> repositoryList = cli.getRepositoryList();
        final String pathToOutputFile = cli.getPathToOutputFile();

        final File out = new File(pathToOutputFile);
        if (out.exists()) {
            // overwrite content
            FileUtils.writeStringToFile(out, "", false);
        }

        FindNewestVersion findNewestVersion = new FindNewestVersion(repositoryList);

        for (String artifactName : artifactNames) {
            final DefaultArtifact artifact = new DefaultArtifact(artifactName);
            Version version = findNewestVersion.getNewestVersion(artifact);
            if (version != null) {
                String data = String.format("%s.%s=%s\n", artifact.getGroupId(), artifact.getArtifactId(),
                        version.toString());
                FileUtils.writeStringToFile(out, data, Charset.defaultCharset(), true);
            }
        }
    }

    private RepositorySystem system;
    private RepositorySystemSession session;
    private List<RemoteRepository> repositories;

    public FindNewestVersion(List<RepoStruct> repos) {
        system = newRepositorySystem();
        session = newRepositorySystemSession(system);
        repositories = getRemoteRepositories(repos);
    }

    private RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                exception.printStackTrace();
            }
        });
        return locator.getService(RepositorySystem.class);
    }

    private DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        return session;
    }

    private List<RemoteRepository> getRemoteRepositories(List<RepoStruct> repos) {

        List<RemoteRepository> repositories = new ArrayList<RemoteRepository>(repos.size());
        for (RepoStruct repo : repos) {
            repositories.add(newRepository(repo.name, repo.url));
        }

        return repositories;
    }

    private static RemoteRepository newRepository(String id, String url) {
        return new RemoteRepository.Builder(id, "default", url).build();
    }

    public Version getNewestVersion(Artifact artifact) throws VersionRangeResolutionException {

        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(artifact);
        rangeRequest.setRepositories(repositories);

        VersionRangeResult rangeResult = system.resolveVersionRange(session, rangeRequest);

        Version newestVersion = rangeResult.getHighestVersion();

        System.out.format("%s.%s: %s -> %s\n", artifact.getGroupId(), artifact.getArtifactId(), newestVersion,
                rangeResult.getRepository(newestVersion));

        return newestVersion;
    }

    private static class RepoStruct {
        public String name;
        public String url;

        public RepoStruct(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    private static class Cli {
        private String[] args;
        private Options options = new Options();

        private List<String> artifactList;
        private List<RepoStruct> repositoryList;
        private String pathToOutputFile = "./version.properties";

        public Cli(String[] args) {
            this.args = args;
            this.artifactList = new LinkedList<String>();
            this.repositoryList = new LinkedList<RepoStruct>();

            options.addOption("h", "help", false, "[OPTIONAL] prints this help");
            options.addOption("a", "artifacts", true, "[REQUIRED] list of artifacts to get latest version");
            options.addOption("r", "repositories", true,
                    "[REQUIRED] list of repositories where to search artifact. Format: id1=url1;id2=url2."
                            + " Example: central=http://central.maven.org/maven2/");
            options.addOption("o", "out", true, "[OPTIONAL] path to file with queried artifacts and it's latest versions");

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
                if (cmd.hasOption("o")) {
                    pathToOutputFile = cmd.getOptionValue("o");
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

        public String getPathToOutputFile() {
            return pathToOutputFile;
        }

        public List<RepoStruct> getRepositoryList() {
            return new LinkedList<RepoStruct>(repositoryList);
        }
    }

}
