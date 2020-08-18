package com.thed.service.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.thed.model.*;
import com.thed.service.HttpClientService;
import com.thed.service.ZephyrRestService;
import com.thed.utils.GsonUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.*;

/**user
 * Created by prashant on 20/6/19.
 */
public class ZephyrRestServiceImpl implements ZephyrRestService {

    public static final String GET_CURRENT_USER_URL = "/flex/services/rest/{restVersion}/user/current";

    public static final String GET_PROJECT_BY_ID_URL = "/flex/services/rest/{restVersion}/project/{projectId}";
    public static final String GET_ALL_PROJECTS_FOR_CURRENT_USER_URL = "/flex/services/rest/{restVersion}/project/user/{userId}";
    public static final String GET_ALL_PROJECT_ID_FOR_CURRENT_USER_URL = "/flex/services/rest/{restVersion}/project/project/{userId}";

    public static final String GET_ALL_RELEASES_FOR_PROJECT_ID_URL = "/flex/services/rest/{restVersion}/release/paged/project/{projectId}"; //?order=id&isascorder=true&isVisible=false

    public static final String GET_TCR_CATALOG_TREE_NODES_URL = "/flex/services/rest/{restVersion}/testcasetree"; //?type=Phase&revisionid=0&releaseid=10
    public static final String GET_TCR_CATALOG_TREE_NODE_URL = "/flex/services/rest/{restVersion}/testcasetree/{tcrCatalogTreeId}";
    public static final String GET_TCR_CATALOG_TREE_HIERARCHY_URL = "/flex/services/rest/{restVersion}/testcasetree/hierarchy/{tcrCatalogTreeId}";
    public static final String CREATE_TCR_CATALOG_TREE_NODE_URL = "/flex/services/rest/{restVersion}/testcasetree"; //?parentid=0

    public static final String MAP_TESTCASE_TO_REQUIREMENTS_URL = "/flex/services/rest/v3/requirement/bulk";

    public static final String GET_TESTCASES_FOR_TREE_ID_URL = "/flex/services/rest/{restVersion}/testcase/tree/{tcrCatalogTreeId}"; //?offset=0&pagesize=50&dbsearch=true&isascorder=true&order=orderId&frozen=false&is_cfield=false
    public static final String GET_TESTCASES_FOR_TREE_ID_FROM_PLANNING_URL = "/flex/services/rest/{restVersion}/testcase/planning/{tcrCatalogTreeId}"; //?offset=0&pagesize=50&dbsearch=true&isascorder=true&order=orderId
    public static final String CREATE_TESTCASES_BULK_URL = "/flex/services/rest/{restVersion}/testcase/bulk";

    public static final String GET_CYCLE_BY_ID_URL = "/flex/services/rest/{restVersion}/cycle/{id}";
    public static final String CREATE_CYCLE_URL = "/flex/services/rest/{restVersion}/cycle";
    public static final String GET_ALL_CYCLES_FOR_RELEASE_ID_URL = "/flex/services/rest/{restVersion}/cycle/release/{releaseId}";

    public static final String CREATE_CYCLE_PHASE_URL = "/flex/services/rest/{restVersion}/cycle/{cycleId}/phase";
    public static final String ADD_TESTCASES_TO_FREE_FORM_CYCLE_PHASE_URL = "/flex/services/rest/{restVersion}/assignmenttree/{cyclePhaseId}/assign/bytree/{tcrCatalogTreeId}"; //?includehierarchy=false;
    public static final String ASSIGN_CYCLE_PHASE_TO_CREATOR_URL = "/flex/services/rest/{restVersion}/assignmenttree/{cyclePhaseId}/assign";
    public static final String EXECUTION_MODIFY_URL = "/flex/services/rest/{restVersion}/execution/modify";
    public static final String GET_RELEASE_TEST_SCHEDULES_URL = "/flex/services/rest/{restVersion}/execution"; //?cyclephaseid=11&pagesize=10000;
    public static final String EXECUTE_RELEASE_TEST_SCHEDULES_IN_BULK_URL = "/flex/services/rest/{restVersion}/execution/bulk";//?status=1&testerid=1&allExecutions=false&includeanyoneuser=true

    public static final String UPLOAD_ATTACHMENT_URL = "/flex/upload/document/genericattachment";
    public static final String ADD_ATTACHMENT_URL = "/flex/services/rest/{restVersion}/attachment/list";

    public static final String GET_TEST_STEP_URL = "/flex/services/rest/{restVersion}/testcase/{testcaseVersionId}/teststep";
    public static final String ADD_TEST_STEP_URL = "/flex/services/rest/{restVersion}/testcase/{testcaseVersionId}/teststep/{tctId}";
    public static final String ADD_TEST_STEP_RESULT_URL = "/flex/services/rest/{restVersion}/execution/teststepresult/saveorupdate";

    public static final String GET_ALL_PARSER_TEMPLATES_URL = "/flex/services/rest/{restVersion}/parsertemplate/";
    public static final String GET_PARSER_TEMPLATE_BY_ID_URL = "/flex/services/rest/{restVersion}/parsertemplate/{id}";

    public static final String GET_PREFERENCE_URL = "/flex/services/rest/{restVersion}/admin/preference"; //?key=testresult.testresultStatus.LOV

    private User currentUser;
    private String hostAddress;
    private String restVersion = "v3";
    private String password;
    private String pageSize;

    private HttpClientService httpClientService = new HttpClientServiceImpl();

    public ZephyrRestServiceImpl() {}

    private String buildUrl(String url, Map<String, String> pathParams, List<NameValuePair> queryParams) throws URISyntaxException {

        if(pathParams != null) {
            for (String key : pathParams.keySet()) {
                url = url.replaceAll("\\{"+key+"\\}", pathParams.get(key));
            }
        }

        if(queryParams != null) {
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.setParameters(queryParams);
            url = uriBuilder.build().toString();
        }

        return url;
    }

    private String prepareUrl(String url) throws URISyntaxException {
        url = getHostAddress() + url;

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("restVersion", restVersion);
        return buildUrl(url, pathParams, null);
    }

    @Override
    public Boolean verifyCredentials(String hostAddress, String username, String password) throws URISyntaxException, IOException {
        Boolean res = login(hostAddress, username, password);
        if(res == Boolean.TRUE) {
            clear();
        }
        return res;
    }

    /**
     * Verifies given credentials
     *
     * @param hostUrl
     * @param secretText
     * @return
     * @throws URISyntaxException
     */
    @Override
    public Boolean verifyCredentials(String hostUrl, String secretText) throws URISyntaxException, IOException {
        Boolean res = login(hostUrl, secretText);
        clear();
        return res;
    }

    @Override
    public Boolean login(String hostAddress, String username, String password) throws URISyntaxException, IOException {
        String url = hostAddress + GET_CURRENT_USER_URL;
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("restVersion", restVersion);
        url = buildUrl(url, pathParams, null);
        httpClientService.clear();
        String encoding = Base64.getEncoder().encodeToString((username+":"+password).getBytes());
        httpClientService.getHeaders().add(new BasicHeader("Authorization", "Basic "+encoding));
        String res = httpClientService.getRequest(url);
        httpClientService.getHeaders().clear();
        if(res != null) {
            setCurrentUser(GsonUtil.CUSTOM_GSON.fromJson(res, User.class));
            setHostAddress(hostAddress);
            this.password = password;
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Verifies given credentials and stores hostAddress if verification succeeds
     *
     * @param hostAddress
     * @param secretText
     * @return
     * @throws URISyntaxException
     */
    @Override
    public Boolean login(String hostAddress, String secretText) throws URISyntaxException, IOException {
        String url = hostAddress + GET_CURRENT_USER_URL;
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("restVersion", restVersion);
        url = buildUrl(url, pathParams, null);
        httpClientService.clear();
        httpClientService.getHeaders().add(new BasicHeader("Authorization", "Bearer "+ secretText));
        String res = httpClientService.getRequest(url);
        if(res != null) {
            setCurrentUser(GsonUtil.CUSTOM_GSON.fromJson(res, User.class));
            setHostAddress(hostAddress);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }


    @Override
    public Project getProjectById(Long projectId) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("projectId", String.valueOf(projectId));

        String url = prepareUrl(GET_PROJECT_BY_ID_URL);
        url = buildUrl(url, pathParams, null);
        String res = httpClientService.getRequest(url);
        return GsonUtil.CUSTOM_GSON.fromJson(res, Project.class);
    }

    @Override
    public Cycle createCycle(Cycle cycle) throws URISyntaxException, IOException {
        String url = prepareUrl(CREATE_CYCLE_URL);
        String res = httpClientService.postRequest(url, GsonUtil.CUSTOM_GSON.toJson(cycle));
        return GsonUtil.CUSTOM_GSON.fromJson(res, Cycle.class);
    }

    @Override
    public List<Project> getAllProjectsForCurrentUser() throws URISyntaxException, IOException {
        if(currentUser == null) {
            return null;
        }

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("userId", currentUser.getId().toString());
        String url = buildUrl(prepareUrl(GET_ALL_PROJECTS_FOR_CURRENT_USER_URL), pathParams, null);

        String res = httpClientService.getRequest(url);

        Type projectListType = new TypeToken<List<Project>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(res, projectListType);
    }

    @Override
    public List<Long> getAllProjectIdsForCurrentUser() throws URISyntaxException, IOException {
        if(currentUser == null) {
            return null;
        }

        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("userId", currentUser.getId().toString());
        String url = buildUrl(prepareUrl(GET_ALL_PROJECT_ID_FOR_CURRENT_USER_URL), pathParams, null);

        String res = httpClientService.getRequest(url);

        Type longListType = new TypeToken<List<Long>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(res, longListType);
    }



    @Override
    public List<Release> getAllReleasesForProjectId(Long projectId) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("projectId", projectId.toString());

        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("order", "id"));
        queryParams.add(new BasicNameValuePair("isascorder", "true"));
        queryParams.add(new BasicNameValuePair("isVisible", "false"));
        queryParams.add(new BasicNameValuePair("pagesize", "0"));

        String url = buildUrl(prepareUrl(GET_ALL_RELEASES_FOR_PROJECT_ID_URL), pathParams, queryParams);

        String res = httpClientService.getRequest(url);
        JSONObject resObject = new JSONObject(res);
        JSONArray jsonArray = resObject.getJSONArray("results");
        String resultStr = jsonArray.toString();

        Type releaseListType = new TypeToken<List<Release>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(resultStr, releaseListType);
    }

    @Override
    public List<Cycle> getAllCyclesForReleaseId(Long releaseId) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("releaseId", releaseId.toString());

        String url = buildUrl(prepareUrl(GET_ALL_CYCLES_FOR_RELEASE_ID_URL), pathParams, null);
        String res = httpClientService.getRequest(url);

        Type releaseListType = new TypeToken<List<Cycle>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(res, releaseListType);
    }

    @Override
    public List<TCRCatalogTreeDTO> getTCRCatalogTreeNodes(String type, Long revisionId, Long releaseId) throws URISyntaxException, IOException {
        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("type", type));
        queryParams.add(new BasicNameValuePair("revisionid", revisionId.toString()));
        queryParams.add(new BasicNameValuePair("releaseid", releaseId.toString()));

        String url = buildUrl(prepareUrl(GET_TCR_CATALOG_TREE_NODES_URL), null, queryParams);
        String res = httpClientService.getRequest(url);

        Type tcrCatalogTreeListType = new TypeToken<List<TCRCatalogTreeDTO>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(res, tcrCatalogTreeListType);
    }

    @Override
    public TCRCatalogTreeDTO getTCRCatalogTreeNode(Long tcrCatalogTreeId) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("tcrCatalogTreeId", tcrCatalogTreeId.toString());

        String url = buildUrl(prepareUrl(GET_TCR_CATALOG_TREE_NODE_URL), pathParams, null);
        String res = httpClientService.getRequest(url);

        return GsonUtil.CUSTOM_GSON.fromJson(res, TCRCatalogTreeDTO.class);
    }

    @Override
    public List<Long> getTCRCatalogTreeIdHierarchy(Long tcrCatalogTreeId) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("tcrCatalogTreeId", tcrCatalogTreeId.toString());

        String url = buildUrl(prepareUrl(GET_TCR_CATALOG_TREE_HIERARCHY_URL), pathParams, null);
        String res = httpClientService.getRequest(url);
        Type longListType = new TypeToken<List<Long>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(res, longListType);
    }

    @Override
    public TCRCatalogTreeDTO createTCRCatalogTreeNode(TCRCatalogTreeDTO tcrCatalogTreeDTO) throws URISyntaxException, IOException {
        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("parentid", tcrCatalogTreeDTO.getParentId().toString()));
        tcrCatalogTreeDTO.setParentId(null);

        String url = buildUrl(prepareUrl(CREATE_TCR_CATALOG_TREE_NODE_URL), null, queryParams);
        String res = httpClientService.postRequest(url, GsonUtil.CUSTOM_GSON.toJson(tcrCatalogTreeDTO));
        return GsonUtil.CUSTOM_GSON.fromJson(res, TCRCatalogTreeDTO.class);
    }

    @Override
    public List<String> mapTestcaseToRequirements(List<MapTestcaseToRequirement> mapTestcaseToRequirements) throws URISyntaxException, IOException {
        String url = buildUrl(prepareUrl(MAP_TESTCASE_TO_REQUIREMENTS_URL), null, null);
        String res = httpClientService.postRequest(url, GsonUtil.CUSTOM_GSON.toJson(mapTestcaseToRequirements));
        Type stringListType = new TypeToken<List<String>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(res, stringListType);
    }

    @Override
    public List<TCRCatalogTreeTestcase> getTestcasesForTreeId(Long tcrCatalogTreeId) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("tcrCatalogTreeId", tcrCatalogTreeId.toString());

        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("offset", "0"));
        queryParams.add(new BasicNameValuePair("pagesize", "10000"));
        queryParams.add(new BasicNameValuePair("dbsearch", "true"));
        queryParams.add(new BasicNameValuePair("isascorder", "true"));
        queryParams.add(new BasicNameValuePair("order", "orderId"));
        queryParams.add(new BasicNameValuePair("frozen", "false"));
        queryParams.add(new BasicNameValuePair("is_cfield", "false"));

        String url = buildUrl(prepareUrl(GET_TESTCASES_FOR_TREE_ID_URL), pathParams, queryParams);
        String res = httpClientService.getRequest(url);
        JSONObject resObject = new JSONObject(res);
        JSONArray jsonArray = resObject.getJSONArray("results");
        String resultStr = jsonArray.toString();

        Type tcrCatalogTreeTestcaseListType = new TypeToken<List<TCRCatalogTreeTestcase>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(resultStr, tcrCatalogTreeTestcaseListType);
    }

    @Override
    public List<PlanningTestcase> getTestcasesForTreeIdFromPlanning(Long tcrCatalogTreeId, Integer offset, Integer pageSize) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("tcrCatalogTreeId", tcrCatalogTreeId.toString());

        List<NameValuePair> queryParams = new ArrayList<>();
        if(offset != null) {
            queryParams.add(new BasicNameValuePair("offset", offset.toString()));
        }

        if(pageSize != null) {
            queryParams.add(new BasicNameValuePair("pagesize", pageSize.toString()));
        }

        String url = buildUrl(prepareUrl(GET_TESTCASES_FOR_TREE_ID_FROM_PLANNING_URL), pathParams, queryParams);
        String res = httpClientService.getRequest(url);
        JSONObject resObject = new JSONObject(res);
        JSONArray jsonArray = resObject.getJSONArray("results");
        String resultStr = jsonArray.toString();

        Type planningTestcaseListType = new TypeToken<List<PlanningTestcase>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(resultStr, planningTestcaseListType);
    }

    @Override
    public List<TCRCatalogTreeTestcase> createTestcases(List<TCRCatalogTreeTestcase> tcrCatalogTreeTestcases) throws URISyntaxException, IOException {
        String url = prepareUrl(CREATE_TESTCASES_BULK_URL);
        String res = httpClientService.postRequest(url, GsonUtil.CUSTOM_GSON.toJson(tcrCatalogTreeTestcases));

        Type tcrCatalogTreeTestcaseListType = new TypeToken<List<TCRCatalogTreeTestcase>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(res, tcrCatalogTreeTestcaseListType);
    }

    @Override
    public Cycle getCycleById(Long cycleId) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", cycleId.toString());

        String url = buildUrl(prepareUrl(GET_CYCLE_BY_ID_URL), pathParams, null);
        String res = httpClientService.getRequest(url);
        return GsonUtil.CUSTOM_GSON.fromJson(res, Cycle.class);
    }

    @Override
    public CyclePhase createCyclePhase(CyclePhase cyclePhase) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("cycleId", cyclePhase.getCycleId().toString());

        String url = buildUrl(prepareUrl(CREATE_CYCLE_PHASE_URL), pathParams, null);
        String res = httpClientService.postRequest(url, GsonUtil.CUSTOM_GSON.toJson(cyclePhase));
        return GsonUtil.CUSTOM_GSON.fromJson(res, CyclePhase.class);
    }

    @Override
    public String addTestcasesToFreeFormCyclePhase(CyclePhase cyclePhase, Map<Long, Set<Long>> treeTestcaseMap, Boolean includeHierarchy) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("cyclePhaseId", cyclePhase.getId().toString());
        pathParams.put("tcrCatalogTreeId", cyclePhase.getTcrCatalogTreeId().toString());
        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("includehierarchy", includeHierarchy.toString()));

        JSONArray contentJsonArray = new JSONArray();

        for (Map.Entry<Long, Set<Long>> entry : treeTestcaseMap.entrySet()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("treeid", entry.getKey());
            jsonObject.put("tctIds", entry.getValue());
            jsonObject.put("isExclusion", Boolean.TRUE);

            contentJsonArray.put(jsonObject);
        }

        String url = buildUrl(prepareUrl(ADD_TESTCASES_TO_FREE_FORM_CYCLE_PHASE_URL), pathParams, queryParams);
        return httpClientService.postRequest(url, contentJsonArray.toString());
    }

    @Override
    public Integer assignCyclePhaseToCreator(Long cyclePhaseId) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("cyclePhaseId", cyclePhaseId.toString());

        String url = buildUrl(prepareUrl(ASSIGN_CYCLE_PHASE_TO_CREATOR_URL), pathParams, null);
        String res = httpClientService.postRequest(url, "");
        return Integer.parseInt(res);
    }

    @Override
    public List<ReleaseTestSchedule> assignTCRCatalogTreeTestcasesToUser(Long cyclePhaseId, Long tcrCatalogTreeId, List<Long> tctIdList, Long userId) throws URISyntaxException, IOException {
        JSONArray createRTSList = new JSONArray();
        for(Long tctId : tctIdList) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cyclePhaseId", cyclePhaseId);
            jsonObject.put("tctId", tctId);
            jsonObject.put("testerId", userId);
            createRTSList.put(jsonObject);
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("selectedAll", 1);
        jsonObject.put("testerId", userId);
        jsonObject.put("cyclePhaseId", cyclePhaseId);
        jsonObject.put("createRTSList", createRTSList);
        jsonObject.put("tcrCatalogTreeId", tcrCatalogTreeId);

        String url = buildUrl(prepareUrl(EXECUTION_MODIFY_URL),null, null);
        String res = httpClientService.postRequest(url, jsonObject.toString());
        Type releaseTestScheduleListType = new TypeToken<List<ReleaseTestSchedule>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(res, releaseTestScheduleListType);
    }

    @Override
    public List<ReleaseTestSchedule> getReleaseTestSchedules(Long cyclePhaseId, Integer offset, Integer pageSize) throws URISyntaxException, IOException {
        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("cyclephaseid", cyclePhaseId.toString()));
        if(offset != null) {
            queryParams.add(new BasicNameValuePair("offset", offset.toString()));
        }
        if(pageSize != null) {
            queryParams.add(new BasicNameValuePair("pagesize", pageSize.toString()));
        }

        String url = buildUrl(prepareUrl(GET_RELEASE_TEST_SCHEDULES_URL), null, queryParams);
        String res = httpClientService.getRequest(url);

        JSONObject resObject = new JSONObject(res);
        JSONArray jsonArray = resObject.getJSONArray("results");
        String resultStr = jsonArray.toString();

        Type releaseTestScheduleListType = new TypeToken<List<ReleaseTestSchedule>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(resultStr, releaseTestScheduleListType);
    }

    @Override
    public List<ReleaseTestSchedule> executeReleaseTestSchedules(Set<Long> rtsIds, String executionStatus) throws URISyntaxException, IOException {
        List<NameValuePair> queryParams = new ArrayList<>();
        queryParams.add(new BasicNameValuePair("status", executionStatus));
        queryParams.add(new BasicNameValuePair("testerid", getCurrentUser().getId().toString()));
        queryParams.add(new BasicNameValuePair("allExecutions", "false"));
        queryParams.add(new BasicNameValuePair("includeanyoneuser", "true"));

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ids", rtsIds);
        jsonObject.put("serachView", false);

        String url = buildUrl(prepareUrl(EXECUTE_RELEASE_TEST_SCHEDULES_IN_BULK_URL), null, queryParams);
        String res = httpClientService.putRequest(url, jsonObject.toString());

        Type releaseTestScheduleListType = new TypeToken<List<ReleaseTestSchedule>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(res, releaseTestScheduleListType);
    }

    @Override
    public List<GenericAttachmentDTO> uploadAttachments(List<GenericAttachmentDTO> attachmentDTOs) throws URISyntaxException, IOException {
        String url = buildUrl(prepareUrl(UPLOAD_ATTACHMENT_URL), null, null);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (GenericAttachmentDTO attachmentDTO : attachmentDTOs) {
            builder.addBinaryBody(attachmentDTO.getFieldName(), attachmentDTO.getByteData(), ContentType.MULTIPART_FORM_DATA, attachmentDTO.getFileName());
        }

        String res = httpClientService.postRequest(url, builder.build());
        Type type = new TypeToken<List<GenericAttachmentDTO>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(res, type);
    }

    @Override
    public List<Attachment> addAttachment(List<Attachment> attachments) throws URISyntaxException, IOException {
        String url = buildUrl(prepareUrl(ADD_ATTACHMENT_URL), null, null);

        String res = httpClientService.postRequest(url, GsonUtil.CUSTOM_GSON.toJson(attachments));
        Type type = new TypeToken<List<Attachment>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(res, type);
    }

    @Override
    public TestStep getTestStep(Long testcaseVersionId) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("testcaseVersionId", testcaseVersionId.toString());
        String url = buildUrl(prepareUrl(GET_TEST_STEP_URL), pathParams, null);
        String res = httpClientService.getRequest(url);
        return GsonUtil.CUSTOM_GSON.fromJson(res, TestStep.class);
    }

    @Override
    public TestStep addTestStep(TestStep testStep) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("testcaseVersionId", testStep.getTcId().toString());
        pathParams.put("tctId", testStep.getTctId().toString());
        String url = buildUrl(prepareUrl(ADD_TEST_STEP_URL), pathParams, null);
        String res = httpClientService.postRequest(url, GsonUtil.CUSTOM_GSON.toJson(testStep));
        return GsonUtil.CUSTOM_GSON.fromJson(res, TestStep.class);
    }

    @Override
    public List<TestStepResult> addTestStepsResults(List<TestStepResult> testStepResults) throws URISyntaxException, IOException {
        String url = buildUrl(prepareUrl(ADD_TEST_STEP_RESULT_URL), null, null);
        String res = httpClientService.postRequest(url, GsonUtil.CUSTOM_GSON.toJson(testStepResults));
        Type type = new TypeToken<List<TestStepResult>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(res, type);
    }

    @Override
    public List<ParserTemplate> getAllParserTemplates() throws URISyntaxException, IOException {
        String url = buildUrl(prepareUrl(GET_ALL_PARSER_TEMPLATES_URL), null, null);
        String res = httpClientService.getRequest(url);

        Type parserTemplateListType = new TypeToken<List<ParserTemplate>>(){}.getType();
        return GsonUtil.CUSTOM_GSON.fromJson(res, parserTemplateListType);
    }

    @Override
    public ParserTemplate getParserTemplateById(Long templateId) throws URISyntaxException, IOException {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", templateId.toString());

        String url = buildUrl(prepareUrl(GET_PARSER_TEMPLATE_BY_ID_URL), pathParams, null);
        String res = httpClientService.getRequest(url);
        return GsonUtil.CUSTOM_GSON.fromJson(res, ParserTemplate.class);
    }

    @Override
    public PreferenceDTO getPreference(String key) throws URISyntaxException, IOException {
        List<NameValuePair> queryParams = Collections.singletonList(new BasicNameValuePair("key", key));

        String url = buildUrl(prepareUrl(GET_PREFERENCE_URL), null, queryParams);
        String res = httpClientService.getRequest(url);
        return GsonUtil.CUSTOM_GSON.fromJson(res, PreferenceDTO.class);
    }

    @Override
    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public String getRestVersion() {
        return restVersion;
    }

    public void setRestVersion(String restVersion) {
        this.restVersion = restVersion;
    }

    @Override
    public void closeHttpConnection() throws IOException {
        httpClientService.closeHttpClient();
    }

    @Override
    public void clear() {
        setCurrentUser(null);
        setHostAddress("");
        httpClientService.clear();
    }

}
