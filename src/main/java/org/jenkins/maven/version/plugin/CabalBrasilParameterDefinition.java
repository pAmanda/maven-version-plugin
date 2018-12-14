package org.jenkins.maven.version.plugin;

import hudson.Extension;
import hudson.model.*;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CabalBrasilParameterDefinition extends ParameterDefinition {

  public static final Logger logger = Logger.getLogger(CabalBrasilParameterDefinition.class.getName());
  private static final long serialVersionUID = 1L;
  private GitParameter gitParameter;
  private String pomVersion = "";
  private final UUID uuid;

  @DataBoundConstructor
  public CabalBrasilParameterDefinition(String name, String description) {
    super(name, description);
    this.uuid = UUID.randomUUID();
  }

  @Override
  public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject) {
    String name = getName();
    String parameters = "ENVIRONMENT: " + staplerRequest.getParameter(name + "_environment") +
            "; VERSION: " + staplerRequest.getParameter(name + "_version") +
            "; NEXT_VERSION: " + staplerRequest.getParameter(name + "_nextVersion") +
            "; TAG_NAME: " + staplerRequest.getParameter(name + "_tag") +
            "; BRANCH_NAME: " + staplerRequest.getParameter(name + "_branch");
    System.out.println("Parâmetros: " + parameters);
    CabalBrasilParameterValue cabalBrasilParameterValue = new CabalBrasilParameterValue(name, parameters);
    return cabalBrasilParameterValue;
  }

  public String getPomVersion(){
      System.out.println("Pegando a versão do pom.");
      if(StringUtils.isBlank(this.pomVersion)) {
        System.out.println("Lendo o pom.xml");
        File file = new File(getJobWorkspace(), "pom.xml");
        if (file.exists()) {
          System.out.println("O pom.xml existe.");
          try {
            System.out.println("Parse pom.xml");
            final Model mavenModels = parseMavenModel(file);
            this.pomVersion = mavenModels.getVersion();
          } catch (XmlPullParserException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    return this.pomVersion;
  }

  public Model parseMavenModel(File file) throws XmlPullParserException, IOException {
    MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
    FileReader fileReader = new FileReader(file);
    return mavenXpp3Reader.read(fileReader);
  }

  public int compareUUID(CabalBrasilParameterDefinition pd) {
    return pd.uuid.equals(uuid) ? 0 : -1;
  }

  public String getJobWorkspace(){
    Jenkins jenkins = Jenkins.getInstance();
    TopLevelItem topLevelItem = jenkins.getItem(getCustomJobName());
    return jenkins.getWorkspaceFor(topLevelItem).getRemote();
  }

  public String getCustomJobName() {
    Job job = getParentJob();
    String fullName = job != null ? job.getFullName() : ConstsUtil.EMPTY_JOB_NAME;
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
            if (pd instanceof CabalBrasilParameterDefinition && ((CabalBrasilParameterDefinition) pd).compareUUID(this) == 0) {
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
    if(!StringUtils.isBlank(version)){
      return removeSnapshot(version);
    }
    return "";
  }

  public String getMavenNextVersion() {
    String version = getPomVersion();
    if(!StringUtils.isBlank(version)){
      String[] pointsVersions = version.split(Pattern.quote("."));
      Integer pointsQtd = pointsVersions.length;
      String minorVersion = removeSnapshot(pointsVersions[pointsQtd - 1]);

      Integer minorVersionInteger = new Integer(minorVersion);
      ++minorVersionInteger;
      pointsVersions[pointsQtd - 1] = minorVersionInteger.toString();
      StringBuilder nextVersion = new StringBuilder();

      for (int i = 0; i < pointsQtd; i ++) {
        if(i == pointsQtd - 1) {
          nextVersion.append(pointsVersions[i] + ConstsUtil.SNAPSHOT);
        } else {
          nextVersion.append(pointsVersions[i] + ".");
        }
      }
      return nextVersion.toString();
    }

    return "";
  }

  public Set<String> getBranch() {
    gitParameter = GitParameter.getInstance(getParentJob());
    return gitParameter.getBranch();
  }

  public Set<String> getTag() {
    //gitParameter = GitParameter.getInstance(getParentJob());
    return gitParameter.getTag();
  }

  public String removeSnapshot(String version) {
    return version.toUpperCase().replace(ConstsUtil.SNAPSHOT, "").trim();
  }

  @Override
  public ParameterValue createValue(StaplerRequest staplerRequest) {
    String value[] = staplerRequest.getParameterValues(getName());
    return new CabalBrasilParameterValue(getName(), value[0]);
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Extension
  public static final class DescriptorImpl extends ParameterDescriptor {

    @Override
    public String getDisplayName() {
      return ConstsUtil.PLUGIN_NAME;
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
