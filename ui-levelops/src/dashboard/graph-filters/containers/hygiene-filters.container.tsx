import React, { useContext } from "react";

import "./hygiene-filters.container.scss";
import { DashboardGraphFilters, HygieneJiraFilter } from "../components";
import { WidgetTabsContext } from "../../pages/context";
import { WIDGET_CONFIGURATION_KEYS } from "../../../constants/widgets";
import HygieneSettingsComponent from "../components/HygieneSettings";
import IssueManagementSystemFilter from "../components/IssueManagementSystemFilter";
import { getMetadataValue } from "configurable-dashboard/helpers/helper";
import {
  HYGIENE_TREND_REPORT,
  ISSUE_MANAGEMENT_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT
} from "dashboard/constants/applications/names";
import { Form } from "antd";
import { AntSelect } from "../../../shared-resources/components";
import { get } from "lodash";
import { hygieneIntervalReport, hygieneVisualizationOptions, ITEM_TEST_ID } from "../components/Constants";
import OUFiltersComponent from "../components/OUFilters/OUFilters.component";
import { extraReportWithTicketCategorizationFilter } from "../../constants/bussiness-alignment-applications/constants";

interface HygieneFiltersContainerProps {
  data: Array<any>;
  customData?: Array<any>;
  onFilterValueChange: (value: any, type?: any) => void;
  onWeightChange: (value: any, type?: any) => void;
  onCustomFilterValueChange?: (value: any, type?: any) => void;
  onExcludeChange?: (key: string, value: boolean) => void;
  onAggregationAcrossSelection?: (value: any) => void;
  filters: any;
  application: string;
  reportType: string;
  weightError: string;
  widgetWeights: any;
  customHygienes?: Array<any>;
  acrossOptions?: Array<any>;
  onTimeRangeTypeChange?: (key: string, value: any) => void;
  metaData?: any;
  onPartialChange: (key: string, value: any) => void;
  partialFilterError?: any;
  onTimeFilterValueChange?: (value: any, type?: any, rangeType?: string) => void;
  hasNext?: boolean;
  sprintData: Array<any>;
  handleLastSprintChange?: (value: boolean, filterKey: string) => void;
  onMetadataChange?: (value: any, type: any, reportType?: String) => void;
  integrationIds: string[];
  isCompositeChild?: boolean;
  dashboardMetaData?: any;
  fieldTypeList?: { key: string; type: string; name: string }[];
  queryParamDashboardOUId?: any;
}

const HygieneFiltersContainer: React.FC<HygieneFiltersContainerProps> = (props: HygieneFiltersContainerProps) => {
  const { isVisibleOnTab } = useContext(WidgetTabsContext);
  const { isCompositeChild } = props;
  const showOUFilters =
    !!(props.queryParamDashboardOUId || props.dashboardMetaData?.ou_ids || []).length &&
    ["jira", "jenkins", "github", "azure_devops", "jenkinsgithub", "githubjira"].includes(props.application) &&
    !extraReportWithTicketCategorizationFilter.includes(props.reportType);

  if (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS)) {
    return (
      <DashboardGraphFilters
        application={props.application}
        customData={props.customData}
        onCustomFilterValueChange={props.onCustomFilterValueChange}
        onAggregationAcrossSelection={props.onAggregationAcrossSelection}
        data={props.data}
        filters={props.filters}
        reportType={props.reportType}
        acrossOptions={props.acrossOptions}
        onFilterValueChange={props.onFilterValueChange}
        applicationUse={false}
        onExcludeChange={props.onExcludeChange}
        metaData={props.metaData}
        sprintData={props.sprintData}
        onPartialChange={props.onPartialChange}
        onTimeRangeTypeChange={props.onTimeRangeTypeChange}
        onTimeFilterValueChange={props.onTimeFilterValueChange}
        partialFilterError={props.partialFilterError}
        hasNext={props.hasNext}
        handleLastSprintChange={props.handleLastSprintChange}
        integrationIds={props.integrationIds}
        isCompositeChild={isCompositeChild}
        onMetadataChange={props.onMetadataChange}
        dashboardMetaData={props.dashboardMetaData}
        fieldTypeList={props.fieldTypeList}
      />
    );
  }

  if (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.WEIGHTS)) {
    return (
      <HygieneJiraFilter
        widgetWeights={props.widgetWeights}
        onWeightChange={props.onWeightChange}
        onFilterValueChange={props.onFilterValueChange}
        weightError={props.weightError}
        application={props.application}
        reportType={props.reportType}
        filters={props.filters}
        customHygienes={props.customHygienes}
      />
    );
  }

  if (isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS)) {
    return (
      <>
        <HygieneSettingsComponent
          application={props.application}
          filters={props.filters}
          onFilterValueChange={props.onFilterValueChange}
          reportType={props.reportType}
        />
        {HYGIENE_TREND_REPORT.includes(props.reportType as any) && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
          <>
            <Form.Item
              key="sample_interval"
              label={"Sample Interval"}
              data-filterselectornamekey={`${ITEM_TEST_ID}-hygiene-sample-interval`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-hygiene-sample-interval`}>
              <AntSelect
                dropdownTestingKey={`${ITEM_TEST_ID}-hygiene-sample-interval_dropdown`}
                showArrow
                value={get(props.filters, ["interval"], "month")}
                options={hygieneIntervalReport}
                mode="single"
                onChange={(value: any, options: any) => props.onFilterValueChange(value, "interval")}
              />
            </Form.Item>
            <Form.Item
              key="visualization"
              label={"Visualization"}
              data-filterselectornamekey={`${ITEM_TEST_ID}-hygiene-visualization`}
              data-filtervaluesnamekey={`${ITEM_TEST_ID}-hygiene-visualization`}>
              <AntSelect
                dropdownTestingKey={`${ITEM_TEST_ID}-hygiene-visualization_dropdown`}
                showArrow
                value={get(props.filters, ["visualization"], "stacked_area")}
                options={hygieneVisualizationOptions}
                mode="single"
                onChange={(value: any, options: any) => props.onFilterValueChange(value, "visualization")}
              />
            </Form.Item>
          </>
        )}
        {[
          JIRA_MANAGEMENT_TICKET_REPORT.HYGIENE_REPORT,
          JIRA_MANAGEMENT_TICKET_REPORT.HYGIENE_REPORT_TREND,
          ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT,
          ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT_TREND
        ].includes(props.reportType as any) && (
          <IssueManagementSystemFilter
            disabled={getMetadataValue(props.metaData, "disable_issue_management_system", false)}
            filterValue={props.application.includes("azure_devops") ? "azure_devops" : "jira"}
            onMetadataChange={props.onMetadataChange}
          />
        )}
        {showOUFilters && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
          <OUFiltersComponent
            reportType={props.reportType}
            metaData={props.metaData}
            onMetadataChange={props.onMetadataChange}
          />
        )}
      </>
    );
  }

  return null;
};

export default HygieneFiltersContainer;
