package com.thed.zephyr.jenkins.reporter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.google.gson.Gson;
import com.thed.model.*;
import com.thed.parser.ParserUtil;
import com.thed.service.*;
import com.thed.service.impl.*;
import com.thed.utils.EggplantParser;
import com.thed.utils.ListUtil;
import com.thed.utils.ZephyrConstants;
import com.thed.zephyr.jenkins.model.ZephyrConfigModel;
import com.thed.zephyr.jenkins.model.ZephyrInstance;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.thed.zephyr.jenkins.reporter.ZeeConstants.*;
import static com.thed.zephyr.jenkins.reporter.ZeeConstants.ADD_ZEPHYR_GLOBAL_CONFIG;

public class UploadResultCallable extends MasterToSlaveFileCallable<Boolean> {

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
    private TaskListener listener;
    private StandardCredentials standardCredentials;
    private int buildNumber;

    private File workspaceFile;

    private static final String PluginName = "[Zephyr Enterprise Test Management";
    private final String pInfo = String.format("%s [INFO]", PluginName);

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
    private PreferenceService preferenceService = new PreferenceServiceImpl();

    public UploadResultCallable(String serverAddress, String projectKey,
                                String releaseKey, String cycleKey, String cyclePrefix,
                                String cycleDuration, boolean createPackage, String resultXmlFilePath, String parserTemplateKey,
                                TaskListener listener, int buildNumber, StandardCredentials standardCredentials) {
        this.serverAddress = serverAddress;
        this.projectKey = projectKey;
        this.releaseKey = releaseKey;
        this.cycleKey = cycleKey;
        this.cyclePrefix = cyclePrefix;
        this.createPackage = createPackage;
        this.cycleDuration = cycleDuration;
        this.resultXmlFilePath = resultXmlFilePath;
        this.parserTemplateKey = parserTemplateKey;
        this.listener = listener;
        this.buildNumber = buildNumber;
        this.standardCredentials = standardCredentials;
    }

    @Override
    public Boolean invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
        if (file != null && file.exists()) {
            workspaceFile = file;
            return perform(buildNumber, listener);
        } else {
            listener.getLogger().println("Workspace doesn't exist.");
        }
        return false;
    }

    public boolean perform(int buildNumber, final TaskListener listener) throws IOException, InterruptedException {
        PrintStream logger = listener.getLogger();
        logger.printf("%s Examining test results...%n", pInfo);

        if (!validateBuildConfig()) {
            logger.println("Cannot Proceed. Please verify the job configuration");
            return false;
        }

        try {
            //login to zephyr server
            StandardCredentials upCredentials = getStandardCredentials();
            Boolean loggedIn = Boolean.FALSE;
            if(upCredentials instanceof UsernamePasswordCredentialsImpl){
                String user = ((UsernamePasswordCredentialsImpl) upCredentials).getUsername();
                String pass = ((UsernamePasswordCredentialsImpl) upCredentials).getPassword().getPlainText();
                loggedIn = userService.login(getServerAddress(),user,pass);
            }else if(upCredentials instanceof StringCredentialsImpl){
                String secretText = ((StringCredentialsImpl) upCredentials).getSecret().getPlainText();
                loggedIn = userService.login(getServerAddress(), secretText);
            }
            if(!loggedIn) {
                logger.println("Authorization for zephyr server failed.");
                return false;
            }

            ZephyrConfigModel zephyrConfigModel = new ZephyrConfigModel();
            zephyrConfigModel.setZephyrProjectId(Long.parseLong(getProjectKey()));
            zephyrConfigModel.setReleaseId(Long.parseLong(getReleaseKey()));
            zephyrConfigModel.setParserTemplateId(Long.parseLong(getParserTemplateKey()));
            zephyrConfigModel.setJsonParserTemplate(parserTemplateService.getParserTemplateById(zephyrConfigModel.getParserTemplateId()).getJsonTemplate());

            if (cycleKey.equalsIgnoreCase(NEW_CYCLE_KEY)) {
                zephyrConfigModel.setCycleId(NEW_CYCLE_KEY_IDENTIFIER);
            }
            else {
                zephyrConfigModel.setCycleId(Long.parseLong(getCycleKey()));
            }

            zephyrConfigModel.setCycleDuration(getCycleDuration());

            if (StringUtils.isNotBlank(getCyclePrefix())) {
                zephyrConfigModel.setCyclePrefix(getCyclePrefix() + "_");
            } else {
                zephyrConfigModel.setCyclePrefix(CYCLE_PREFIX_DEFAULT);
            }

            zephyrConfigModel.setCreatePackage(isCreatePackage());
            zephyrConfigModel.setResultXmlFilePath(getResultXmlFilePath());

            zephyrConfigModel.setBuilNumber(buildNumber);

            //creating Map<testcaseName, passed>, Set<packageName> and set to zephyrConfigModel

            List<Map> dataMapList = new ArrayList<>();

            Set<String> xmlFiles = new HashSet<>();

            List<String> resultFilePathList = getAllIncludedFilePathList(workspaceFile.getAbsolutePath(), resultXmlFilePath);

            for(String resultFilePath : resultFilePathList) {
                if(Objects.equals(zephyrConfigModel.getParserTemplateId(), eggplantParserIndex)) {
                    //use eggplant parser to find all related xml files
                    EggplantParser eggplantParser = new EggplantParser("unknownSUT", "url");
                    List<EggPlantResult> eggPlantResults = eggplantParser.invoke(new File(resultFilePath));
                    xmlFiles.addAll(getTestcasesForEggplant(eggPlantResults));
                } else {
                    xmlFiles.add(resultFilePath);
                }
            }

            for(String xmlFilePath : xmlFiles) {

                List<Map> currentDataMapList = genericParserXML(xmlFilePath, zephyrConfigModel.getJsonParserTemplate());
                File file = new File(xmlFilePath);

                for(Map dataMap : currentDataMapList) {
                    dataMap.put("basePath", file.getParent());
                }
                dataMapList.addAll(currentDataMapList);
            }

            //zephyrConfigModel.setPackageNames(getPackageNamesFromXML(dataMapList));
            zephyrConfigModel.setPackageNames(getPackageNamesFromXML(dataMapList));
            Map<String, TCRCatalogTreeDTO> packagePhaseMap = createPackagePhaseMap(zephyrConfigModel);
            Map<TCRCatalogTreeTestcase, Map<String, Object>> tcrStatusMap = createTestcasesFromMap(packagePhaseMap, dataMapList, zephyrConfigModel, logger);

            logger.println("Total Test Cases : " + tcrStatusMap.keySet().size());

            com.thed.model.Project project = projectService.getProjectById(zephyrConfigModel.getZephyrProjectId());

            com.thed.model.Cycle cycle;

            if(zephyrConfigModel.getCycleId() == NEW_CYCLE_KEY_IDENTIFIER) {

                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("E_dd MMM yyyy_HH:mm");
                String dateFormatForCycleCreation = sdf.format(date);

                String cycleName = zephyrConfigModel.getCyclePrefix() + dateFormatForCycleCreation;

                cycle = new com.thed.model.Cycle();
                cycle.setName(cycleName);
                cycle.setReleaseId(zephyrConfigModel.getReleaseId());
                cycle.setBuild(String.valueOf(zephyrConfigModel.getBuilNumber()));
                cycle.setStartDate(project.getStartDate());

                Date projectStartDate = project.getStartDate();

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(projectStartDate);


                if(zephyrConfigModel.getCycleDuration().equals("30 days")) {
                    calendar.add(Calendar.DAY_OF_MONTH, 29);
                }
                else if(zephyrConfigModel.getCycleDuration().equals("7 days")) {
                    calendar.add(Calendar.DAY_OF_MONTH, 6);
                }
                cycle.setEndDate(calendar.getTime());

                cycle = cycleService.createCycle(cycle);
                zephyrConfigModel.setCycleId(cycle.getId());
            }
            else {
                cycle = cycleService.getCycleById(zephyrConfigModel.getCycleId());
            }

            CyclePhase cyclePhase = new CyclePhase();
            SimpleDateFormat sdf = new SimpleDateFormat("_E_dd MMM yyyy_HH:mm");
            String timeFormatForCyclePhaseCreation = sdf.format(new Date());
            String phaseName = packagePhaseMap.get("parentPhase").getName() + timeFormatForCyclePhaseCreation;
            cyclePhase.setName(phaseName);
            cyclePhase.setCycleId(cycle.getId());
            cyclePhase.setStartDate(cycle.getStartDate());
            cyclePhase.setEndDate(cycle.getEndDate());
            cyclePhase.setReleaseId(zephyrConfigModel.getReleaseId());
            cyclePhase.setFreeForm(true);
            cyclePhase = cycleService.createCyclePhase(cyclePhase);

            //adding testcases to free form cycle phase
            cycleService.addTestcasesToFreeFormCyclePhase(cyclePhase, new ArrayList<>(tcrStatusMap.keySet()), zephyrConfigModel.isCreatePackage());

            //assigning testcases in cycle phase to creator
            List<ReleaseTestSchedule> releaseTestSchedules = cycleService.assignCyclePhaseToUser(cyclePhase, userService.getCurrentUser().getId());

            Map<String, Set<Long>> executionMap = new HashMap<>();

            Map<Long, List<String>> testcaseAttachmentsMap = new HashMap<>();
            Map<Long, GenericAttachmentDTO> statusAttachmentMap = new HashMap<>();

            List<TestStepResult> testStepResultList = new ArrayList<>();

            loop1 : for(Map.Entry<TCRCatalogTreeTestcase, Map<String, Object>> caseEntry : tcrStatusMap.entrySet()) {

                for(ReleaseTestSchedule releaseTestSchedule : releaseTestSchedules) {

                    if(Objects.equals(releaseTestSchedule.getTcrTreeTestcase().getTestcase().getId(), caseEntry.getKey().getTestcase().getId())) {
                        // tcrTestcase matched, map caseResult.isPass status to rtsId
                        Map<String, Object> testcaseValueMap = caseEntry.getValue();

                        if(testcaseValueMap.containsKey("statusId")) {
                            String statusId = testcaseValueMap.get("statusId").toString();
                            if(!executionMap.containsKey(statusId)) {
                                executionMap.put(statusId, new HashSet<Long>());
                            }
                            executionMap.get(statusId).add(releaseTestSchedule.getId());
                        }

                        if(testcaseValueMap.containsKey("attachments")) {
                            List<String> attachmentPathList = (List<String>)testcaseValueMap.get("attachments");
                            if(!attachmentPathList.isEmpty()) {
                                testcaseAttachmentsMap.put(releaseTestSchedule.getId(), attachmentPathList);
                            }
                        }

                        if(testcaseValueMap.containsKey("statusAttachment")) {
                            statusAttachmentMap.put(releaseTestSchedule.getId(), (GenericAttachmentDTO) testcaseValueMap.get("statusAttachment"));
                        }

                        TCRCatalogTreeTestcase tcrTestCase = caseEntry.getKey();

                        //testStepResult handled here
                        if(testcaseValueMap.containsKey("stepList")) {
                            //we have parsed steps from xml, this testcase existed before and the teststeps weren't fetched, fetching now
                            tcrTestCase.getTestcase().setTestSteps(testStepService.getTestStep(tcrTestCase.getTestcase().getId()));
                        }

                        if(tcrTestCase.getTestcase().getTestSteps() != null && tcrTestCase.getTestcase().getTestSteps().getSteps() != null) {
                            List<Map<String, String>> stepList = (List<Map<String, String>>)testcaseValueMap.get("stepList");

                            for (TestStepDetail testStepDetail : tcrTestCase.getTestcase().getTestSteps().getSteps()) {
                                for (Map<String, String> stepMap : stepList) {

                                    if(testStepDetail.getStep().equals(stepMap.get("step"))) {
                                        TestStepResult testStepResult = new TestStepResult();
                                        testStepResult.setCyclePhaseId(releaseTestSchedule.getCyclePhaseId());
                                        testStepResult.setReleaseTestScheduleId(releaseTestSchedule.getId());
                                        testStepResult.setTestStepId(testStepDetail.getOrderId());

                                        String status = "";

                                        if(stepMap.get("status").equalsIgnoreCase("true")) {
                                            status = ZephyrConstants.EXECUTION_STATUS_PASS;
                                        } else if(stepMap.get("status").equalsIgnoreCase("false")) {
                                            status = ZephyrConstants.EXECUTION_STATUS_FAIL;
                                        } else {
                                            status = ZephyrConstants.EXECUTION_STATUS_NOT_EXECUTED;
                                        }
                                        testStepResult.setStatus(Long.parseLong(status));
                                        testStepResultList.add(testStepResult);
                                        stepList.remove(stepMap);
                                        break;
                                    }
                                }
                            }
                        }

                        continue loop1;
                    }
                }
            }

            List<String> errorLogs = attachmentService.addAttachments(AttachmentService.ItemType.releaseTestSchedule, testcaseAttachmentsMap, statusAttachmentMap);
            errorLogs.forEach(errorLog -> logger.println(errorLog));

            if(!testStepResultList.isEmpty()) {
                executionService.addTestStepResults(testStepResultList);
            }

            Set<String> activeStatusIdSet = preferenceService.getTestcaseExecutionStatusIds(true);
            for(Map.Entry<String, Set<Long>> entry : executionMap.entrySet()) {
                String statusId = entry.getKey();
                if(activeStatusIdSet.contains(statusId)) {
                    executionService.executeReleaseTestSchedules(entry.getValue(), statusId);
                } else {
                    logger.println("No active testcase execution status found for id: " + statusId);
                }
            }
        }
        catch(Exception e) {
            //todo:handle exceptions gracefully
            e.printStackTrace();
            logger.println("Error uploading test results to Zephyr");
            logger.println(e.getMessage());
            for(StackTraceElement stackTraceElement : e.getStackTrace()) {
                logger.println(stackTraceElement.toString());
            }
            return false;
        } finally {
            userService.getZephyrRestService().closeHttpConnection();
        }

        logger.printf("%s Done uploading tests to Zephyr.%n", pInfo);
        return true;
    }

    private Map<String, TCRCatalogTreeDTO> createPackagePhaseMap(ZephyrConfigModel zephyrConfigModel) throws URISyntaxException, IOException {

        List<TCRCatalogTreeDTO> tcrCatalogTreeDTOList = tcrCatalogTreeService.getTCRCatalogTreeNodes(ZephyrConstants.TCR_CATALOG_TREE_TYPE_PHASE, zephyrConfigModel.getReleaseId());

        String phaseDescription = zephyrConfigModel.isCreatePackage() ? ZephyrConstants.PACKAGE_TRUE_DESCRIPTION : ZephyrConstants.PACKAGE_FALSE_DESCRIPTION;
        boolean createPhase = true;
        TCRCatalogTreeDTO automationPhase = null;
        for(TCRCatalogTreeDTO tcrCatalogTreeDTO : tcrCatalogTreeDTOList) {
            if(tcrCatalogTreeDTO.getName().equals(ZephyrConstants.TOP_PARENT_PHASE_NAME)) {
                if((zephyrConfigModel.isCreatePackage() && tcrCatalogTreeDTO.getDescription().equals(ZephyrConstants.PACKAGE_TRUE_DESCRIPTION))
                        || (!zephyrConfigModel.isCreatePackage() && tcrCatalogTreeDTO.getDescription().equals(ZephyrConstants.PACKAGE_FALSE_DESCRIPTION))) {
                    automationPhase = tcrCatalogTreeDTO;
                    createPhase = false;
                }
            }
        }

        if(createPhase) {
            //top parent phase is not available which means we have to create this and all following phases
            automationPhase = tcrCatalogTreeService.createPhase(ZephyrConstants.TOP_PARENT_PHASE_NAME,
                    phaseDescription,
                    zephyrConfigModel.getReleaseId(),
                    0l);
        }

        Map<String, TCRCatalogTreeDTO> packagePhaseMap = new HashMap<>();
        packagePhaseMap.put("parentPhase", automationPhase);

        if(!zephyrConfigModel.isCreatePackage()) {
            return packagePhaseMap;
        }

        Set<String> packageNames = zephyrConfigModel.getPackageNames();

        for(String packageName : packageNames) {

            String[] packageNameArr = packageName.split("\\.");
            //todo: make hierarchy locally instead of these extra rest calls
            automationPhase = tcrCatalogTreeService.getTCRCatalogTreeNode(automationPhase.getId()); //To refresh child nodes already created in the loop below
            TCRCatalogTreeDTO endingNode = automationPhase; //This node is where testcases will be created, it is the last node to be created in this package hierarchy
            createPhase = false;
            for (int i = 0; i < packageNameArr.length; i++) {
                String pName = packageNameArr[i];

                if(createPhase) {
                    //last phase searched doesn't exist and was created new, any following phases need to be created
                    endingNode = tcrCatalogTreeService.createPhase(pName, phaseDescription, zephyrConfigModel.getReleaseId(), endingNode.getId());
                }
                else {
                    //endingNode exists for this package level and we can search for following nodes

                    TCRCatalogTreeDTO searchedPhase = null;

                    if(endingNode.getCategories() != null && endingNode.getCategories().size() > 0) {
                        for (TCRCatalogTreeDTO tct : endingNode.getCategories()) {
                            if(tct.getName().equals(pName)) {
                                searchedPhase = tct;
                            }
                        }
                    }

                    if(searchedPhase == null) {
                        //no more children to this phase to search, need to create new phases
                        searchedPhase = tcrCatalogTreeService.createPhase(pName, phaseDescription, zephyrConfigModel.getReleaseId(), endingNode.getId());
                        createPhase = true;
                    }
                    endingNode = searchedPhase;
                }
            }
            packagePhaseMap.put(packageName, endingNode);
        }
        return packagePhaseMap;
    }

    private List<TCRCatalogTreeTestcase> createTestcasesWithoutDuplicate(Map<Long, List<Testcase>> treeIdTestcaseMap) throws URISyntaxException, IOException {

        List<TCRCatalogTreeTestcase> existingTestcases = new ArrayList<>();
        List<TCRCatalogTreeTestcase> updateTagTestcases = new ArrayList<>();
        Map<Long, List<Testcase>> toBeCreatedTestcases = new HashMap<>();

        for(Map.Entry<Long, List<Testcase>> entry : treeIdTestcaseMap.entrySet()) {
            Long treeId = entry.getKey();
            List<Testcase> testcaseList = entry.getValue();

            List<TCRCatalogTreeTestcase> tcrTestcases = testcaseService.getTestcasesForTreeId(treeId);

            if(tcrTestcases == null || tcrTestcases.isEmpty()) {
                //no testcases exist for this tree, add need to be created
                List<Testcase> addToList = toBeCreatedTestcases.get(treeId);
                if(addToList == null) {
                    addToList = new ArrayList<>();
                    addToList.addAll(testcaseList);
                    toBeCreatedTestcases.put(treeId, addToList);
                } else {
                    addToList.addAll(testcaseList);
                }
                continue;
            }

            testcaseLoop : for(Testcase testcase : testcaseList) {
                for (TCRCatalogTreeTestcase tcrCatalogTreeTestcase : tcrTestcases) {
                    if(tcrCatalogTreeTestcase.getTestcase().getName().equals(testcase.getName())) {
                        //this testcase already exists in this tree, no need to create
                        if((StringUtils.isNotBlank(testcase.getTag()))
                                && (StringUtils.isBlank(tcrCatalogTreeTestcase.getTestcase().getTag()) ||
                                !ListUtil.getSet(tcrCatalogTreeTestcase.getTestcase().getTag(), " ").equals(ListUtil.getSet(testcase.getTag(), " ")))) {
                            //parsed tag is not null and is not equal to existing tag so update the tag
                            tcrCatalogTreeTestcase.getTestcase().setTag(tcrCatalogTreeTestcase.getTestcase().getTag() + " " + testcase.getTag());
                            updateTagTestcases.add(tcrCatalogTreeTestcase);
                        } else {
                            //no tag change found so no need to update
                            existingTestcases.add(tcrCatalogTreeTestcase);
                        }
                        tcrTestcases.remove(tcrCatalogTreeTestcase);
                        continue testcaseLoop;
                    }
                }

                //on previous testcase, need to create
                List<Testcase> addToList = toBeCreatedTestcases.get(treeId);
                if(addToList == null) {
                    addToList = new ArrayList<>();
                    addToList.add(testcase);
                    toBeCreatedTestcases.put(treeId, addToList);
                } else {
                    addToList.add(testcase);
                }
            }
        }

        if(!updateTagTestcases.isEmpty()) {
            existingTestcases.addAll(testcaseService.updateTestcaseTags(updateTagTestcases));
        }

        if(!toBeCreatedTestcases.isEmpty()) {
            List<TCRCatalogTreeTestcase> tcrList = testcaseService.createTestcasesWithList(toBeCreatedTestcases);
            existingTestcases.addAll(tcrList);
        }

        return existingTestcases;
    }

    /**
     * Validates the build configuration details
     */
    private boolean validateBuildConfig() {
        boolean valid = true;
        if (StringUtils.isBlank(serverAddress)
                || StringUtils.isBlank(projectKey)
                || StringUtils.isBlank(releaseKey)
                || StringUtils.isBlank(cycleKey)
                || ADD_ZEPHYR_GLOBAL_CONFIG.equals(serverAddress.trim())
                || ADD_ZEPHYR_GLOBAL_CONFIG.equals(projectKey.trim())
                || ADD_ZEPHYR_GLOBAL_CONFIG.equals(releaseKey.trim())
                || ADD_ZEPHYR_GLOBAL_CONFIG.equals(cycleKey.trim())) {
            valid = false;
        }
        return valid;
    }

    public Set<String> getPackageNamesFromXML(List<Map> dataMapList) throws ParserConfigurationException, SAXException, IOException {
        Set<String> packageNames = new HashSet<>();
        for (Map dataMap : dataMapList) {
            packageNames.add(dataMap.get("packageName").toString());
        }
        return packageNames;
    }

    public List<Map> genericParserXML(String absoluteFilePath, String parserTemplate) throws ParserConfigurationException, SAXException, IOException {
        return ParserUtil.parseXmlLang(absoluteFilePath, parserTemplate);
    }

    public Map<TCRCatalogTreeTestcase, Map<String, Object>> createTestcasesFromMap(Map<String, TCRCatalogTreeDTO> packagePhaseMap, List<Map> dataMapList, ZephyrConfigModel zephyrConfigModel, PrintStream logger) throws URISyntaxException, IOException {
        Map<Long, List<Testcase>> treeIdTestcaseMap = new HashMap<>();
        List<Map<String, Map<String, Object>>> testcaseNameValueMapList = new ArrayList<>();

        long statusAttachmentCount = 0;

        dataMapLoop: for (Map dataMap : dataMapList) {

            Map testcaseMap = (Map) dataMap.get("testcase");

            String testcaseJson = new Gson().toJson(testcaseMap);
            Testcase testcase = new Gson().fromJson(testcaseJson, Testcase.class);
            if(testcase.getAutomated() == null) {
                testcase.setAutomated(true);
                testcase.setScriptName("Created By Jenkins");
            }

            if(testcase.getName().isEmpty()) {
                continue;
            }

            if(dataMap.containsKey("skipTestcaseNames")) {
                String[] skipTestcaseNames = dataMap.get("skipTestcaseNames").toString().split(",");
                for (String testcaseName : skipTestcaseNames) {
                    if(testcase.getName().equals(testcaseName)) {
                        //name matches, skip this testcase
                        continue dataMapLoop;
                    }
                }
            }

            String packageName = "parentPhase";
            if(isCreatePackage()) {
                packageName = dataMap.get("packageName").toString();
            }

            Map<String, String> statusCondition = new HashMap<>();
            if(dataMap.containsKey("statuses") && dataMap.get("statuses") != null) {
                List<Map> statuses = (List) dataMap.get("statuses");
                boolean matched = false;
                for(Map sc : statuses) {
                    if((sc.get("statusString") == null && sc.get("status") != null && StringUtils.isNotEmpty(sc.get("status").toString())) //status is not empty
                            || (sc.get("statusString") != null && sc.get("statusString").equals(sc.get("status")))) { //status matches statusString
                        statusCondition = sc;
                        matched = true;
                        break;
                    }
                }
                if(!matched) {
                    //none of the status condition satisfied, search for default
                    for(Map sc : statuses) {
                        if(sc.get("default") != null && Boolean.valueOf(sc.get("default").toString())) { //default status
                            statusCondition = sc;
                            break;
                        }
                    }
                }
            }

            TCRCatalogTreeDTO treeDTO = packagePhaseMap.get(packageName);

            if(treeIdTestcaseMap.containsKey(treeDTO.getId())) {
                treeIdTestcaseMap.get(treeDTO.getId()).add(testcase);
            } else {
                List<Testcase> testcaseList = new ArrayList<>();
                testcaseList.add(testcase);
                treeIdTestcaseMap.put(treeDTO.getId(), testcaseList);
            }

            Set<Long> requirementIds = new HashSet<>();
            Set<String> requirementAltIds = new HashSet<>();
            if(dataMap.containsKey("requirements")) {
                List<Map> requirements = (List) dataMap.get("requirements");
                for (Map requirement : requirements) {
                    String id = requirement.get("id").toString();
                    String[] splitRequirementId = id.split("_");
                    if(id.startsWith("ID_")) {
                        requirementIds.add(Long.parseLong(splitRequirementId[1]));
                    } else if(id.startsWith("AltID_")) {
                        requirementAltIds.add(splitRequirementId[1]);
                    }
                }
            }

            MapTestcaseToRequirement mapTestcaseToRequirement = new MapTestcaseToRequirement();
            mapTestcaseToRequirement.setRequirementId(requirementIds);
            mapTestcaseToRequirement.setRequirementAltId(requirementAltIds);
            mapTestcaseToRequirement.setReleaseId(zephyrConfigModel.getReleaseId());

            List<String> attachments = new ArrayList<>();

            if(dataMap.containsKey("attachments")) {
                List<Map> attachmentFilePaths = (List) dataMap.get("attachments");
                for(Map attachment : attachmentFilePaths) {
                    String filePath = attachment.get("filePath").toString();
                    attachments.add(filePath);
                }
            }

            if(dataMap.containsKey("basePath") && statusCondition.containsKey("attachmentFileIncludes")) {
                String basePath = dataMap.get("basePath").toString();
                String includesStr = statusCondition.get("attachmentFileIncludes");
                attachments.addAll(getAllIncludedFilePathList(basePath, includesStr));
            }

            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put("treeId", treeDTO.getId());
            valueMap.put("mapTestcaseToRequirement", mapTestcaseToRequirement);
            if(statusCondition.containsKey("statusId")) {
                valueMap.put("statusId", statusCondition.get("statusId"));
            }
            valueMap.put("attachments", attachments);

            if(statusCondition.containsKey("attachmentText")) {
                String successAttachmentStr = statusCondition.get("attachmentText");
                if(StringUtils.isNotBlank(successAttachmentStr)) {
                    GenericAttachmentDTO genericAttachmentDTO = new GenericAttachmentDTO();
                    genericAttachmentDTO.setFileName("status_" + ++statusAttachmentCount + "_" + System.currentTimeMillis() + ".txt");
                    genericAttachmentDTO.setContentType("text/plain");
                    genericAttachmentDTO.setFieldName(AttachmentService.ItemType.releaseTestSchedule.toString());
                    genericAttachmentDTO.setByteData(successAttachmentStr.getBytes());
                    valueMap.put("statusAttachment", genericAttachmentDTO);
                }
            }

            if(dataMap.containsKey("stepText")) {
                String stepStr = dataMap.get("stepText").toString();
                if(!StringUtils.isEmpty(stepStr)) {
                    List<Map<String, String>> stepList = new ArrayList<>();
                    TestStep testStep = stepMaker(stepStr, stepList);
                    valueMap.put("stepList", stepList);
                    testcase.setTestSteps(testStep);
                }
            }

            Map<String, Map<String, Object>> testcaseNameValueMap = new HashMap<>();
            testcaseNameValueMap.put(testcase.getName(), valueMap);
            testcaseNameValueMapList.add(testcaseNameValueMap);
        }
        List<TCRCatalogTreeTestcase> tcrList =  createTestcasesWithoutDuplicate(treeIdTestcaseMap);
        Map<TCRCatalogTreeTestcase, Map<String, Object>> tcrTestcaseStatusMap = new HashMap<>();
        List<MapTestcaseToRequirement> mapTestcaseToRequirements = new ArrayList<>();
        loop1 : for (Map<String, Map<String, Object>> map : testcaseNameValueMapList) {
            for(Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
                for(TCRCatalogTreeTestcase tcrCatalogTreeTestcase : tcrList) {
                    Long treeId = (Long) entry.getValue().get("treeId");
                    if (tcrCatalogTreeTestcase.getTestcase().getName().equals(entry.getKey()) && tcrCatalogTreeTestcase.getTcrCatalogTreeId().equals(treeId)) {
                        //same testcase, add id and status to map
                        Map<String, Object> statusMap = new HashMap<>();
                        if(entry.getValue().containsKey("statusId")) {
                            statusMap.put("statusId", entry.getValue().get("statusId"));
                        }

                        MapTestcaseToRequirement mapTestcaseToRequirement = (MapTestcaseToRequirement) entry.getValue().get("mapTestcaseToRequirement");
                        mapTestcaseToRequirement.setTestcaseId(tcrCatalogTreeTestcase.getTestcase().getId());
                        mapTestcaseToRequirements.add(mapTestcaseToRequirement);

                        if (entry.getValue().containsKey("attachments")) {
                            statusMap.put("attachments", entry.getValue().get("attachments"));
                        }

                        if (entry.getValue().containsKey("statusAttachment")) {
                            statusMap.put("statusAttachment", entry.getValue().get("statusAttachment"));
                        }

                        if (entry.getValue().containsKey("stepList")) {
                            statusMap.put("stepList", entry.getValue().get("stepList"));
                        }
                        tcrTestcaseStatusMap.put(tcrCatalogTreeTestcase, statusMap);
                        tcrList.remove(tcrCatalogTreeTestcase);
                        continue loop1;
                    }
                }
            }
        }
        List<String> msgs = requirementService.mapTestcaseToRequirements(mapTestcaseToRequirements);
        logger.println(msgs);
        return tcrTestcaseStatusMap;
    }

    TestStep stepMaker(String testSteps, List<Map<String, String>> stepsList) {
        String[] keyWords={"And","Or","Given","When","Then"};
        List<String> keywordsList= Arrays.asList(keyWords);
        String steps[] = testSteps.split("\\r?\\n");
        String status="";
        TestStep testStep = new TestStep();
        Integer i=1;
        for(String step : steps) {
            step = step.trim();
            if(keywordsList.contains(step.trim().split(" ", 2)[0])) {
                TestStepDetail testStepDetail = new TestStepDetail();
                Map<String, String> stepMap = new HashMap<String, String>();
                testStepDetail.setOrderId((long)i);
                int countMatches = StringUtils.countMatches(step, "..");
                if(countMatches == 0){
                    String stepData = step.substring(0,step.lastIndexOf(".")).trim();
                    testStepDetail.setStep(stepData);
                    stepMap.put("step", stepData);
                }else {
                    testStepDetail.setStep(step.substring(0, step.indexOf("..")));
                    stepMap.put("step", step.substring(0, step.indexOf("..")));
                }
                stepMap.put("orderId", i.toString());
                String[] statusStringArr = step.split("\\.");
                status = statusStringArr[statusStringArr.length-1].trim();

                if(StringUtils.isNumeric(status.substring(0,1))) {
                    //first character is numeric, check item before this in array for status
                    status = statusStringArr[statusStringArr.length-2].trim();
                }

                if(status.startsWith("failed")) {
                    stepMap.put("status", "false");
                }else if(status.startsWith("skipped") || status.startsWith("undefined")) {
                    stepMap.put("status", "skipped");
                }else {
                    stepMap.put("status", "true");
                }
                stepsList.add(stepMap);
                testStep.getSteps().add(testStepDetail);
                i++;
            }
        }
        testStep.setMaxId((long)testStep.getSteps().size());
        return testStep;
    }

    private List<String> getTestcasesForEggplant(List<EggPlantResult> eggPlantResults) throws ParseException, ParserConfigurationException, SAXException, IOException {
        String scriptNameParseTemplate = "[{\"scriptName\": \"${testsuite:name}\"}]";
        Map<String, EggPlantResult> eggPlantMap = new HashMap<>();//suite name, eggPlantResult
        for (EggPlantResult eggPlantResult : eggPlantResults) {
            List<Map> parseData = ParserUtil.parseXmlLang(eggPlantResult.getXmlResultFile(), scriptNameParseTemplate);
            EggPlantResult existingEPR = eggPlantMap.get(parseData.get(0).get("scriptName").toString());
            if (existingEPR == null || existingEPR.getRunDateInDate().before(eggPlantResult.getRunDateInDate())) {
                //either the file path for this eggplant script doesn't exist in map or it is older
                eggPlantMap.put(parseData.get(0).get("scriptName").toString(), eggPlantResult);
            }
        }
        List<String> xmlFilePaths = new ArrayList<>();
        eggPlantMap.forEach((key, value) -> xmlFilePaths.add(value.getXmlResultFile()));
        return xmlFilePaths;
    }

    private List<String> getAllIncludedFilePathList(String basePath, String includes) {
        FileSet fileSet = Util.createFileSet(new File(basePath), includes);
        String baseDir = fileSet.getDirectoryScanner().getBasedir().getAbsolutePath();
        List<String> filePathList = new ArrayList<>();
        for (String path : fileSet.getDirectoryScanner().getIncludedFiles()) {
            filePathList.add( baseDir + File.separator + path);
        }
        return filePathList;
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

    public StandardCredentials getStandardCredentials() {
        return standardCredentials;
    }

    public void setStandardCredentials(StandardCredentials standardCredentials) {
        this.standardCredentials = standardCredentials;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }
}
