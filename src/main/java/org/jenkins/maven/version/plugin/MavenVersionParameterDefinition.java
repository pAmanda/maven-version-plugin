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
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class MavenVersionParameterDefinition extends ParameterDefinition {

  public static final String EMPTY_JOB_NAME = "EMPTY_JOB_NAME";

  public static final Logger logger = Logger.getLogger(MavenVersionParameterDefinition.class.getName());

  private final UUID uuid;

  private String defaultValue = "0";

  @DataBoundConstructor
  public MavenVersionParameterDefinition(String name, String description) {
    super(name, description);
    this.uuid = UUID.randomUUID();
  }

  @Override
  public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject) {
    System.out.println("Name: " + getName() + ", Description: " + getDescription());
    Object value = jsonObject.get("value");
    StringBuilder strValue = new StringBuilder();
    if (value instanceof String) {
      strValue.append(value);
    } else if (value instanceof JSONArray) {
      JSONArray jsonValues = (JSONArray) value;
      for (int i = 0; i < jsonValues.size(); i++) {
        strValue.append(jsonValues.getString(i));
        if (i < jsonValues.size() - 1) {
          strValue.append(",");
        }
      }
    }
    System.out.println("Path: " + getCustomeJobName());
    System.out.println("Version: " + getPomVersion());

    return new MavenVersionParameterValue(getName(), strValue.toString());
  }

  public String getPomVersion(){
    System.out.println("Lendo o pom.");
    final File rootPomFile = new File(getJobWorspace() + "/pom.xml");
    if(rootPomFile.exists()) {
      System.out.println("Pom existe");

      try {
        final Model mavenModels = parseMavenModel(rootPomFile);
        System.out.println("Pegando a versão. : " + rootPomFile.getAbsolutePath());
        return mavenModels.getVersion();
      } catch (XmlPullParserException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return null;

  }

  public Model parseMavenModel(File rootPomFile) throws XmlPullParserException, IOException {
    MavenXpp3Reader reader = new MavenXpp3Reader();
    Model model = reader.read(new FileReader(rootPomFile));
    return model;
  }

  public int compareTo(MavenVersionParameterDefinition pd) {
    return pd.uuid.equals(uuid) ? 0 : -1;
  }

  public String getJobWorspace(){
    return Jenkins.getInstance().getItem(getCustomeJobName()).getRootDir().toString();
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

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public ParameterValue createValue(StaplerRequest staplerRequest) {
    System.out.println("Create value witch 1 argument");
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
