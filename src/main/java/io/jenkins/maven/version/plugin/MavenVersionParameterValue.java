package io.jenkins.maven.version.plugin;

import hudson.AbortException;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.ParameterValue;
import hudson.tasks.BuildWrapper;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.regex.Pattern;

public class MavenVersionParameterValue extends ParameterValue {

  private String version;
  private String nextVersion;

  @DataBoundConstructor
  public MavenVersionParameterValue(String name, String description) {
    this(name, description, "1.0.0", "1.1.0-SNAPSHOT");
  }

  public MavenVersionParameterValue(String name, String description, String version, String nextVersion) {
    super(name, description);
    this.version = version;
    this.nextVersion = nextVersion;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getNextVersion() {
    return nextVersion;
  }

  public void setNextVersion(String nextVersion) {
    this.nextVersion = nextVersion;
  }

  public String toString() {
    return "(MavenVersionParameter) " + this.getName() + ": Version: " + this.getVersion() + " Next Version: " + this.getNextVersion();
  }

  @Override
  public BuildWrapper createBuildWrapper(AbstractBuild<?, ?> build) {
    Pattern pattern = Pattern.compile("\\d*");
    if (!pattern.matcher(version).matches() || !pattern.matcher(nextVersion).matches()) {
      // abort the build within BuildWrapper
      return new BuildWrapper() {
        @Override
        public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
          throw new AbortException("Invalid value type! Vale must be integer");
        }
      };
    } else {
      return null;
    }
  }

}
