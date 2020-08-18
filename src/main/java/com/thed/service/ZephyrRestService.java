package com.thed.service;

import com.thed.model.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by prashant on 20/6/19.
 */
public interface ZephyrRestService {

    /**
     * Verifies given credentials
     * @param hostUrl
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    public Boolean verifyCredentials(String hostUrl, String username, String password) throws URISyntaxException, IOException;

    /**
     * Verifies given credentials
     * @param hostUrl
     * @param secretText
     * @return
     * @throws URISyntaxException
     */
    public Boolean verifyCredentials(String hostUrl, String secretText) throws URISyntaxException, IOException;

    /**
     * Verifies given credentials and stores hostAddress if verification succeeds
     * @param hostAddress
     * @param username
     * @param password
     * @return
     * @throws URISyntaxException
     */
    Boolean login(String hostAddress, String username, String password) throws URISyntaxException, IOException;

    /**
     * Verifies given credentials and stores hostAddress if verification succeeds
     * @param hostAddress
     * @param secretText
     * @return
     * @throws URISyntaxException
     */
    Boolean login(String hostAddress, String secretText) throws URISyntaxException, IOException;

    /**
     * Get project using project id.
     * @param projectId
     * @return
     * @throws URISyntaxException
     */
    Project getProjectById(Long projectId) throws URISyntaxException, IOException;

    /**
     * Create a new cycle with given cycle.
     * @param cycle
     * @return
     */
    Cycle createCycle(Cycle cycle) throws URISyntaxException, IOException;

    /**
     * Get all projects for current user.
     * @return
     */
    public List<Project> getAllProjectsForCurrentUser() throws URISyntaxException, IOException;

    List<Long> getAllProjectIdsForCurrentUser() throws URISyntaxException, IOException;

    /**
     * Get all releases for given projectId.
     * @param projectId
     * @return
     * @throws URISyntaxException
     */
    List<Release> getAllReleasesForProjectId(Long projectId) throws URISyntaxException, IOException;

    /**
     * Get all cycles for given releaseId.
     * @param releaseId
     * @return
     * @throws URISyntaxException
     */
    List<Cycle> getAllCyclesForReleaseId(Long releaseId) throws URISyntaxException, IOException;

    /**
     * Get all tree nodes such as phases for given releaseId.
     * @param type
     * @param revisionId
     * @param releaseId
     * @return
     * @throws URISyntaxException
     */
    List<TCRCatalogTreeDTO> getTCRCatalogTreeNodes(String type, Long revisionId, Long releaseId) throws URISyntaxException, IOException;

    /**
     * Get tcrCatalogTree node for given id.
     * @param tcrCatalogTreeId
     * @return
     * @throws URISyntaxException
     */
    TCRCatalogTreeDTO getTCRCatalogTreeNode(Long tcrCatalogTreeId) throws URISyntaxException, IOException;

    /**
     * Get treeIds of all the nodes in hierarchy in given treeId.
     * @param tcrCatalogTreeId
     * @return
     * @throws URISyntaxException
     */
    List<Long> getTCRCatalogTreeIdHierarchy(Long tcrCatalogTreeId) throws URISyntaxException, IOException;

    /**
     * Create a tree node such as phase.
     * @param tcrCatalogTreeDTO
     * @return
     * @throws URISyntaxException
     */
    TCRCatalogTreeDTO createTCRCatalogTreeNode(TCRCatalogTreeDTO tcrCatalogTreeDTO) throws URISyntaxException, IOException;

    /**
     * Create mappings between testcase and requirements given.
     * @param mapTestcaseToRequirements
     * @return
     * @throws URISyntaxException
     */
    List<String> mapTestcaseToRequirements(List<MapTestcaseToRequirement> mapTestcaseToRequirements) throws URISyntaxException, IOException;

    /**
     * Get testcases for given tree id.
     * @param tcrCatalogTreeId
     * @return
     * @throws URISyntaxException
     */
    List<TCRCatalogTreeTestcase> getTestcasesForTreeId(Long tcrCatalogTreeId) throws URISyntaxException, IOException;

    /**
     * Get list of testcases from planning using treeId.
     * @param tcrCatalogTreeId
     * @param offset
     * @param pageSize
     * @return
     * @throws URISyntaxException
     */
    List<PlanningTestcase> getTestcasesForTreeIdFromPlanning(Long tcrCatalogTreeId, Integer offset, Integer pageSize) throws URISyntaxException, IOException;

    /**
     * Create testcases in bulk.
     * @param tcrCatalogTreeTestcases
     * @return
     * @throws URISyntaxException
     */
    List<TCRCatalogTreeTestcase> createTestcases(List<TCRCatalogTreeTestcase> tcrCatalogTreeTestcases) throws URISyntaxException, IOException;

    /**
     * Update testcases in bulk.
     * @param testcaseBulkUpdateParamList
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    List<TCRCatalogTreeTestcase> updateTestcases(List<TestcaseBulkUpdateParam> testcaseBulkUpdateParamList) throws URISyntaxException, IOException;

    /**
     * Get cycle for id.
     * @param cycleId
     * @return
     * @throws URISyntaxException
     */
    Cycle getCycleById(Long cycleId) throws URISyntaxException, IOException;

    /**
     * Create cycle phase.
     * @param cyclePhase
     * @return
     * @throws URISyntaxException
     */
    CyclePhase createCyclePhase(CyclePhase cyclePhase) throws URISyntaxException, IOException;

    /**
     * Adds testcases to free form cycle phase.
     * @param cyclePhase
     * @param treeTestcaseMap
     * @param includeHierarchy
     * @return
     * @throws URISyntaxException
     */
    String addTestcasesToFreeFormCyclePhase(CyclePhase cyclePhase, Map<Long, Set<Long>> treeTestcaseMap, Boolean includeHierarchy) throws URISyntaxException, IOException;

    /**
     * Assigns cycle phase to creator.
     * @param cyclePhaseId
     * @return
     * @throws URISyntaxException
     */
    Integer assignCyclePhaseToCreator(Long cyclePhaseId) throws URISyntaxException, IOException;

    /**
     * Assign testcases to user.
     * @param cyclePhaseId
     * @param tcrCatalogTreeId
     * @param tctIdList
     * @param userId
     * @return
     * @throws URISyntaxException
     */
    List<ReleaseTestSchedule> assignTCRCatalogTreeTestcasesToUser(Long cyclePhaseId, Long tcrCatalogTreeId, List<Long> tctIdList, Long userId) throws URISyntaxException, IOException;

    /**
     * Get testcases scheduled under this cycle phase.
     * @param cyclePhaseId
     * @param offset Offset for starting results
     * @param pageSize Number of results per page, 0 will return all
     * @return
     * @throws URISyntaxException
     */
    List<ReleaseTestSchedule> getReleaseTestSchedules(Long cyclePhaseId, Integer offset, Integer pageSize) throws URISyntaxException, IOException;

    /**
     * Execute given releaseTestSchedule ids with given status.
     * @param rtsIds
     * @param executionStatus
     * @return
     * @throws URISyntaxException
     */
    List<ReleaseTestSchedule> executeReleaseTestSchedules(Set<Long> rtsIds, String executionStatus) throws URISyntaxException, IOException;

    /**
     * Upload all the attachments from list given.
     * @param attachmentDTOs
     * @return
     * @throws URISyntaxException
     */
    List<GenericAttachmentDTO> uploadAttachments(List<GenericAttachmentDTO> attachmentDTOs) throws URISyntaxException, IOException;

    /**
     * Add attachments to items(ex: testcase, requirement, etc.) with given details.
     * @param attachments
     * @return
     * @throws URISyntaxException
     */
    List<Attachment> addAttachment(List<Attachment> attachments) throws URISyntaxException, IOException;

    /**
     * Get testStep for for given testcaseVersionId.
     * @param testcaseVersionId
     * @return
     * @throws URISyntaxException
     */
    TestStep getTestStep(Long testcaseVersionId) throws URISyntaxException, IOException;

    /**
     * Add testStep to single testcase.
     * @param testStep
     * @return
     * @throws URISyntaxException
     */
    TestStep addTestStep(TestStep testStep) throws URISyntaxException, IOException;

    /**
     * Add testStep results.
     * @param testStepResults
     * @return
     * @throws URISyntaxException
     */
    List<TestStepResult> addTestStepsResults(List<TestStepResult> testStepResults) throws URISyntaxException, IOException;

    User getCurrentUser();

    void closeHttpConnection() throws IOException;

    /**
     * Clears all data saved in this instance and related to this.
     */
    void clear();

    /**
     * Get all parser-templates.
     * @return
     */
    List<ParserTemplate> getAllParserTemplates() throws URISyntaxException, IOException;

    /**
     * Get parser-template by templateId.
     * @param templateId
     * @return
     */
    ParserTemplate getParserTemplateById(Long templateId) throws URISyntaxException, IOException;

    /**
     * Get preference for given key.
     * @param key
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    PreferenceDTO getPreference(String key) throws URISyntaxException, IOException;
}
