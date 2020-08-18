package com.thed.model;

import java.util.List;

public class TestcaseBulkUpdateParam {

    private String tag;

    private List<TctTestcaseVersionParam> tctTestcaseVersionParam;

    public TestcaseBulkUpdateParam() {}

    public TestcaseBulkUpdateParam(String tag, List<TctTestcaseVersionParam> tctTestcaseVersionParam) {
        setTag(tag);
        setTctTestcaseVersionParam(tctTestcaseVersionParam);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<TctTestcaseVersionParam> getTctTestcaseVersionParam() {
        return tctTestcaseVersionParam;
    }

    public void setTctTestcaseVersionParam(List<TctTestcaseVersionParam> tctTestcaseVersionParam) {
        this.tctTestcaseVersionParam = tctTestcaseVersionParam;
    }
}