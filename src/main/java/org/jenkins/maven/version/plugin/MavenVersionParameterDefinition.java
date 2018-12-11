package org.jenkins.maven.version.plugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.jenkins.maven.version.plugin.job.JobWrapper;
import org.jenkins.maven.version.plugin.job.JobWrapperFactory;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class MavenVersionParameterDefinition extends ParameterDefinition {

  public static final Logger logger = Logger.getLogger(MavenVersionParameterDefinition.class.getName());
  public static final String EMPTY_JOB_NAME = "EMPTY_JOB_NAME";
  public static final String SNAPSHOT = "-SNAPSHOT";
  public static final String REFS_TAGS_PATTERN = ".*refs/tags/";
  private final UUID uuid;
  public String typeDeploy;

  public String getTypeDeploy() {
    return typeDeploy;
  }

  public void setTypeDeploy(String typeDeploy) {
    this.typeDeploy = typeDeploy;
  }

  @DataBoundConstructor
  public MavenVersionParameterDefinition(String name, String description) {
    super(name, description);
    this.uuid = UUID.randomUUID();
  }

  @Override
  public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject) {
    String name = getName();
    String parameters = "VERSION: " + staplerRequest.getParameter(name + "_version") + "; NEXT_VERSION: " + staplerRequest.getParameter(name + "_nextVersion");
    System.out.println("Chamando test SCM");
    System.out.println("Type deploy: " + getTypeDeploy());
    testScm();
    MavenVersionParameterValue mavenVersionParameterValue = new MavenVersionParameterValue(name, parameters);
    return mavenVersionParameterValue;
  }

  /** GIT **/

  public void testScm() {
    Job job = this.getParentJob();
    JobWrapper jobWrapper = JobWrapperFactory.createJobWrapper(job);

    ParametersDefinitionProperty prop = jobWrapper.getProperty(ParametersDefinitionProperty.class);

    List<SCM> scms = jobWrapper.getScms();

    List<GitSCM> gitSCMs = getFirstGitScm(scms);

    if(gitSCMs == null || gitSCMs.isEmpty()) {
      System.out.println("SCM está vazio");
    } else {
      generateContents(jobWrapper, gitSCMs);
    }
  }

  private void generateContents(JobWrapper jobWrapper, List<GitSCM> gitSCMS) {
    try {
      EnvVars environment = getEnvironment(jobWrapper);

      for (GitSCM gitSCM : gitSCMS) {
        for (RemoteConfig remoteConfig: gitSCM.getRepositories()) {
          GitClient gitClient = getGitClient(jobWrapper, null, gitSCM, environment);
          for (URIish urIish : remoteConfig.getURIs()) {
            String gitUrl = Util.replaceMacro(urIish.toPrivateASCIIString(), environment);

            System.out.println("GIR URL: " + gitUrl);
            System.out.println("Repository: " + remoteConfig.getName());

            try {
              Set<String> branchs = getBranch(gitClient, gitUrl, remoteConfig.getName());
              for (String branch: branchs) {
                System.out.println("Branch: " + branchs);
              }
            } catch (Exception e) {
              e.printStackTrace();
            }

            Set<String> tags = getTag(gitClient, gitUrl);
            for (String tag: tags) {
              System.out.println("Tag: " + tag);
            }
          }
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static List<String> getExamples() {
    List<String> list = new ArrayList<String>();
    list.add("Amanda");
    list.add("Vieira");
    list.add("Pires");
    return list;
  }

  private Set<String> getTag(GitClient gitClient, String gitUrl) throws InterruptedException {
    Set<String> tagSet = new HashSet<String>();
    try {
      Map<String, ObjectId> tags = gitClient.getRemoteReferences(gitUrl, "*", false, true);
      for (String tagName : tags.keySet()) {
        tagSet.add(tagName.replaceFirst(REFS_TAGS_PATTERN, ""));
      }
    } catch (GitException e) {
      e.printStackTrace();
    }
    return tagSet;
  }

  private Set<String> getBranch(GitClient gitClient, String gitUrl, String remoteName) throws Exception {
    Set<String> branchSet = new HashSet<>();
    Pattern branchFilterPattern = compileBranchFilterPattern();

    Map<String, ObjectId> branches = gitClient.getRemoteReferences(gitUrl, null, true, false);
    Iterator<String> remoteBranchesName = branches.keySet().iterator();
    while (remoteBranchesName.hasNext()) {
      String branchName = strip(remoteBranchesName.next(), remoteName);
      Matcher matcher = branchFilterPattern.matcher(branchName);
      if (matcher.matches()) {
        if (matcher.groupCount() == 1) {
          branchSet.add(matcher.group(1));
        } else {
          branchSet.add(branchName);
        }
      }
    }
    return branchSet;
  }

  private String strip(String name, String remote) {
    return remote + "/" + name.substring(name.indexOf('/', 5) + 1);
  }

  private Pattern compileBranchFilterPattern() {
    Pattern branchFilterPattern;
    try {
      branchFilterPattern = Pattern.compile("origin/hotfix/.*|origin/master");
    } catch (Exception e) {
      branchFilterPattern = Pattern.compile(".*");
    }
    return branchFilterPattern;
  }

  private GitClient getGitClient(final JobWrapper jobWrapper, FilePathWrapper workspace, GitSCM git, EnvVars environment) throws IOException, InterruptedException {
    int nextBuildNumber = jobWrapper.getNextBuildNumber();

    GitClient gitClient = git.createClient(TaskListener.NULL, environment, new Run(jobWrapper.getJob()) {
    }, workspace != null ? workspace.getFilePath() : null);

    jobWrapper.updateNextBuildNumber(nextBuildNumber);
    return gitClient;
  }

  private EnvVars getEnvironment(JobWrapper jobWrapper) throws IOException, InterruptedException {
    EnvVars environment = jobWrapper.getEnvironment(Jenkins.getInstance().toComputer().getNode(), TaskListener.NULL);
    EnvVars buildEnvironments = jobWrapper.getSomeBuildEnvironments();
    if (buildEnvironments != null) {
      environment.putAll(buildEnvironments);
    }

    EnvVars jobDefautEnvironments = getJobDefaultEnvironment(jobWrapper);
    environment.putAll(jobDefautEnvironments);

    EnvVars.resolve(environment);
    return environment;
  }

  private EnvVars getJobDefaultEnvironment(JobWrapper jobWrapper) {
    EnvVars environment = new EnvVars();
    ParametersDefinitionProperty property = (ParametersDefinitionProperty) jobWrapper.getJob().getProperty(ParametersDefinitionProperty.class);
    if (property != null) {
      for (ParameterDefinition parameterDefinition : property.getParameterDefinitions()) {
        if (parameterDefinition != null && isAcceptedParameterClass(parameterDefinition)) {
          checkAndAddDefaultParameterValue(parameterDefinition, environment);
        }
      }
    }
    return environment;
  }

  private boolean isAcceptedParameterClass(ParameterDefinition parameterDefinition) {
    return parameterDefinition instanceof StringParameterDefinition
            || parameterDefinition instanceof ChoiceParameterDefinition;
  }


  private void checkAndAddDefaultParameterValue(ParameterDefinition parameterDefinition, EnvVars environment) {
    ParameterValue defaultParameterValue = parameterDefinition.getDefaultParameterValue();
    if (defaultParameterValue != null && defaultParameterValue.getValue() != null && defaultParameterValue.getValue() instanceof String) {
      environment.put(parameterDefinition.getName(), (String) defaultParameterValue.getValue());
    }
  }

  private List<GitSCM> getFirstGitScm(List<SCM> scms){
    SCM scm = scms.get(0);
    if (scm instanceof GitSCM) {
      return Collections.singletonList((GitSCM) scm);
    }
    return Collections.EMPTY_LIST;
  }

  /** MAVEN **/

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
