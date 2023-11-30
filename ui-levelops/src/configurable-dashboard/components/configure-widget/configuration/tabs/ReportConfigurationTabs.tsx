import React, { useCallback, useContext, useEffect, useMemo, useState } from "react";
import { Spin, Tabs } from "antd";

import "./ConfigurationTabs.scss";
import { RestDashboard, RestWidget } from "../../../../../classes/RestDashboards";
import { WidgetPayloadContext, WidgetTabsContext } from "../../../../../dashboard/pages/context";
import { WIDGET_CONFIGURATION_KEYS, WIDGET_CONFIGURATION_PARENT_KEYS } from "../../../../../constants/widgets";
import WidgetConfigurationContainer from "../WidgetConfigurationContainer";
import { getSelectedChildWidget } from "reduxConfigs/selectors/widgetSelector";
import { useDispatch, useSelector } from "react-redux";
import JiraOrQueryFiltersWidgetConfigure from "./JiraOrQueryFiltersWidgetConfigure";
import { JIRA_WIDGETS_TYPE } from "dashboard/constants/applications/constant";
import { AntParagraph } from "../../../../../shared-resources/components";
import { get } from "lodash";
import { widgetUpdate } from "reduxConfigs/actions/restapi";
import {
  ALL_VELOCITY_PROFILE_REPORTS,
  AZURE_LEAD_TIME_ISSUE_REPORT,
  JIRA_MANAGEMENT_TICKET_REPORT,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import { setRequiredFieldError } from "reduxConfigs/actions/requiredField";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import useVelocityConfigProfiles from "custom-hooks/useVelocityConfigProfiles";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import widgetConstants from "dashboard/constants/widgetConstants";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import {
  cachedIntegrationsListSelector,
  cachedIntegrationsLoadingAndError
} from "reduxConfigs/selectors/CachedIntegrationSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getCachedIntegrations } from "reduxConfigs/actions/cachedIntegrationActions";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import AntWarning from "shared-resources/components/ant-warning/AntWarning";

const { TabPane } = Tabs;

interface ReportConfigurationTabsProps {
  dashboardId: string;
  widgetId: string;
}

const availableFiltersDict: any = {
  [WIDGET_CONFIGURATION_KEYS.FILTERS]: 0,
  [WIDGET_CONFIGURATION_KEYS.METRICS]: 0,
  [WIDGET_CONFIGURATION_KEYS.AGGREGATIONS]: 0,
  [WIDGET_CONFIGURATION_KEYS.WEIGHTS]: 0,
  [WIDGET_CONFIGURATION_KEYS.SETTINGS]: 0,
  [WIDGET_CONFIGURATION_PARENT_KEYS.SETTINGS]: 0
};
const NEXT_ACTION_KEY = "action_next";

const ReportConfigurationTabs: React.FC<ReportConfigurationTabsProps> = ({ dashboardId, widgetId }) => {
  const [activeKey, setActiveKey] = useState<WIDGET_CONFIGURATION_KEYS>(WIDGET_CONFIGURATION_KEYS.FILTERS);
  const [filtersDisabled, setFiltersDisabled] = useState(true);
  const [metricsDisabled, setMetricsDisabled] = useState(true);
  const [aggregationsDisabled, setAggregationsDisabled] = useState(true);
  const [parentActiveKey, setParentActiveKey] = useState<WIDGET_CONFIGURATION_PARENT_KEYS>(
    WIDGET_CONFIGURATION_PARENT_KEYS.SETTINGS
  );
  const [showChildTabs, setShowChildTabs] = useState<boolean>(false);
  const [integrationsLoading, setIntegrationsLoading] = useState<boolean>(true);
  const widget: RestWidget | null = useSelector(getSelectedChildWidget);

  const selectedOUState = useSelector(getSelectedOU);
  const workspaceProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU: selectedOUState?.id });

  const dashboard: RestDashboard | null = useSelector(selectedDashboard);
  const integrationIds = get(dashboard, ["query", "integration_ids"], []);
  const integrationsLoadingState = useSelector(cachedIntegrationsLoadingAndError);
  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });
  const { showAggregationTab, showWeightsTab, showMetricsTab, showFiltersTab } = widget || {};
  const { payload } = useContext(WidgetPayloadContext);

  useEffect(() => {
    dispatch(getCachedIntegrations("list", undefined, integrationIds));
  }, []);

  useEffect(() => {
    const loading = get(integrationsLoadingState, "loading", true);
    const error = get(integrationsLoadingState, "error", true);
    if (!loading && !error) {
      setIntegrationsLoading(false);
    }
  }, [integrationsLoadingState]);

  const handleTabChange = useCallback((key: string) => {
    setActiveKey(key as WIDGET_CONFIGURATION_KEYS);
  }, []);

  const handleParentTabChange = useCallback((key: string) => {
    setParentActiveKey(key as WIDGET_CONFIGURATION_PARENT_KEYS);
  }, []);

  const resetCounters = () => {
    availableFiltersDict[WIDGET_CONFIGURATION_KEYS.FILTERS] = 0;
    availableFiltersDict[WIDGET_CONFIGURATION_KEYS.METRICS] = 0;
    availableFiltersDict[WIDGET_CONFIGURATION_KEYS.AGGREGATIONS] = 0;
    availableFiltersDict[WIDGET_CONFIGURATION_KEYS.WEIGHTS] = 0;
    availableFiltersDict[WIDGET_CONFIGURATION_KEYS.SETTINGS] = 0;
    availableFiltersDict[WIDGET_CONFIGURATION_PARENT_KEYS.SETTINGS] = 0;
  };

  const dispatch = useDispatch();
  const { defaultProfile } = useVelocityConfigProfiles(widget?.type);
  useEffect(() => {
    if (ALL_VELOCITY_PROFILE_REPORTS.includes(widget?.type)) {
      if (defaultProfile && !widget?.query?.velocity_config_id) {
        const newQuery = {
          ...widget?.query,
          velocity_config_id: defaultProfile?.id
        };
        dispatch(widgetUpdate(widgetId, { ...widget?.json, query: newQuery }));
      }
    } else if (widget?.query.velocity_config_id) {
      delete widget?.query.velocity_config_id;
      dispatch(widgetUpdate(widgetId, { ...widget?.json }));
    }
  }, [defaultProfile]);

  useEffect(() => {
    if (ALL_VELOCITY_PROFILE_REPORTS.includes(widget?.type)) {
      if (!widget?.query.velocity_config_id) {
        dispatch(
          setRequiredFieldError({
            is_required_error_field: true,
            required_field_msg: "Workflow Configuration Profile is required"
          })
        );
      } else {
        dispatch(setRequiredFieldError({ is_required_error_field: false, required_field_msg: "" }));
      }
    }
  }, [widget?.type, widget?.query.velocity_config_id]);

  useEffect(() => {
    resetCounters();
    // TODO NEED TO FIGURE OUT
    // setAggregationsDisabled(true);
    // setFiltersDisabled(true);
    // setMetricsDisabled(true);
  }, [widget]);

  const memoizedContextValue = useMemo(() => {
    resetCounters();
    return {
      isVisibleOnTab: (tab: WIDGET_CONFIGURATION_KEYS | WIDGET_CONFIGURATION_PARENT_KEYS, isParentTab?: boolean) => {
        availableFiltersDict[tab] = availableFiltersDict[tab] + 1;
        if (isParentTab) {
          return tab === parentActiveKey;
        }
        switch (tab) {
          case WIDGET_CONFIGURATION_KEYS.AGGREGATIONS:
            setAggregationsDisabled(false);
            break;
          case WIDGET_CONFIGURATION_KEYS.FILTERS:
            setFiltersDisabled(false);
            break;
          case WIDGET_CONFIGURATION_KEYS.METRICS:
            setMetricsDisabled(false);
            break;
        }
        return activeKey === tab;
      }
    };
  }, [activeKey, parentActiveKey]);

  const pageSettings: any = useSelector(getPageSettingsSelector);
  useEffect(() => {
    const isButtonClicked =
      pageSettings[window.location.href.substring(1)]?.action_buttons?.[NEXT_ACTION_KEY].hasClicked;

    if (
      isButtonClicked &&
      widget?.type === AZURE_LEAD_TIME_ISSUE_REPORT.LEAD_TIME_SINGLE_STAT &&
      !widget?.query.velocity_config_id
    ) {
      setActiveKey(WIDGET_CONFIGURATION_KEYS.SETTINGS);
    }
  }, [pageSettings]);

  const renderActions = useMemo(() => {
    const widgetPayload = get(payload, widgetId, {});
    const payloadString = JSON.stringify(widgetPayload);

    return (
      <>
        <AntParagraph className="widget-payload-copy" copyable={{ text: payloadString }}>
          Copy Widget Payload
        </AntParagraph>
        <AntParagraph className="widget-payload-copy" copyable={{ text: window.location.href }}>
          Copy Widget Url
        </AntParagraph>
      </>
    );
  }, [payload, widgetId]);

  const showTab = useCallback(
    (tab: WIDGET_CONFIGURATION_KEYS) => {
      if (widget?.hasFilterConfigs) {
        let widgetFilterConfig = get(widgetConstants, [widget.type, REPORT_FILTERS_CONFIG], undefined);
        if (widgetFilterConfig && typeof widgetFilterConfig === "function") {
          return (
            widgetFilterConfig({
              filters: widget.query,
              integrationState: integrations,
              workspaceProfile: workspaceProfile
            })?.filter((item: LevelOpsFilter) => item?.tab === tab)?.length > 0
          );
        }
        return widget.filterConfig.filter((item: LevelOpsFilter) => item.tab === tab)?.length > 0;
      }
      if (tab === WIDGET_CONFIGURATION_KEYS.FILTERS) return showFiltersTab;
      if (tab === WIDGET_CONFIGURATION_KEYS.AGGREGATIONS) return showAggregationTab;
      if (tab === WIDGET_CONFIGURATION_KEYS.WEIGHTS) return showWeightsTab;
      if (tab === WIDGET_CONFIGURATION_KEYS.METRICS) return showMetricsTab;
    },
    [showFiltersTab, showAggregationTab, showWeightsTab, showMetricsTab, widget, integrations]
  );

  if (!widget?.type) {
    return null;
  }

  if (integrationsLoading) {
    return <Spin className="centered" />;
  }

  const showContent = (isParentTabData?: boolean) => (
    <WidgetConfigurationContainer
      dashboardId={dashboardId}
      widgetId={widgetId}
      isParentTabData={isParentTabData}
      advancedTabState={{ value: showChildTabs, callback: setShowChildTabs }}
    />
  );

  const showAdvancedFilters = get(widgetConstants, [widget.type, "isAdvancedFilterSetting"], false);
  const filterWarningLabel = get(widgetConstants, [widget.type, "filterWarningLabel"], undefined);

  if (showAdvancedFilters) {
    return (
      <WidgetTabsContext.Provider value={memoizedContextValue}>
        <div className="report-configuration-tabs">
          <Tabs
            className="h-100"
            defaultActiveKey={WIDGET_CONFIGURATION_PARENT_KEYS.SETTINGS}
            onChange={handleParentTabChange}
            destroyInactiveTabPane
            activeKey={parentActiveKey}>
            <TabPane className="h-100 o-auto" tab="Settings" key={WIDGET_CONFIGURATION_PARENT_KEYS.SETTINGS}>
              {showContent(true)}
              {showChildTabs && (
                <Tabs defaultActiveKey={activeKey} onChange={handleTabChange} activeKey={activeKey}>
                  {
                    <TabPane className="h-100 o-auto" tab="Filters" key={WIDGET_CONFIGURATION_KEYS.FILTERS}>
                      {JIRA_WIDGETS_TYPE.includes(widget.type) &&
                        ![JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT].includes(widget.type) && (
                          <JiraOrQueryFiltersWidgetConfigure dashboardId={dashboardId} widgetId={widgetId} />
                        )}
                      {filterWarningLabel && <AntWarning label={filterWarningLabel} />}
                      {showContent()}
                    </TabPane>
                  }
                  <TabPane className="h-100 o-auto" tab="Others" key={WIDGET_CONFIGURATION_KEYS.OTHERS}>
                    {renderActions}
                    {showContent()}
                  </TabPane>
                </Tabs>
              )}
            </TabPane>
          </Tabs>
        </div>
      </WidgetTabsContext.Provider>
    );
  }

  return (
    <WidgetTabsContext.Provider value={memoizedContextValue}>
      <div className="report-configuration-tabs">
        <Tabs
          className="h-100"
          defaultActiveKey={activeKey}
          onChange={handleTabChange}
          destroyInactiveTabPane
          activeKey={activeKey}>
          {showTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && (
            <TabPane
              className="h-100 o-auto"
              tab="Filters"
              key={WIDGET_CONFIGURATION_KEYS.FILTERS}
              disabled={filtersDisabled}>
              {JIRA_WIDGETS_TYPE.includes(widget.type) &&
                ![JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT].includes(widget.type) && (
                  <JiraOrQueryFiltersWidgetConfigure dashboardId={dashboardId} widgetId={widgetId} />
                )}

              {showContent()}
            </TabPane>
          )}
          {showTab(WIDGET_CONFIGURATION_KEYS.METRICS) && (
            <TabPane
              className="h-100 o-auto"
              tab="Metrics"
              key={WIDGET_CONFIGURATION_KEYS.METRICS}
              disabled={metricsDisabled}>
              {showContent()}
            </TabPane>
          )}
          {showTab(WIDGET_CONFIGURATION_KEYS.AGGREGATIONS) && (
            <TabPane
              className="h-100 o-auto"
              tab="Aggregations"
              key={WIDGET_CONFIGURATION_KEYS.AGGREGATIONS}
              disabled={aggregationsDisabled}>
              {showContent()}
            </TabPane>
          )}
          {showTab(WIDGET_CONFIGURATION_KEYS.WEIGHTS) && (
            <TabPane className="h-100 o-auto" tab="Weights" key={WIDGET_CONFIGURATION_KEYS.WEIGHTS}>
              {showContent()}
            </TabPane>
          )}
          <TabPane className="h-100 o-auto" tab="Settings" key={WIDGET_CONFIGURATION_KEYS.SETTINGS}>
            {renderActions}
            {showContent()}
          </TabPane>
        </Tabs>
      </div>
    </WidgetTabsContext.Provider>
  );
};

export default React.memo(ReportConfigurationTabs);

// TODO: rename component and containers and move them under right directories
