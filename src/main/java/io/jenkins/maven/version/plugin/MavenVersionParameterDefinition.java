package io.jenkins.maven.version.plugin;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MavenVersionParameterDefinition extends ParameterDefinition {

  public static final Logger logger = Logger.getLogger(MavenVersionParameterDefinition.class.getName());

  @DataBoundConstructor
  public MavenVersionParameterDefinition(String name, String description) {
    super(name, description);
  }

  @Override
  public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject) {
    MavenVersionParameterValue mavenVersionParameterValue = new MavenVersionParameterValue(getName(), getDescription());
    mavenVersionParameterValue.setVersion(staplerRequest.getParameter(getName() + "_version"));
    mavenVersionParameterValue.setNextVersion(staplerRequest.getParameter(getName() + "_nextVersion"));
    return mavenVersionParameterValue;
  }

  @Override
  public ParameterValue createValue(StaplerRequest staplerRequest) {
    return new MavenVersionParameterValue(getName(), getDescription(), "0", "0");
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public String getRootUrl() {
    return Jenkins.getInstance().getRootUrl();
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
