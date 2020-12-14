package com.thed.zephyr.jenkins.reporter;

/**
 * @author mohan
 */

import static com.thed.zephyr.jenkins.reporter.ZeeConstants.ADD_ZEPHYR_GLOBAL_CONFIG;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.CYCLE_PREFIX_DEFAULT;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.NEW_CYCLE_KEY;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.NEW_CYCLE_KEY_IDENTIFIER;

import com.google.gson.Gson;
import com.thed.model.*;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.thed.model.CyclePhase;
import com.thed.model.ReleaseTestSchedule;
import com.thed.model.TCRCatalogTreeDTO;
import com.thed.model.TCRCatalogTreeTestcase;
import com.thed.service.*;
import com.thed.service.impl.*;
import com.thed.utils.EggplantParser;
import com.thed.utils.ParserUtil;
import com.thed.utils.ZephyrConstants;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.*;
import hudson.tasks.junit.*;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.ChildReport;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.DataBoundConstructor;

import com.thed.zephyr.jenkins.model.ZephyrConfigModel;
import com.thed.zephyr.jenkins.model.ZephyrInstance;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

public class ZeeReporter extends Notifier implements SimpleBuildStep {

	private String projectKey;
	private String releaseKey;
	private String cycleKey;
	private String cyclePrefix;
	private String serverAddress;
	private String cycleDuration;
	private boolean createPackage;
    private String resultXmlFilePath;
    private Long eggplantParserIndex = 3l;
    private String parserTemplateKey;


	public static PrintStream logger;
	private static final String PluginName = "[Zephyr Enterprise Test Management";
    private final String pInfo = String.format("%s [INFO]", PluginName);
    private String jenkinsProjectName;


    private UserService userService = new UserServiceImpl();
    private ProjectService projectService = new ProjectServiceImpl();
    private TCRCatalogTreeService tcrCatalogTreeService = new TCRCatalogTreeServiceImpl();
    private RequirementService requirementService = new RequirementServiceImpl();
    private TestcaseService testcaseService = new TestcaseServiceImpl();
    private CycleService cycleService = new CycleServiceImpl();
    private ExecutionService executionService = new ExecutionServiceImpl();
    private AttachmentService attachmentService = new AttachmentServiceImpl();
    private ParserTemplateService parserTemplateService = new ParserTemplateServiceImpl();
    private TestStepService testStepService = new TestStepServiceImpl();

	@DataBoundConstructor
	public ZeeReporter(String serverAddress, String projectKey,
			String releaseKey, String cycleKey, String cyclePrefix,
			String cycleDuration, boolean createPackage, String resultXmlFilePath, String parserTemplateKey) {
		this.serverAddress = serverAddress;
		this.projectKey = projectKey;
		this.releaseKey = releaseKey;
		this.cycleKey = cycleKey;
		this.cyclePrefix = cyclePrefix;
		this.createPackage = createPackage;
		this.cycleDuration = cycleDuration;
        this.resultXmlFilePath = resultXmlFilePath;
        this.parserTemplateKey = parserTemplateKey;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        workspace.act(getUploadResultCallable(run, listener));
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher,
                           final BuildListener listener) throws IOException, InterruptedException {
        return build.getWorkspace().act(getUploadResultCallable(build, listener));
    }

    private UploadResultCallable getUploadResultCallable(Run build, TaskListener listener) {
        ZephyrInstance zephyrInstance = getZephyrInstance(serverAddress);
        StandardUsernamePasswordCredentials standardCredentials = getCredentialsFromId(zephyrInstance.getCredentialsId());

        return new UploadResultCallable(serverAddress, projectKey, releaseKey, cycleKey, cyclePrefix, cycleDuration, createPackage,
                resultXmlFilePath, parserTemplateKey, listener, build.getNumber(), standardCredentials);
    }

    private ZephyrInstance getZephyrInstance(String serverAddress) {
        List<ZephyrInstance> zephyrServers = getDescriptor().getZephyrInstances();

        for (ZephyrInstance zephyrInstance : zephyrServers) {
            if (StringUtils.isNotBlank(zephyrInstance.getServerAddress()) && zephyrInstance.getServerAddress().trim().equals(serverAddress)) {
                return zephyrInstance;
            }
        }
        return null;
    }

    private StandardUsernamePasswordCredentials getCredentialsFromId(String credentialsId) {
        Iterable<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
                Jenkins.getInstance(),
                ACL.SYSTEM,
                Collections.<DomainRequirement>emptyList());

        return CredentialsMatchers.firstOrNull(
                credentials,
                CredentialsMatchers.withId(credentialsId));
    }

	@Override
	public ZeeDescriptor getDescriptor() {
		return (ZeeDescriptor) super.getDescriptor();
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	public String getReleaseKey() {
		return releaseKey;
	}

	public void setReleaseKey(String releaseKey) {
		this.releaseKey = releaseKey;
	}

	public String getCycleKey() {
		return cycleKey;
	}

	public void setCycleKey(String cycleKey) {
		this.cycleKey = cycleKey;
	}

	public String getCyclePrefix() {
		return cyclePrefix;
	}

	public void setCyclePrefix(String cyclePrefix) {
		this.cyclePrefix = cyclePrefix;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public String getCycleDuration() {
		return cycleDuration;
	}

	public void setCycleDuration(String cycleDuration) {
		this.cycleDuration = cycleDuration;
	}

	public boolean isCreatePackage() {
		return createPackage;
	}

	public void setCreatePackage(boolean createPackage) {
		this.createPackage = createPackage;
	}

    public String getResultXmlFilePath() {
        return resultXmlFilePath;
    }

    public void setResultXmlFilePath(String resultXmlFilePath) {
        this.resultXmlFilePath = resultXmlFilePath;
    }

    public String getParserTemplateKey() {
        return parserTemplateKey;
    }

    public void setParserTemplateKey(String parserTemplateKey) {
        this.parserTemplateKey = parserTemplateKey;
    }
}
