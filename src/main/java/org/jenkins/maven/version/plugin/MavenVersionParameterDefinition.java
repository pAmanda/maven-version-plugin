package org.jenkins.maven.version.plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import hudson.Extension;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.util.FormValidation;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkins.maven.version.plugin.maven.ReleaseVersion;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class MavenVersionParameterDefinition extends ParameterDefinition {

    public static final Logger logger = Logger.getLogger(MavenVersionParameterDefinition.class.getName());

    @DataBoundConstructor
    public MavenVersionParameterDefinition(String name, String description) {
        super(name, description);
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

        Jenkins jenkins = Jenkins.getInstance();
        List<AbstractProject> abstractProject = jenkins.getItems(AbstractProject.class);

        System.out.println("STR: " + strValue.toString());
      for (AbstractProject project : abstractProject) {
        System.out.println("FOr: " + project.getSomeWorkspace());
      }

        return new MavenVersionParameterValue(getName(), strValue.toString());
    }

//    protected File getProjectWorkDir() {
//        final RunList<MavenModuleSetBuild> builds = project.getBuilds();
//        // This code assumes that the builds work dir stays the same during builds.
//        final MavenModuleSetBuild build = builds.getLastBuild();
//        // Convert the FilePath into a File object that we can actually use.
//        try {
//          return new File(build.getWorkspace().toURI().getPath());
//        } catch (IOException e) {
//          e.printStackTrace();
//        } catch (InterruptedException e) {
//          e.printStackTrace();
//        }
//        return null;
//    }

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
