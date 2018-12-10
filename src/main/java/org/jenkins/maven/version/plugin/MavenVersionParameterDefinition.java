package org.jenkins.maven.version.plugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import hudson.Extension;
import hudson.model.*;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class MavenVersionParameterDefinition extends ParameterDefinition {

  public static final Logger logger = Logger.getLogger(MavenVersionParameterDefinition.class.getName());
  public static final String EMPTY_JOB_NAME = "EMPTY_JOB_NAME";
  public static final String SNAPSHOT = "-SNAPSHOT";
  private final UUID uuid;

  @DataBoundConstructor
  public MavenVersionParameterDefinition(String name, String description) {
    super(name, description);
    this.uuid = UUID.randomUUID();
  }

  @Override
  public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject) {
    String name = getName();
    String parameters = "VERSION: " + staplerRequest.getParameter(name + "_version") + "; NEXT_VERSION: " + staplerRequest.getParameter(name + "_nextVersion");
    MavenVersionParameterValue mavenVersionParameterValue = new MavenVersionParameterValue(name, parameters);
    return mavenVersionParameterValue;

  }

  public String getPomVersion(){
    File file = new File(getJobWorspace(), "pom.xml");
    if(file.exists()) {
      try {
        final Model mavenModels = parseMavenModel(file);
        return mavenModels.getVersion();
      } catch (XmlPullParserException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  public Model parseMavenModel(File file) throws XmlPullParserException, IOException {
    MavenXpp3Reader reader = new MavenXpp3Reader();
    FileReader fileReader = new FileReader(file);
    return reader.read(fileReader);
  }

  public int compareTo(MavenVersionParameterDefinition pd) {
    return pd.uuid.equals(uuid) ? 0 : -1;
  }

  public String getJobWorspace(){
    Jenkins jenkins = Jenkins.getInstance();
    TopLevelItem topLevelItem = jenkins.getItem(getCustomeJobName());
    return jenkins.getWorkspaceFor(topLevelItem).toString();
  }

  public String getCustomeJobName() {
    Job job = getParentJob();
    String fullName = job != null ? job.getFullName() : EMPTY_JOB_NAME;
    return fullName;
  }

  public Job getParentJob() {
    Job context = null;
    List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);

    for (Job job : jobs) {
      if (!(job instanceof TopLevelItem)) continue;

      ParametersDefinitionProperty property = (ParametersDefinitionProperty) job.getProperty(ParametersDefinitionProperty.class);

      if (property != null) {
        List<ParameterDefinition> parameterDefinitions = property.getParameterDefinitions();

        if (parameterDefinitions != null) {
          for (ParameterDefinition pd : parameterDefinitions) {
            if (pd instanceof MavenVersionParameterDefinition && ((MavenVersionParameterDefinition) pd).compareTo(this) == 0) {
              context = job;
              break;
            }
          }
        }
      }
    }
    return context;
  }

  public String getMavenVersion() {
    String version = getPomVersion();
    if(validateVersion(version)){
      return removeSnapshot(version);
    }
    return "";
  }

  public String getMavenNextVersion() {
    String version = getPomVersion();
    if(validateVersion(version)){
      String[] pointsVersions = version.split(Pattern.quote("."));
      Integer pointsQtd = pointsVersions.length;
      String minorVersion = removeSnapshot(pointsVersions[pointsQtd - 1]);

      Integer minorVersionInteger = new Integer(minorVersion) + 1;
      pointsVersions[pointsQtd - 1] = minorVersionInteger.toString();
      StringBuilder nextVersion = new StringBuilder();

      for (int i = 0; i < pointsQtd; i ++) {
        if(i == pointsQtd - 1) {
          nextVersion.append(pointsVersions[i] + SNAPSHOT);
        } else {
          nextVersion.append(pointsVersions[i] + ".");
        }
      }
      return nextVersion.toString();
    }

    return "";
  }

  public String removeSnapshot(String version) {
    return version.toUpperCase().replace(SNAPSHOT, "").trim();
  }

  public boolean validateVersion(String version) {
    return (version != null && !version.isEmpty()) ? true : false;
  }

  @Override
  public ParameterValue createValue(StaplerRequest staplerRequest) {
    System.out.println("Create value with 1 argument");
    String value[] = staplerRequest.getParameterValues(getName());
    return new MavenVersionParameterValue(getName(), value[0]);
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Extension
  public static final class DescriptorImpl extends ParameterDescriptor {

    @Override
    public String getDisplayName() {
      return "Maven version parameter";
    }

    public FormValidation doValidate(@QueryParameter String value) {
      return validationRegularExpression(value);
    }

    private FormValidation validationRegularExpression(final String value) {
      try {
        Pattern.compile(value);
        return FormValidation.ok();
      } catch (PatternSyntaxException e) {
        logger.severe("Expressão regular errada: " + value);
        return FormValidation.error(e.getMessage());
      }
    }
  }
}
