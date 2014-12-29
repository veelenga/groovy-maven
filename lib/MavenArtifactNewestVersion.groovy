package lib

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

public class MavenArtifactNewestVersion {

    private RepositorySystem system;
    private RepositorySystemSession session;
    private List<RemoteRepository> repositories;

    public MavenArtifactNewestVersion(List<RepoStruct> repos) {
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

        if (newestVersion != null){
          System.out.format("%s.%s: %s -> %s\n", artifact.getGroupId(), artifact.getArtifactId(), newestVersion,
                  rangeResult.getRepository(newestVersion));
        }

        return newestVersion;
    }
}
