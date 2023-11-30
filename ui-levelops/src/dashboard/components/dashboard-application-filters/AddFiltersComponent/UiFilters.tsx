import { Checkbox, Form, Icon } from "antd";
import {
  AntIcon,
  AntInput,
  AntSelect,
  AntText,
  AntTooltip,
  CustomFormItemLabel,
  EditableTag,
  InputRangeFilter
} from "../../../../shared-resources/components";
import React from "react";
import {
  getFilterValue,
  getGroupByRootFolderKey,
  isExcludeVal
} from "../../../../configurable-dashboard/helpers/helper";
import widgetConstants from "../../../constants/widgetConstants";
import IssueTimeFilterComponent from "../../../graph-filters/components/IssueTimeFilter";
import {
  filesFilters,
  GlobalFilesFilters,
  jenkinsConfigAggsType,
  jenkinsConfigTimePeriodOptions,
  jenkinsJobsAggsType,
  scmFilesSortOptions
} from "./filterConstants";
import { JenkinsGithubParameters, JenkinsJobTimeFilter, ModulePathFilter } from "../../../graph-filters/components";
import BullseyeMetricFilter from "../../../graph-filters/components/bullseye-filters/BullseyeMetricFilter";
import { get } from "lodash";
import { CheckboxChangeEvent } from "antd/lib/checkbox";
import {
  hygieneTypes,
  zendeskSalesForceHygieneTypes,
  scmIssueHygieneTypes
} from "../../../constants/hygiene.constants";
import { CreatedAtUpdateAtOptions } from "../../../graph-filters/components/Constants";
import { stringSortingComparator } from "../../../graph-filters/components/sort.helper";
import AzureTeamsFilter from "../../../graph-filters/components/AzureTeamsFilter";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const uiFilterKeys = [
  "epics",
  "hygiene_types",
  "story_points",
  "parent_story_points",
  "issue_created_at",
  "issue_updated_at",
  "created_at",
  "salesforce_created_at",
  "salesforce_updated_at",
  "time_period",
  "agg_type",
  "end_time",
  "pr_created_at",
  "pr_closed_at",
  "sort",
  "committed_at",
  "ingested_at",
  "n_last_reports",
  "metric",
  "score_range",
  "disclosure_range",
  "publication_range",
  "parameters",
  "group_by_modules",
  "module_paths",
  "code_area",
  "teams"
];

export const UiFilters = (
  filters: any,
  filterKey: string,
  report: any,
  onFilterValueChange: any,
  removeFilterOptionSelected: (key: any) => void,
  onExcludeFilterChange: any,
  handleTimeRangeFilterValueChange: any,
  metaData: any,
  handleTimeRangeTypeChange: any,
  integrationIds: any
) => {
  const getHygieneOptions = () => {
    const supportedReports = [
      "github_issues_report",
      "github_issues_report_trends",
      "github_issues_count_single_stat",
      "github_issues_first_reponse_report",
      "github_issues_first_response_report_trends",
      "github_issues_first_response_count_single_stat"
    ];
    const types = ["zendesk", "salesforce"].includes(report.application)
      ? zendeskSalesForceHygieneTypes
      : supportedReports.includes(report.report)
      ? scmIssueHygieneTypes
      : hygieneTypes;

    const hygieneOptions = types.map((item: any) => ({
      label: item.replace(/_/g, " "),
      value: item
    }));
    hygieneOptions.sort(stringSortingComparator("label"));
    return hygieneOptions;
  };

  const getSortFilterValue = () => {
    return filters["sort"] ? filters["sort"][0].id : "";
  };

  const getMapSortValue = (value: any) => {
    return [{ id: value, order: "-1" }];
  };

  switch (filterKey) {
    case "epics":
      return (
        <div className="ui-filter epics">
          <Form.Item className="w-100 epic-form form-label" key={`epics_${report.uri}`} label={"EPICS"}>
            <EditableTag
              tagLabel={"Add Epic"}
              tags={filters["epics"]}
              onTagsChange={(value: string[]) => onFilterValueChange(value, "epic")}
            />
          </Form.Item>
          <AntIcon
            className="filter-delete-icon--epic-icon"
            type={"delete"}
            onClick={e => {
              e.stopPropagation();
              removeFilterOptionSelected("epic");
            }}
          />
        </div>
      );

    case "hygiene_types":
      return (
        <Form.Item
          key={`hygiene_types_${report.uri}`}
          className={"custom-form-item"}
          label={
            <div className="flex w-100">
              <CustomFormItemLabel
                label={"HYGIENE"}
                withSwitch={{
                  showSwitchText: report.application === IntegrationTypes.JIRA,
                  showSwitch: report.application === IntegrationTypes.JIRA,
                  switchValue: isExcludeVal(filters, "hygiene_types"),
                  onSwitchValueChange: value => onExcludeFilterChange("hygiene_types", value)
                }}
              />
              <AntIcon
                className="exclude-delete"
                type={"delete"}
                onClick={e => {
                  e.stopPropagation();
                  e.preventDefault();
                  removeFilterOptionSelected("hygiene_types");
                }}
              />
            </div>
          }>
          <AntSelect
            showArrow={true}
            value={getFilterValue(filters, "hygiene_types")}
            mode="multiple"
            options={getHygieneOptions()}
            onChange={(value: any, options: any) =>
              onFilterValueChange(value, "hygiene_types", isExcludeVal(filters, "hygiene_types"))
            }
          />
        </Form.Item>
      );

    case "story_points":
      return (
        <div className="story-point-label position-relative">
          <InputRangeFilter
            formClass="form-label"
            value={getFilterValue(filters, "story_points")}
            label={"STORY POINT"}
            onChange={(value: any) => onFilterValueChange(value, "story_points")}
          />
          <AntIcon
            className="story-point-delete-icon"
            type={"delete"}
            onClick={e => {
              e.stopPropagation();
              e.preventDefault();
              removeFilterOptionSelected("story_points");
            }}
          />
        </div>
      );

    case "parent_story_points":
      return (
        <div className="story-point-label position-relative">
          <InputRangeFilter
            formClass="form-label"
            value={getFilterValue(filters, "parent_story_points")}
            label={"PARENT STORY POINT"}
            onChange={(value: any) => onFilterValueChange(value, "parent_story_points")}
          />
          <AntIcon
            className="story-point-delete-icon"
            type={"delete"}
            onClick={e => {
              e.stopPropagation();
              e.preventDefault();
              removeFilterOptionSelected("parent_story_points");
            }}
          />
        </div>
      );

    case "issue_created_at":
      return (
        <div className="issue-time-filter">
          <IssueTimeFilterComponent
            filterKey={"issue_created_at"}
            filters={filters}
            label={"ISSUE CREATED IN"}
            onFilterValueChange={handleTimeRangeFilterValueChange}
            options={CreatedAtUpdateAtOptions}
            metaData={metaData}
            onRangeTypeChange={handleTimeRangeTypeChange}
            onDelete={removeFilterOptionSelected}
          />
        </div>
      );

    case "issue_updated_at":
      return (
        <div className="issue-time-filter">
          <IssueTimeFilterComponent
            filterKey={"issue_updated_at"}
            filters={filters}
            label={"Issue Updated In"}
            onFilterValueChange={handleTimeRangeFilterValueChange}
            options={CreatedAtUpdateAtOptions}
            metaData={metaData}
            onRangeTypeChange={handleTimeRangeTypeChange}
            onDelete={removeFilterOptionSelected}
          />
        </div>
      );

    case "created_at":
      let issueLabel = "Issue Created In";

      if (report.uri.includes("ncc_group_issues_values")) {
        issueLabel = "CREATED IN";
      }
      return (
        <div className="issue-time-filter">
          <IssueTimeFilterComponent
            filterKey={"created_at"}
            filters={filters}
            label={issueLabel}
            onFilterValueChange={handleTimeRangeFilterValueChange}
            options={CreatedAtUpdateAtOptions}
            metaData={metaData}
            onRangeTypeChange={handleTimeRangeTypeChange}
            onDelete={removeFilterOptionSelected}
          />
        </div>
      );

    case "salesforce_created_at":
      return (
        <div className="issue-time-filter">
          <IssueTimeFilterComponent
            filterKey={"salesforce_created_at"}
            filters={filters}
            label={"SALESFORCE ISSUE CREATED DATE"}
            onFilterValueChange={handleTimeRangeFilterValueChange}
            options={CreatedAtUpdateAtOptions}
            metaData={metaData}
            onRangeTypeChange={handleTimeRangeTypeChange}
            onDelete={removeFilterOptionSelected}
          />
        </div>
      );

    case "salesforce_updated_at":
      return (
        <div className="issue-time-filter">
          <IssueTimeFilterComponent
            filterKey={"salesforce_updated_at"}
            filters={filters}
            label={"SALESFORCE ISSUE UPDATED DATE"}
            onFilterValueChange={handleTimeRangeFilterValueChange}
            options={CreatedAtUpdateAtOptions}
            metaData={metaData}
            onRangeTypeChange={handleTimeRangeTypeChange}
            onDelete={removeFilterOptionSelected}
          />
        </div>
      );

    case "time_period":
      let label = "TIME PERIOD";

      if (["jenkins_jobs_filter_values", "cicd_filter_values"].includes(report.uri)) {
        label = "JOB START DATE";
      }

      if (
        ["jenkins_job_config_filter_values", "jenkins_jobs_filter_values", "cicd_filter_values"].includes(report.uri)
      ) {
        return (
          <div className="story-point-label position-relative">
            <Form.Item key={`time_period_${report.uri}`} label={label} className="form-label">
              <AntSelect
                value={filters.time_period}
                mode={"single"}
                options={jenkinsConfigTimePeriodOptions}
                onChange={(value: any, options: any) => onFilterValueChange(value, "time_period")}
              />
            </Form.Item>
            <AntIcon
              className="story-point-delete-icon"
              type={"delete"}
              onClick={e => {
                e.stopPropagation();
                e.preventDefault();
                removeFilterOptionSelected("time_period");
              }}
            />
          </div>
        );
      }
      return null;

    case "agg_type":
      if (report.uri === "jenkins_job_config_filter_values") {
        return (
          <div className="story-point-label position-relative">
            <Form.Item key={`aggs_type_${report.uri}`} label={"AGGREGATION TYPE"} className="form-label">
              <AntSelect
                value={filters.agg_type}
                mode={"single"}
                options={jenkinsConfigAggsType.sort(stringSortingComparator("label"))}
                onChange={(value: any, options: any) => onFilterValueChange(value, "agg_type")}
              />
            </Form.Item>
            <AntIcon
              className="story-point-delete-icon"
              type={"delete"}
              onClick={e => {
                e.stopPropagation();
                removeFilterOptionSelected("agg_type");
              }}
            />
          </div>
        );
      }

      if (report.uri === "cicd_filter_values") {
        return (
          <div className="story-point-label position-relative">
            <Form.Item key={`agg_type_${report.uri}`} label={"AGGREGATION TYPE"} className="form-label">
              <AntSelect
                value={filters.agg_type}
                mode={"single"}
                options={jenkinsJobsAggsType.sort(stringSortingComparator("label"))}
                onChange={(value: any, options: any) => onFilterValueChange(value, "agg_type")}
              />
            </Form.Item>
            <AntIcon
              className="story-point-delete-icon"
              type={"delete"}
              onClick={e => {
                e.stopPropagation();
                removeFilterOptionSelected("agg_type");
              }}
            />
          </div>
        );
      }
      return null;

    case "end_time":
      if (report.uri === "jenkins_pipelines_jobs_filter_values") {
        return (
          <div className="ui-filter">
            <div className="filter-input">
              <JenkinsJobTimeFilter
                report={report.report}
                filters={filters}
                metaData={metaData}
                filterKey={"end_time"}
                onRangeTypeChange={(key: string, value: any) => handleTimeRangeTypeChange(key, value)}
                application={report.application}
                onFilterValueChange={handleTimeRangeFilterValueChange}
              />
            </div>
            <div className="filter-delete-icon">
              <AntIcon
                className="filter-delete-icon--icon"
                type={"delete"}
                onClick={e => {
                  e.stopPropagation();
                  removeFilterOptionSelected("end_time");
                }}
              />
            </div>
          </div>
        );
      }
      return null;

    case "pr_created_at":
      return (
        <IssueTimeFilterComponent
          filterKey={"pr_created_at"}
          filters={filters}
          label={"PR Created In"}
          onFilterValueChange={handleTimeRangeFilterValueChange}
          options={CreatedAtUpdateAtOptions}
          metaData={metaData}
          onRangeTypeChange={handleTimeRangeTypeChange}
          onDelete={removeFilterOptionSelected}
        />
      );

    case "pr_closed_at":
      return (
        <IssueTimeFilterComponent
          filterKey={"pr_closed_at"}
          filters={filters}
          label={"PR Closed Time"}
          onFilterValueChange={handleTimeRangeFilterValueChange}
          options={CreatedAtUpdateAtOptions}
          metaData={metaData}
          onRangeTypeChange={handleTimeRangeTypeChange}
          onDelete={removeFilterOptionSelected}
        />
      );

    case "sort":
      if (report.uri === "scm_files_filter_values") {
        return (
          <div className="story-point-label position-relative">
            <Form.Item key={`sort_${report.uri}`} label={"Sort"} className="form-label">
              <AntSelect
                showArrow={true}
                value={getSortFilterValue()}
                options={scmFilesSortOptions.sort(stringSortingComparator("label"))}
                mode={"single"}
                onChange={(value: any, options: any) => onFilterValueChange(getMapSortValue(value), "sort")}
              />
            </Form.Item>
            <AntIcon
              className="story-point-delete-icon"
              type={"delete"}
              onClick={e => {
                e.stopPropagation();
                removeFilterOptionSelected("sort");
              }}
            />
          </div>
        );
      }
      return null;

    case "committed_at":
      return (
        <IssueTimeFilterComponent
          filterKey={"committed_at"}
          filters={filters}
          label={"COMMITTED IN"}
          onFilterValueChange={handleTimeRangeFilterValueChange}
          options={CreatedAtUpdateAtOptions}
          metaData={metaData}
          onRangeTypeChange={handleTimeRangeTypeChange}
          onDelete={removeFilterOptionSelected}
        />
      );

    case "ingested_at":
      return (
        <IssueTimeFilterComponent
          filterKey={"ingested_at"}
          filters={filters}
          label={"INGESTED IN"}
          onFilterValueChange={handleTimeRangeFilterValueChange}
          options={CreatedAtUpdateAtOptions}
          metaData={metaData}
          onRangeTypeChange={handleTimeRangeTypeChange}
          onDelete={removeFilterOptionSelected}
        />
      );

    case "n_last_reports":
      return (
        <div className="story-point-label position-relative">
          <Form.Item key={`n_last_reports_${report.uri}`} label={"Last Reports"} className="form-label">
            <AntInput
              type="number"
              value={filters.n_last_reports || 1}
              style={{ width: "100%" }}
              onChange={(e: number) => onFilterValueChange(e.toString(), "n_last_reports")}
            />
          </Form.Item>
          <AntIcon
            className="story-point-delete-icon"
            type={"delete"}
            onClick={e => {
              e.stopPropagation();
              removeFilterOptionSelected("n_last_reports");
            }}
          />
        </div>
      );

    case "metric":
      return (
        <div className="position-relative">
          <BullseyeMetricFilter reportType={report.report} filters={filters} onFilterChange={onFilterValueChange} />
          <AntIcon
            className="story-point-delete-icon"
            type={"delete"}
            onClick={e => {
              e.stopPropagation();
              removeFilterOptionSelected("metric");
            }}
          />
        </div>
      );

    case "score_range":
      return (
        <div className="story-point-label position-relative">
          <Form.Item key={`score_range_${report.uri}`} className="form-label">
            <InputRangeFilter
              value={getFilterValue(filters, "score_range")}
              label={"Priority Score"}
              onChange={(value: any) => onFilterValueChange(value, "score_range")}
            />
          </Form.Item>
          <AntIcon
            className="story-point-delete-icon"
            type={"delete"}
            onClick={e => {
              e.stopPropagation();
              removeFilterOptionSelected("score_range");
            }}
          />
        </div>
      );

    case "disclosure_range":
      return (
        <IssueTimeFilterComponent
          filterKey={"disclosure_range"}
          filters={filters}
          label={"DISCLOSURE TIME"}
          onFilterValueChange={handleTimeRangeFilterValueChange}
          options={CreatedAtUpdateAtOptions}
          metaData={metaData}
          onRangeTypeChange={handleTimeRangeTypeChange}
          onDelete={removeFilterOptionSelected}
        />
      );

    case "publication_range":
      return (
        <IssueTimeFilterComponent
          filterKey={"publication_range"}
          filters={filters}
          label={"PUBLICATION TIME"}
          onFilterValueChange={handleTimeRangeFilterValueChange}
          options={CreatedAtUpdateAtOptions}
          metaData={metaData}
          onRangeTypeChange={handleTimeRangeTypeChange}
          onDelete={removeFilterOptionSelected}
        />
      );

    case "parameters":
      return (
        <div className="ui-filter">
          <div className="filter-input">
            <JenkinsGithubParameters
              application={report.application}
              data={[]}
              filters={filters}
              reportType={report.report}
              onFilterValueChange={onFilterValueChange}
            />
          </div>
          <div className="filter-delete-icon">
            <AntIcon
              className="filter-delete-icon--icon"
              type={"delete"}
              onClick={e => {
                e.stopPropagation();
                removeFilterOptionSelected("parameters");
              }}
            />
          </div>
        </div>
      );

    case "group_by_modules":
    case "scm_module":
      if (filesFilters.includes(report.uri)) {
        return (
          <div className="story-point-label position-relative">
            <Form.Item>
              {get(metaData, [getGroupByRootFolderKey(report.uri)], false) && (
                <ModulePathFilter
                  uri={get(widgetConstants, [report.report, "rootFolderURI"], "")}
                  value={
                    (GlobalFilesFilters.SCM_FILES_FILTERS === report.uri ? filters?.module : filters?.scm_module) || ""
                  }
                  integrationIds={integrationIds}
                  onChange={value => {
                    onFilterValueChange(
                      value,
                      GlobalFilesFilters.SCM_FILES_FILTERS === report.uri ? "module" : "scm_module"
                    );
                  }}
                />
              )}
              <Checkbox
                checked={get(metaData, [getGroupByRootFolderKey(report.uri)], false)}
                onChange={(e: CheckboxChangeEvent) => onFilterValueChange(e.target.checked, "group_by_modules")}
                className="text-uppercase"
                style={{
                  fontSize: "12px",
                  fontWeight: 700,
                  color: "#575757"
                }}>
                <AntText style={{ marginRight: "4px" }}>{"Group By Modules"}</AntText>
                <AntTooltip title="Root folders are the top most folders in a file system">
                  <Icon type="info-circle" />
                </AntTooltip>
              </Checkbox>
            </Form.Item>
            <AntIcon
              className="story-point-delete-icon"
              type={"delete"}
              onClick={e => {
                e.stopPropagation();
                removeFilterOptionSelected("group_by_modules");
              }}
            />
          </div>
        );
      }
      return null;
    case "teams":
      return (
        <AzureTeamsFilter
          withDelete={{
            key: "teams",
            onDelete: removeFilterOptionSelected,
            showDelete: true
          }}
          filters={filters}
          onFilterValueChange={onFilterValueChange}
          visibleFiltersConfig={{ code_area: false, teams: true }}
        />
      );
    case "code_area":
      return (
        <AzureTeamsFilter
          withDelete={{
            key: "code_area",
            onDelete: removeFilterOptionSelected,
            showDelete: true
          }}
          filters={filters}
          onFilterValueChange={onFilterValueChange}
          visibleFiltersConfig={{ code_area: true, teams: false }}
        />
      );
  }
};
