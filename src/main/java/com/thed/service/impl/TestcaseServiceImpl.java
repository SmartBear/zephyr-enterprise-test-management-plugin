package com.thed.service.impl;

import com.google.common.collect.Lists;
import com.thed.model.*;
import com.thed.service.TestcaseService;
import com.thed.utils.ZephyrConstants;
import hudson.tasks.junit.CaseResult;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by tarun on 25/6/19.
 */
public class TestcaseServiceImpl extends BaseServiceImpl implements TestcaseService {

    public TestcaseServiceImpl() {
        super();
    }

    private Testcase parse(CaseResult caseResult) {
        Testcase testcase = new Testcase();
        testcase.setName(caseResult.getFullName());
        testcase.setAutomated(true);
        testcase.setScriptName("Created By Jenkins");
        return testcase;
    }

    @Override
    public List<TCRCatalogTreeTestcase> getTestcasesForTreeId(Long tcrCatalogTreeId) throws URISyntaxException, IOException {
        return zephyrRestService.getTestcasesForTreeId(tcrCatalogTreeId);
    }

    @Override
    public List<PlanningTestcase> getTestcasesForTreeIdFromPlanning(Long tcrCatalogTreeId, Integer offset, Integer pageSize) throws URISyntaxException, IOException {
        return zephyrRestService.getTestcasesForTreeIdFromPlanning(tcrCatalogTreeId, offset, pageSize);
    }

    @Override
    public List<TCRCatalogTreeTestcase> createTestcases(List<TCRCatalogTreeTestcase> tcrCatalogTreeTestcases) throws URISyntaxException, IOException {
        return zephyrRestService.createTestcases(tcrCatalogTreeTestcases);
    }

    @Override
    public Map<CaseResult, TCRCatalogTreeTestcase> createTestcases(Map<Long, List<CaseResult>> treeIdCaseResultMap) throws URISyntaxException, IOException {

        if(treeIdCaseResultMap == null || treeIdCaseResultMap.isEmpty()) {
            return new HashMap<>();
        }

        List<CaseResult> caseResultList = new ArrayList<>();
        List<TCRCatalogTreeTestcase> treeTestcases = new ArrayList<>();
        Set<Long> treeIds = treeIdCaseResultMap.keySet();

        for (Long treeId : treeIds) {

            List<CaseResult> caseResults = treeIdCaseResultMap.get(treeId);

            caseResultList.addAll(caseResults);

            for(CaseResult caseResult : caseResults) {
                TCRCatalogTreeTestcase tcrCatalogTreeTestcase = new TCRCatalogTreeTestcase();
                tcrCatalogTreeTestcase.setTcrCatalogTreeId(treeId);
                tcrCatalogTreeTestcase.setTestcase(parse(caseResult));

                treeTestcases.add(tcrCatalogTreeTestcase);
            }
        }

        treeTestcases = createTestcases(treeTestcases);

        Map<CaseResult, TCRCatalogTreeTestcase> map = new HashMap<>();

        loop1 : for (CaseResult caseResult : caseResultList) {
            for (TCRCatalogTreeTestcase tcrCatalogTreeTestcase : treeTestcases) {
                if(caseResult.getFullName().equals(tcrCatalogTreeTestcase.getTestcase().getName())) {
                    map.put(caseResult, tcrCatalogTreeTestcase);
                    continue loop1;
                }
            }
        }

        return map;
    }

    @Override
    public List<TCRCatalogTreeTestcase> createTestcasesWithList(Map<Long, List<Testcase>> treeIdTestcaseMap) throws URISyntaxException, IOException {
        List<TCRCatalogTreeTestcase> treeTestcases = new ArrayList<>();
        Set<Long> treeIds = treeIdTestcaseMap.keySet();

        for (Long treeId : treeIds) {
            List<Testcase> testcaseList = treeIdTestcaseMap.get(treeId);
            for (Testcase testcase : testcaseList) {
                TCRCatalogTreeTestcase tcrCatalogTreeTestcase = new TCRCatalogTreeTestcase();
                tcrCatalogTreeTestcase.setTcrCatalogTreeId(treeId);
                tcrCatalogTreeTestcase.setTestcase(testcase);

                treeTestcases.add(tcrCatalogTreeTestcase);
            }
        }
        // create testcase in Batch
        final List<TCRCatalogTreeTestcase> testcaseList = new ArrayList<>();
        for (int i = 0; i < treeTestcases.size(); i += ZephyrConstants.BATCH_SIZE) {
            final List<TCRCatalogTreeTestcase> tcrCatalogTreeTestcases = i + ZephyrConstants.BATCH_SIZE > treeTestcases.size() ? treeTestcases.subList(i, treeTestcases.size()) : treeTestcases.subList(i, i + ZephyrConstants.BATCH_SIZE);
            final List<TCRCatalogTreeTestcase> testcases = createTestcases(tcrCatalogTreeTestcases);
            testcaseList.addAll(testcases);
        }
        return testcaseList;
    }

    @Override
    public List<TCRCatalogTreeTestcase> updateTestcaseTags(List<TCRCatalogTreeTestcase> tcrCatalogTreeTestcaseList) throws IOException, URISyntaxException {
        List<TCRCatalogTreeTestcase> resultList = new ArrayList<>();
        Map<Set<String>, List<TctTestcaseVersionParam>> tagTestcaseMap = new HashMap<>();
        List<List<TCRCatalogTreeTestcase>> subLists = Lists.partition(tcrCatalogTreeTestcaseList, ZephyrConstants.BATCH_SIZE);

        for(List<TCRCatalogTreeTestcase> subList : subLists) {
            for (TCRCatalogTreeTestcase tcrCatalogTreeTestcase : subList) {
                Set<String> tagSet = new HashSet<>(Arrays.asList(tcrCatalogTreeTestcase.getTestcase().getTag().split(" ")));
                TctTestcaseVersionParam param = new TctTestcaseVersionParam(tcrCatalogTreeTestcase.getId(), tcrCatalogTreeTestcase.getTestcase().getId());
                if(tagTestcaseMap.containsKey(tagSet)) {
                    tagTestcaseMap.get(tagSet).add(param);
                } else {
                    tagTestcaseMap.put(tagSet, new ArrayList<>(Collections.singletonList(param)));
                }
            }
            resultList.addAll(updateTagsInTestcases(tagTestcaseMap));
        }
        return resultList;
    }

    private List<TCRCatalogTreeTestcase> updateTagsInTestcases(Map<Set<String>, List<TctTestcaseVersionParam>> tagTestcaseMap) throws IOException, URISyntaxException {
        List<TestcaseBulkUpdateParam> paramList = new ArrayList<>();
        for(Map.Entry<Set<String>, List<TctTestcaseVersionParam>> entry : tagTestcaseMap.entrySet()) {
            //extra space padding before tag data as the api is not handling this without space as expected and corrupts the tag data
            paramList.add(new TestcaseBulkUpdateParam(" " + String.join(" ", entry.getKey()), entry.getValue()));
        }
        return zephyrRestService.updateTestcases(paramList);
    }

}
