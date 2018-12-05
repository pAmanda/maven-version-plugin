package org.jenkins.maven.version.plugin;

import hudson.model.StringParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;

public class MavenVersionParameterValue extends StringParameterValue {

    @DataBoundConstructor
    public MavenVersionParameterValue(String name, String value) {
        super(name, value);
    }

}
