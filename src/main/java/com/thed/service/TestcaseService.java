package com.thed.service;

import com.thed.model.PlanningTestcase;
import com.thed.model.TCRCatalogTreeTestcase;
import com.thed.model.TestStep;
import com.thed.model.Testcase;
import hudson.tasks.junit.CaseResult;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * Created by tarun on 25/6/19.
 */
public interface TestcaseService extends BaseService {

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
     * Create testcases from CaseResults and return testcases with their execution status
     * @param treeIdCaseResultMap
     * @return
     * @throws URISyntaxException
     */
    Map<CaseResult, TCRCatalogTreeTestcase> createTestcases(Map<Long, List<CaseResult>> treeIdCaseResultMap) throws URISyntaxException, IOException;

    List<TCRCatalogTreeTestcase> createTestcasesWithList(Map<Long, List<Testcase>> treeIdTestcaseMap) throws URISyntaxException, IOException;

    /**
     * Update tags of the testcases provided to their respective values.
     * @param tcrCatalogTreeTestcaseList
     * @return
     */
    List<TCRCatalogTreeTestcase> updateTestcaseTags(List<TCRCatalogTreeTestcase> tcrCatalogTreeTestcaseList) throws IOException, URISyntaxException;

}
