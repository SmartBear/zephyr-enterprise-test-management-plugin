package com.thed.zephyr.jenkins.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import hudson.tasks.junit.CaseResult;

public class ZephyrConfigModel {

	private Set<String> packageNames;
	private String cyclePrefix;
	private String cycleDuration;
	private String environment;
	private long zephyrProjectId;
	private long releaseId;
	private long cycleId;
	private long userId;
	private boolean createPackage;
	private ZephyrInstance selectedZephyrServer;
	private int builNumber;
    private Map<String, List<CaseResult>> packageCaseResultMap;
    private String resultXmlFilePath;
    private long parserTemplateId;
    private String jsonParserTemplate;


	public boolean isCreatePackage() {
		return createPackage;
	}

	public void setCreatePackage(boolean createPackage) {
		this.createPackage = createPackage;
	}

	public String getCyclePrefix() {
		return cyclePrefix;
	}

	public void setCyclePrefix(String cyclePrefix) {
		this.cyclePrefix = cyclePrefix;
	}

	public long getZephyrProjectId() {
		return zephyrProjectId;
	}

	public void setZephyrProjectId(long zephyrProjectId) {
		this.zephyrProjectId = zephyrProjectId;
	}

	public long getReleaseId() {
		return releaseId;
	}

	public void setReleaseId(long releaseId) {
		this.releaseId = releaseId;
	}

	public long getCycleId() {
		return cycleId;
	}

	public void setCycleId(long cycleId) {
		this.cycleId = cycleId;
	}

	public Set<String> getPackageNames() {
		return packageNames;
	}

	public void setPackageNames(Set<String> packageNames) {
		this.packageNames = packageNames;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getCycleDuration() {
		return cycleDuration;
	}

	public void setCycleDuration(String cycleDuration) {
		this.cycleDuration = cycleDuration;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public void setSelectedZephyrServer(ZephyrInstance selectedZephyrServer) {
		this.selectedZephyrServer = selectedZephyrServer;
	}

	public ZephyrInstance getSelectedZephyrServer() {
		return selectedZephyrServer;
	}

	public int getBuilNumber() {
		return builNumber;
	}

	public void setBuilNumber(int builNumber) {
		this.builNumber = builNumber;
	}

    public Map<String, List<CaseResult>> getPackageCaseResultMap() {
        return packageCaseResultMap;
    }

    public void setPackageCaseResultMap(Map<String, List<CaseResult>> packageCaseResultMap) {
        this.packageCaseResultMap = packageCaseResultMap;
    }

    public String getResultXmlFilePath() {
        return resultXmlFilePath;
    }

    public void setResultXmlFilePath(String resultXmlFilePath) {
        this.resultXmlFilePath = resultXmlFilePath;
    }

    public long getParserTemplateId() {
        return parserTemplateId;
    }

    public void setParserTemplateId(long parserTemplateId) {
        this.parserTemplateId = parserTemplateId;
    }

    public String getJsonParserTemplate() {
        return jsonParserTemplate;
    }

    public void setJsonParserTemplate(String jsonParserTemplate) {
        this.jsonParserTemplate = jsonParserTemplate;
    }
}
