import React, { useCallback, useEffect, useState } from "react";
import { forEach, get, unset, cloneDeep, uniq } from "lodash";
import { Button, Col, Modal, Tabs } from "antd";

import "./DashboardApplicationFilters.scss";

import {
  removeFiltersWithEmptyValues,
  removeGlobalEmptyValues,
  trimPartialStringFilters
} from "shared-resources/containers/widget-api-wrapper/helper";

import { getGroupByRootFolderKey } from "../../../configurable-dashboard/helpers/helper";
import DashboardFiltersWrapper from "./DashboardFiltersWrapper/DashboardFiltersWrapper";
import WidgetDefaultsWrapper from "./WidgetDefaultsWrapper/WidgetDefaultsWrapper";
import { valuesToFilters } from "../../constants/constants";
import { removeFilterKey, getOrderedFilterKeys, getGlobalOrderedFilterKeys } from "./AddFiltersComponent/helpers";
import { filterCount } from "../dashboard-header/helper";
import { selectedDashboardIntegrations } from "reduxConfigs/selectors/integrationSelector";
import { jiraSupportedFilters } from "../../constants/supported-filters.constant";
import { getApplicationFilters, sanitizeGlobalMetaDataFilters, sanitizeMetaDataFilters } from "./helper";
import { filesFilters, GlobalFilesFilters } from "./AddFiltersComponent/filterConstants";
import { ouUserDesignationType } from "configurations/configuration-types/OUTypes";
import OUUsersDefaultWrapper from "./OUUsersDefaultWrapper/OUUsersDefaultWrapper";
import { OU_USER_FILTER_DESIGNATION_KEY } from "./constants";
import { sanitizeObject } from "utils/commonUtils";
import { useMemo } from "react";
import { useSelector } from "react-redux";
import { buildExcludeQuery, buildWidgetQuery, buildPartialQuery } from "configurable-dashboard/helpers/queryHelper";

const { TabPane } = Tabs;
interface DashboardApplicationFiltersProps {
  visible: boolean;
  onOk: (filters: any, update?: boolean) => void;
  onCancel: () => void;
  integrationIds: Array<any>;
  ouUserFilterDesignations: ouUserDesignationType;
  showOUOverrides: boolean;
  filters: any;
  jiraOrFilters: any;
  viewMode?: boolean;
  ou_ids: string;
}

const DashboardApplicationFilters: React.FC<DashboardApplicationFiltersProps> = (
  props: DashboardApplicationFiltersProps
) => {
  const { visible, onCancel, onOk, integrationIds, filters } = props;
  const [activeKey, setActiveKey] = useState<string>("dashboard_filters");

  const [globalApplicationFilters, setGlobalApplicationFilters] = useState<any>({});
  const [jiraOrFilters, setJiraOrFilters] = useState<any>({});
  const [partialFiltersErrors, setPartialFiltersErrors] = useState<any>({});

  const [globalApplicationOrderedFilters, setGlobalApplicationOrderedFilters] = useState<{ [key: string]: string[] }>(
    {}
  );
  const [jiraOrOrderedFilters, setJiraOrOrderedFilters] = useState<string[]>([]);
  const [ouUserFilterDesignations, setOUUserFiltersDesignation] = useState<ouUserDesignationType>({});

  const integrationsRecords = useSelector(selectedDashboardIntegrations);

  const availableApplications: string[] = useMemo(() => {
    const apps = (integrationsRecords || []).map((integration: any) => integration.application);
    return uniq(apps);
  }, [integrationsRecords]);

  useEffect(() => {
    if (!props.showOUOverrides && activeKey === "ou-user-filter-designation") {
      const activeTab = (availableApplications || []).includes("jira") ? "dashboard_filters" : "widget_filters";
      setActiveKey(activeTab);
    }
  }, [props.showOUOverrides, availableApplications]);

  useEffect(() => {
    if (!(availableApplications || []).includes("jira") && activeKey === "dashboard_filters") {
      setActiveKey("widget_filters");
    }
  }, [availableApplications]);

  useEffect(() => {
    if (visible && filters) {
      // add sanitization code
      setGlobalApplicationFilters(sanitizeGlobalMetaDataFilters({ ...filters }));
      setJiraOrFilters(sanitizeMetaDataFilters({ ...props.jiraOrFilters }, jiraSupportedFilters));

      setGlobalApplicationOrderedFilters(getGlobalOrderedFilterKeys(props.filters));
      setJiraOrOrderedFilters(getOrderedFilterKeys(props.jiraOrFilters, jiraSupportedFilters.values));
    }
    if (visible && !!props.ouUserFilterDesignations && Object.keys(props.ouUserFilterDesignations).length > 0) {
      setOUUserFiltersDesignation(props.ouUserFilterDesignations);
    }
  }, [props.visible]);

  const onFilterChange = (value: any, report: string, type?: any, exclude?: boolean) => {
    if (type !== "group_by_modules") {
      if (type !== "scm_module" && type !== "module") {
        setGlobalApplicationFilters((filters: any) => ({
          ...filters,
          [report]: buildWidgetQuery(get(filters, [report], {}), value, type, exclude)
        }));
      } else {
        const filterKey = report === GlobalFilesFilters.SCM_FILES_FILTERS ? "module" : "scm_module";
        const repoFilterKey = report === GlobalFilesFilters.SCM_FILES_FILTERS ? "repo_ids" : "scm_file_repo_ids";
        let _filters = {
          ...globalApplicationFilters,
          [report]: {
            ...buildWidgetQuery(get(globalApplicationFilters, [report], {}), value.module, filterKey, exclude)
          }
        };
        if (get(_filters, [report], {}).hasOwnProperty(repoFilterKey)) {
          if (value.repoId) {
            _filters = {
              ..._filters,
              [report]: {
                ...get(_filters, [report], {}),
                [repoFilterKey]: [value.repoId]
              }
            };
          }
        }
        setGlobalApplicationFilters({
          ..._filters
        });
      }
    } else {
      const filterKey = report === GlobalFilesFilters.SCM_FILES_FILTERS ? "module" : "scm_module";
      const newFilters = removeFilterKey(globalApplicationFilters[report], filterKey);
      setGlobalApplicationFilters((filters: any) => ({
        ...filters,
        [report]: { ...newFilters },
        ["metadata"]: {
          ...(filters.metadata || {}),
          [report]: {
            ...(filters?.metadata?.report || {}),
            [getGroupByRootFolderKey(report)]: value
          }
        }
      }));
    }
    setPartialFiltersErrors((prev: any) => ({
      ...prev,
      [type]: undefined
    }));
  };

  const handleExcludeFilter = (key: string, value: boolean, uri: string) => {
    setGlobalApplicationFilters((filters: any) => ({
      ...filters,
      [uri]: buildExcludeQuery(get(filters, [uri], {}), key, value)
    }));
    setPartialFiltersErrors((prev: any) => ({
      ...prev,
      [key]: undefined
    }));
  };

  const handleTimeRangeTypeChange = (key: string, value: any, uri: string) => {
    setGlobalApplicationFilters((filters: any) => ({
      ...(filters || {}),
      metadata: {
        ...(filters.metadata || {}),
        [uri]: {
          ...(filters?.metadata?.[uri] || {}),
          range_filter_choice: {
            ...(filters?.metadata?.[uri]?.range_filter_choice || {}),
            [key]: value
          }
        }
      }
    }));
  };

  const handlePartialFiltersChange = (key: string, value: any, report: string, uri: string) => {
    const { filters: newFilters, error } = buildPartialQuery(globalApplicationFilters[uri] || {}, key, value, report);
    if (!!error) {
      setPartialFiltersErrors((prev: any) => ({ ...prev, [uri]: { ...(prev[uri] || {}), [key]: error } }));
    } else {
      setPartialFiltersErrors((prev: any) => ({
        ...prev,
        [uri]: { ...(prev[uri] || {}), [key]: undefined }
      }));
      const initialValue = !value ? { [(valuesToFilters as any)[key]]: [] } : {};
      setGlobalApplicationFilters((filters: any) => ({
        ...(filters || {}),
        [uri]: { ...newFilters, ...initialValue }
      }));
    }
  };

  const handleTimeRangeFilterValueChange = (
    value: any,
    uri: string,
    type?: any,
    rangeType?: string,
    isCustom: boolean = false
  ) => {
    setGlobalApplicationFilters((filters: any) => {
      if (!isCustom) {
        return {
          ...(filters || {}),
          [uri]: {
            ...(filters?.[uri] || {}),
            [type]: value.absolute
          },
          metadata: {
            ...(filters.metadata || {}),
            [uri]: {
              ...(filters?.metadata?.[uri] || {}),
              range_filter_choice: {
                ...(filters?.metadata?.[uri]?.range_filter_choice || {}),
                [type]: { type: value.type, relative: value.relative }
              }
            }
          }
        };
      } else {
        return {
          ...(filters || {}),
          [uri]: {
            ...(filters?.[uri] || {}),
            ["custom_fields"]: {
              ...(filters?.[uri]?.custom_fields || {}),
              [type]: value.absolute
            }
          },
          metadata: {
            ...(filters.metadata || {}),
            [uri]: {
              ...(filters?.metadata?.[uri] || {}),
              range_filter_choice: {
                ...(filters?.metadata?.[uri]?.range_filter_choice || {}),
                [type]: { type: value.type, relative: value.relative }
              }
            }
          }
        };
      }
    });
  };

  const handleRemoveReportFilters = useCallback(
    (report: string) => {
      let _filters = { ...globalApplicationFilters };

      delete _filters[report];
      delete _filters?.metadata?.[report];

      if (filesFilters.includes(report as GlobalFilesFilters)) {
        delete _filters.metadata?.[report];
      }

      setGlobalApplicationFilters({ ..._filters });
      setGlobalApplicationOrderedFilters(state => {
        delete state[report];
        return { ...state };
      });
    },
    [globalApplicationFilters]
  );

  const handleAddReportFilters = useCallback(
    (report: string) => {
      setGlobalApplicationFilters((filter: any) => ({ ...filter, [report]: {} }));
      setGlobalApplicationOrderedFilters(state => ({ ...state, [report]: [] }));
    },
    [globalApplicationFilters]
  );

  const setGlobalOrderedFilters = useCallback(
    (keys: string[], uri: string) => {
      setGlobalApplicationOrderedFilters((filters: any) => ({
        ...filters,
        [uri]: keys
      }));
    },
    [globalApplicationFilters]
  );

  const getMappedGlobalFilters = useCallback(() => {
    let updatedFilters: any = {};
    forEach(Object.keys(globalApplicationFilters), key => {
      updatedFilters[key] = trimPartialStringFilters(globalApplicationFilters[key]);
    });
    let _filters = {
      global_filters: removeGlobalEmptyValues(updatedFilters),
      jira_or_filters: removeFiltersWithEmptyValues(trimPartialStringFilters(jiraOrFilters), "default", {}, false),
      [OU_USER_FILTER_DESIGNATION_KEY]: sanitizeObject(ouUserFilterDesignations),
      ou_ids: props.ou_ids || ""
    };
    return _filters;
  }, [globalApplicationFilters, jiraOrFilters, ouUserFilterDesignations, props.ou_ids]);

  const onFilterRemoved = (key: string, uri: string) => {
    setGlobalApplicationFilters((filters: any) => {
      let updatedFilters = cloneDeep(filters);
      unset(updatedFilters, ["metadata", uri, "range_filter_choice", key]);
      if (filesFilters.includes(uri as GlobalFilesFilters) && key === "group_by_modules") {
        const newFilters = removeFilterKey(
          filters[uri],
          uri === GlobalFilesFilters.SCM_FILES_FILTERS ? "module" : "scm_module"
        );
        unset(updatedFilters.metadata[uri], [getGroupByRootFolderKey(uri)]);
        updatedFilters = {
          ...updatedFilters,
          [uri]: newFilters
        };
      } else {
        const newFilters = removeFilterKey(filters[uri], key);
        updatedFilters = {
          ...updatedFilters,
          [uri]: newFilters
        };
      }
      return updatedFilters;
    });
  };

  const handleSetOUUserFiltersDesignation = useCallback(
    (type: string, value: string[]) => {
      let nDesignation = cloneDeep(ouUserFilterDesignations);
      nDesignation = {
        ...(nDesignation || {}),
        [type]: value || []
      };
      setOUUserFiltersDesignation(nDesignation);
    },
    [ouUserFilterDesignations]
  );

  let all_global_filters = Object.keys(getApplicationFilters());

  const widgetFilterCount = Object.entries(globalApplicationFilters).reduce((count: number, [key, filter]: any) => {
    // Added this logic for some already set invalid URI in filters
    if (all_global_filters.includes(key)) {
      return count + filterCount(filter);
    }
    return count;
  }, 0);
  const dashboardFilterCount = filterCount(jiraOrFilters);

  return (
    <Modal
      destroyOnClose={true}
      wrapClassName={"application-filters"}
      style={{ top: 15 }}
      title={"Filters"}
      visible={visible}
      onCancel={onCancel}
      footer={
        <>
          <Button key="cancel" onClick={onCancel}>
            Cancel
          </Button>
          <Button
            key={"update"}
            type={props.viewMode ? "primary" : "default"}
            onClick={e => onOk(getMappedGlobalFilters(), true)}>
            {props.viewMode ? "Save" : "Update Widgets"}
          </Button>
          {!props.viewMode && (
            <Button key="save" type="primary" onClick={e => onOk(getMappedGlobalFilters(), false)}>
              Save
            </Button>
          )}
        </>
      }>
      <div style={{ width: "100%", height: "100%" }}>
        <Col span={24}>
          <Tabs activeKey={activeKey} animated={false} size={"small"} onChange={(key: string) => setActiveKey(key)}>
            {(availableApplications || []).includes("jira") && (
              <TabPane key={"dashboard_filters"} tab={`Insights Filters (${dashboardFilterCount})`}>
                <DashboardFiltersWrapper
                  integrationIds={integrationIds}
                  filters={jiraOrFilters}
                  setJiraOrFilters={setJiraOrFilters}
                  orderedFilters={jiraOrOrderedFilters}
                  setJiraOrOrderedFilters={setJiraOrOrderedFilters}
                />
              </TabPane>
            )}
            <TabPane key={"widget_filters"} tab={`Widget Defaults (${widgetFilterCount})`}>
              <WidgetDefaultsWrapper
                integrationIds={integrationIds}
                filters={globalApplicationFilters}
                onRemoveReportFilters={handleRemoveReportFilters}
                onAddReportFilters={handleAddReportFilters}
                onFilterValueChange={onFilterChange}
                handlePartialValueChange={handlePartialFiltersChange}
                handleTimeRangeTypeChange={handleTimeRangeTypeChange}
                handleExcludeFilter={handleExcludeFilter}
                onFilterRemoved={onFilterRemoved}
                handleTimeRangeFilterValueChange={handleTimeRangeFilterValueChange}
                orderedFilters={globalApplicationOrderedFilters}
                setOrderedFilters={setGlobalOrderedFilters}
                partialFiltersErrors={partialFiltersErrors}
              />
            </TabPane>
            {props.showOUOverrides && (
              <TabPane key={"ou-user-filter-designation"} tab={"Collection Overrides"}>
                <OUUsersDefaultWrapper
                  availableApplications={availableApplications}
                  ouUserFilterDesignation={ouUserFilterDesignations}
                  handleOUUserFilterDesignationChange={handleSetOUUserFiltersDesignation}
                />
              </TabPane>
            )}
          </Tabs>
        </Col>
      </div>
    </Modal>
  );
};

export default DashboardApplicationFilters;
