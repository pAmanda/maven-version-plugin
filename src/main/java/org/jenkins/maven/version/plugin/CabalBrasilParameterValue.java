package org.jenkins.maven.version.plugin;

import hudson.model.StringParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;

public class CabalBrasilParameterValue extends StringParameterValue {

    @DataBoundConstructor
    public CabalBrasilParameterValue(String name, String value) {
        super(name, value);
    }

}
