import { Button, Empty, Icon, Popover } from "antd";
import {
  WIDGET_CONFIGURATION_KEYS,
  WIDGET_FILTER_TAB_ORDER,
  WIDGET_CONFIGURATION_PARENT_KEYS
} from "constants/widgets";
import {
  doraReports,
  HIDE_CUSTOM_FIELDS,
  LEAD_TIME_MTTR_REPORTS,
  MULTI_SERIES_REPORT_FILTERS_CONFIG,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import { AZURE_CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import widgetConstants, { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { IGNORE_FILTER_KEYS_CONFIG } from "dashboard/constants/filter-key.mapping";
import { ReportsApplicationType } from "dashboard/constants/helper";
import { ignoreFilterKeysType } from "dashboard/dashboard-types/common-types";
import { WidgetTabsContext } from "dashboard/pages/context";
import { SearchInput } from "dashboard/pages/dashboard-drill-down-preview/components/SearchInput.component";
import { generateSalesforceCustomFieldConfig } from "dashboard/report-filters/cross-aggregation/jira-salesforce/common-filters.config";
import { generateZendeskCustomFieldConfig } from "dashboard/report-filters/cross-aggregation/jira-zendesk/common-filters.config";
import { generateCustomFieldConfig } from "dashboard/report-filters/jira/common-filters.config";
import { generateMultiseriesCustomFieldConfig } from "dashboard/report-filters/multi-series-report/jira/common-filter-config";
import { generateAzureCustomFieldConfig } from "dashboard/reports/azure/azure-specific-filter-config.constant";
import { getFilterValue } from "helper/widgetFilter.helper";
import { difference, get, isEqual, lowerCase, map, max, sortBy, uniqBy } from "lodash";
import {
  EffortInvestmentProfileFilterData,
  LevelOpsFilter,
  StatTimeRangeFilterData
} from "model/filters/levelopsFilters";
import { Integration } from "model/entities/Integration";
import React, { useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { fieldListDataSelector } from "reduxConfigs/selectors/fields-list.selector";
import {
  selectedDashboardCustomEpicFields,
  selectedDashboardCustomFields,
  selectedDashboardCustomHygienes
} from "reduxConfigs/selectors/integration-config.selector";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { AntText, AntTooltip } from "shared-resources/components";
import { updateTimeFiltersValue } from "shared-resources/containers/widget-api-wrapper/helper";
import { isSanitizedValue } from "utils/commonUtils";
import { toTitleCase } from "utils/stringUtils";
import { getFilterById, getFiltersByTab } from "../FiltersContainer/helper";
import "./AddWidgetFilter.container.scss";
import SetDefaultEffortProfile from "./SetDefaultEffortProfile";
import { DORA_REPORT_TO_KEY_MAPPING } from "../helper";
import { ShowDoraGradingConfig } from "dashboard/report-filters/dora/show-dora-grading";
import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { removeCustomDateFields } from "./helper";
import { requiredOneFiltersKeys } from "dashboard/reports/jira/constant";
import { generateTestrailsCustomFieldConfig } from "dashboard/report-filters/testrails/common-filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { restapiClear } from "reduxConfigs/actions/restapi";

export interface AddWidgetFilterContainerProps {
  report: any;
  onDeleteReportFilters?: (report: any) => void;
  integrationIds: Array<any>;
  filters: any;
  metadata: any;
  dashboardMetaData?: any;
  hideHeader?: boolean;
  onWeightChange: (value: any, type: any) => void;
  widgetWeights: any;
  weightError: string;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  onExcludeFilterChange: (key: string, value: boolean) => void;
  handlePartialValueChange: (key: string, value: any) => void;
  onFilterRemoved: (key: string) => void;
  handleTimeRangeTypeChange: (key: string, value: any) => void;
  handleTimeRangeFilterValueChange: (value: any, type?: any, rangeType?: string, isCustom?: boolean) => void;
  dropdownButtonClass?: string;
  additionalId?: string;
  partialFiltersErrors: any;
  ignoreFilterKeys?: string[];
  handleMetadataChange?: (key: any, value: any) => void;
  handleLastSprintChange?: (value: boolean, filterKey: string) => void;
  handleBulkAddToMetadata?: (metadataForMerge?: any, filterPayload?: any) => void;
  onAggregationAcrossSelection?: (value: any) => void;
  onChildFilterRemove?: (filterpayload: any, returnUpdatedQuery?: boolean | undefined) => any;
  onModifiedFilterValueChange?: (payload: any) => void;
  handleSingleStatTypeFilterChange: (value: string, removeKey: string) => void;
  queryParamDashboardOUId?: any;
  isMultiTimeSeriesReport?: boolean;
  isParentTabData?: boolean;
  advancedTabState?: {
    value: boolean;
    callback: any;
  };
  currentFilterKeyValueChange?: string;
}

const AddWidgetFilterContainer: React.FC<AddWidgetFilterContainerProps> = props => {
  const {
    report,
    integrationIds,
    filters,
    dropdownButtonClass,
    isMultiTimeSeriesReport,
    isParentTabData,
    advancedTabState,
    currentFilterKeyValueChange,
    handleTimeRangeFilterValueChange
  } = props;
  const [showFiltersPopOver, setShowFiltersPopOver] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [dropDownOptions, setDropDownOptions] = useState<any[]>([]);
  const [orderedFilters, setOrderedFilters] = useState<any>([]);
  const [orderedFiltersChild, setorderedFiltersChild] = useState<any>([]);
  const [filterRefData, setFilterRefData] = useState<any>();
  const { isVisibleOnTab } = useContext(WidgetTabsContext);
  const [activePopKey, setActivePopKey] = useState<string | undefined>();

  const selectedOUState = useSelector(getSelectedOU);
  const dispatch = useDispatch();

  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });
  const workspaceProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU: selectedOUState?.id });
  const reportNameKey = DORA_REPORT_TO_KEY_MAPPING[report];

  const disabledReportsForDoraConfig: any = [];

  const disabledFieldsList = () => {
    if (reportNameKey === "change_failure_rate") {
      if (workspaceProfile?.[reportNameKey]?.is_absolute) {
        disabledReportsForDoraConfig.push(ShowDoraGradingConfig?.beKey);
      }
    }
    return disabledReportsForDoraConfig;
  };

  const integrationType = useMemo(() => {
    const getIntegrationType = get(widgetConstants, [report, "getDoraProfileIntegrationType"], undefined);
    if (getIntegrationType) {
      return getIntegrationType({ workspaceOuProfilestate: workspaceProfile });
    }
  }, [workspaceProfile]);

  const baseReportsFiltersConfig = useMemo(() => {
    const config = getWidgetConstant(report, REPORT_FILTERS_CONFIG);
    if (isMultiTimeSeriesReport) {
      return getWidgetConstant(report, MULTI_SERIES_REPORT_FILTERS_CONFIG);
    }
    if (typeof config === "function")
      return config({
        filters,
        integrationState: integrations,
        workspaceProfile: workspaceProfile
      });
    return config;
  }, [filters, report, isMultiTimeSeriesReport, integrations]);

  const parentTabFilterConfig = useMemo(() => {
    return (baseReportsFiltersConfig || [])?.filter((config: LevelOpsFilter) => config?.isParentTab);
  }, [baseReportsFiltersConfig]);

  const getApplication = (report: any) => {
    let _application = getWidgetConstant(report || "", "application");
    if (_application === "any") {
      const getReportApplication = get(widgetConstants, [report, "getDoraProfileIntegrationApplication"], undefined);
      if (getReportApplication) {
        _application = getReportApplication({ workspaceOuProfilestate: workspaceProfile, reportType: report });
      }
    }
    return _application;
  };

  const filtersConfig = getFiltersByTab(baseReportsFiltersConfig).filters;
  const application = getApplication(report);

  const handleVisibleChange = useCallback(visible => {
    setShowFiltersPopOver(visible);
    setSearchQuery("");
  }, []);

  const getIssueManagement = useCallback(
    (issueManagementValue: string) => {
      if ([IntegrationTypes.JIRAZENDESK, IntegrationTypes.JIRA_SALES_FORCE].includes(application)) {
        return [IntegrationTypes.JIRA, issueManagementValue];
      }
      return [issueManagementValue];
    },
    [application]
  );

  const ims = getFilterById(baseReportsFiltersConfig, "issue_management_system");

  const customSelectorArgs = useMemo(() => {
    const issueManagementValue = get(props.metadata, "default_value");
    const ignoreFilterKeysConfig: ignoreFilterKeysType = getWidgetConstant(report, IGNORE_FILTER_KEYS_CONFIG);
    const ignoreCustomFieldKeys = ignoreFilterKeysConfig?.ignoreCustomFilterKeys || [];
    const defaultParams = {
      ignore_custom_filter_keys: ignoreCustomFieldKeys
    };
    if (issueManagementValue)
      return { ...defaultParams, issue_management_system: getIssueManagement(issueManagementValue) };
    if (ims) return { ...defaultParams, issue_management_system: getIssueManagement(ims.defaultValue as string) };
    return { ...defaultParams, issue_management_system: getIssueManagement(application) };
  }, [props.metadata, report, ims, getIssueManagement, application]);

  const fieldList = useParamSelector(fieldListDataSelector, { application });

  const customFields = useParamSelector(selectedDashboardCustomFields, customSelectorArgs);
  const customHygienes = useParamSelector(selectedDashboardCustomHygienes, customSelectorArgs);
  const customEpics = useParamSelector(selectedDashboardCustomEpicFields, customSelectorArgs);

  const transformedCustomFields = useMemo(() => {
    if (
      doraReports.includes(report) &&
      (integrationType === WorkflowIntegrationType.CICD || integrationType === WorkflowIntegrationType.SCM)
    ) {
      return [];
    }
    if (application === IntegrationTypes.AZURE) {
      const azureCustomFields = (customFields ?? []).map((custom: { key: string }) => {
        const fieldListCustom: { key: string } = (fieldList ?? []).find((field: { key: string }) =>
          field?.key?.includes(custom?.key)
        );
        if (fieldListCustom) {
          const initialFieldKey = fieldListCustom.key?.replace(AZURE_CUSTOM_FIELD_PREFIX, "");
          if (initialFieldKey === custom.key) {
            return {
              ...(custom ?? {}),
              key: fieldListCustom?.key,
              metadata: {
                transformed: AZURE_CUSTOM_FIELD_PREFIX
              }
            };
          }
        }
        return custom;
      });
      if (doraReports.includes(report)) {
        return removeCustomDateFields(azureCustomFields, fieldList);
      }
      return azureCustomFields;
    } else if (application === IntegrationTypes.TESTRAILS) {
      const testrailsCustomFilds = (customFields ?? []).map((data: any) => {
        const customFiled = (fieldList ?? []).find((custom: { key: any }) => custom.key === data.key);
        if (customFiled) {
          data = {
            ...data,
            type: customFiled?.type
          };
        }
        return data;
      });
      return testrailsCustomFilds;
    }
    return doraReports.includes(report) ? removeCustomDateFields(customFields, fieldList) : customFields;
  }, [customFields, fieldList, application]);

  const isCustomSprint = useCallback(
    (key: string) => {
      return (
        (((transformedCustomFields || []).find((cF: any) => cF.key === key)?.name || "") as string).toLowerCase() ===
        "sprint"
      );
    },
    [transformedCustomFields]
  );
  const hideCustomField = useMemo(() => {
    const hideCustomFields = getWidgetConstant(report, HIDE_CUSTOM_FIELDS);
    if (hideCustomFields) {
      if (typeof hideCustomFields === "function") return hideCustomFields({ filters });
      return hideCustomFields;
    }
    return false;
  }, [report, filters]);

  const allFiltersConfig = useMemo(() => {
    let customFieldConfig: any = [];

    if (hideCustomField || transformedCustomFields?.length === 0) {
      return filtersConfig;
    }
    if (doraReports.includes(report) || LEAD_TIME_MTTR_REPORTS.includes(report)) {
      if (integrationType === WorkflowIntegrationType.CICD || integrationType === WorkflowIntegrationType.SCM) {
        return [...filtersConfig];
      }
      if (application === IntegrationTypes.JIRA) {
        customFieldConfig = generateCustomFieldConfig(transformedCustomFields, fieldList);
      }
      if (application === IntegrationTypes.AZURE) {
        customFieldConfig = generateAzureCustomFieldConfig(transformedCustomFields, fieldList);
      }
      return [...filtersConfig, ...customFieldConfig];
    }
    customFieldConfig =
      application === ReportsApplicationType.AZURE_DEVOPS
        ? generateAzureCustomFieldConfig(transformedCustomFields, fieldList)
        : application === ReportsApplicationType.JIRA_ZENDESK || application === ReportsApplicationType.ZENDESK
        ? generateZendeskCustomFieldConfig(transformedCustomFields, fieldList)
        : application === IntegrationTypes.JIRA_SALES_FORCE || application === ReportsApplicationType.SALESFORCE
        ? generateSalesforceCustomFieldConfig(transformedCustomFields, fieldList)
        : application === IntegrationTypes.TESTRAILS
        ? generateTestrailsCustomFieldConfig(transformedCustomFields, fieldList)
        : isMultiTimeSeriesReport
        ? generateMultiseriesCustomFieldConfig(transformedCustomFields, fieldList)
        : generateCustomFieldConfig(transformedCustomFields, fieldList);
    return [...filtersConfig, ...customFieldConfig];
  }, [transformedCustomFields, fieldList, hideCustomField, integrationType]);

  const filterLableCase = useMemo(() => {
    if (doraReports.includes(report)) {
      if (integrationType === WorkflowIntegrationType.CICD) {
        return "none";
      }
    }
  }, [integrationType]);

  useEffect(() => {
    const mappedFilters = {
      ...(filters || {}),
      ...updateTimeFiltersValue(props.dashboardMetaData, props.metadata, { ...(filters || {}) })
    };

    const mappedDropDownOptions = allFiltersConfig?.map((filterConfig: LevelOpsFilter) => {
      let getExcludeWithPartialMatchKey = getWidgetConstant(report, "getExcludeWithPartialMatchKey");
      let getExcludeWithPartialMatchKeyFlag =
        typeof getExcludeWithPartialMatchKey === "function" ? getExcludeWithPartialMatchKey(filterConfig.beKey) : false;
      let selected = isSanitizedValue(
        getFilterValue(
          mappedFilters,
          filterConfig.beKey,
          true,
          filterConfig?.parentKey ? [filterConfig.parentKey as string] : undefined,
          filterConfig.partialKey,
          filterConfig.excludeKey,
          getExcludeWithPartialMatchKeyFlag
        )?.value
      );
      let isFilterHidden = false;
      const isSelected = filterConfig.isSelected;
      if (isSelected) {
        if (typeof isSelected === "function") {
          selected = isSelected({ filters: mappedFilters, metadata: props.metadata });
        } else {
          selected = isSelected;
        }
      }
      // by default all the required filters should render
      // they don't have the delete option too
      if (filterConfig.required) {
        selected = true;
      }

      if (filterConfig.hideFilter) {
        if (typeof filterConfig.hideFilter === "function")
          isFilterHidden = filterConfig.hideFilter({ filters: mappedFilters });
        else isFilterHidden = filterConfig.hideFilter;
      }

      if (filterConfig.beKey === "stat_time_based") {
        const filterOptions = (filterConfig.filterMetaData as StatTimeRangeFilterData)?.options;
        if (filterOptions) {
          for (const op in filterOptions) {
            const val = (filterOptions as any[])[op as any]?.value;
            const valToCheck = val.includes("_at") ? val : `${val}_at`;
            if (isSanitizedValue(getFilterValue(mappedFilters, valToCheck))) {
              selected = true;
              break;
            }
          }
        }
      }

      let childFilterKeys;
      if (filterConfig?.childFilterKeys && typeof filterConfig.childFilterKeys === "function") {
        let childFilterKeysData = filterConfig.childFilterKeys(filterConfig.beKey);
        if (childFilterKeysData && childFilterKeysData.length > 0) {
          let childFilterKeysDataFinal = childFilterKeysData.map((childKey: string) => {
            let selectedChild = isSanitizedValue(getFilterValue(mappedFilters, childKey, true)?.value);
            return {
              selectedChild,
              key: childKey
            };
          });
          childFilterKeys =
            childFilterKeysDataFinal && childFilterKeysDataFinal.length > 0
              ? { childKeys: childFilterKeysDataFinal }
              : {};
        }
      }

      return {
        selected,
        isFilterHidden,
        label: filterConfig.label,
        key: filterConfig.beKey,
        ...childFilterKeys
      };
    });

    setDropDownOptions(mappedDropDownOptions);
    const filterOrderMap = props.metadata?.[WIDGET_FILTER_TAB_ORDER];
    let orderedChildKeys: any[] = [];
    let orderedKeys = mappedDropDownOptions
      .filter(item => item.selected)
      .map(item => {
        if (item?.childKeys && item?.childKeys.length > 0) {
          let childKeys = item.childKeys
            .filter((item: { selectedChild: boolean }) => item.selectedChild)
            .map((item: { key: string }) => item.key);
          orderedChildKeys = [...orderedChildKeys, ...childKeys];
        }
        return item.key;
      });

    if (orderedKeys.length) {
      if (Object.keys(filterOrderMap || {}).length === 0) {
        const newOrderMap = orderedKeys.reduce((acc: Record<string, number>, curr: string, currIndex: number) => {
          acc = { ...acc, [curr]: currIndex + 1 };
          return acc;
        }, {});
        props.handleBulkAddToMetadata?.({ [WIDGET_FILTER_TAB_ORDER]: newOrderMap });
      } else {
        const newOrderedKeys: string[] = [];
        sortBy(Object.values(filterOrderMap)).forEach((key: any) => {
          Object.keys(filterOrderMap).forEach((filterKey: string) => {
            if (orderedKeys.includes(filterKey) && filterOrderMap[filterKey] === key) {
              newOrderedKeys.push(filterKey);
            }
          });
        });
        const diffKeys = difference(orderedKeys, newOrderedKeys);
        let maxOrder = max(Object.values(filterOrderMap)) as number;
        const newOrderMap = diffKeys.reduce((acc: Record<string, number>, curr: string, currIndex: number) => {
          acc = { ...acc, [curr]: maxOrder + 1 };
          maxOrder += 1;
          return acc;
        }, {});
        props.handleBulkAddToMetadata?.({ [WIDGET_FILTER_TAB_ORDER]: { ...newOrderMap, ...filterOrderMap } });
        orderedKeys = [...newOrderedKeys, ...diffKeys];
      }
    }
    setOrderedFilters(orderedKeys);
    setorderedFiltersChild(orderedChildKeys);
  }, []);

  const memoizedStyle = useMemo(() => ({ width: "23.5rem" }), []);

  const removeHiddenFilters = (allFilters: any[]) => {
    const getFilterKeysToHide = getWidgetConstant(report, "getFilterKeysToHide");
    let filterKeysToHide: string[] = [];
    if (typeof getFilterKeysToHide === "function") {
      filterKeysToHide = getFilterKeysToHide({
        filters,
        integrationState: integrations,
        workspaceProfile: workspaceProfile,
        selectedOU: selectedOUState,
        allFilters: allFilters
      });
    }
    if (filterKeysToHide && Array.isArray(filterKeysToHide)) {
      return allFilters.filter((item: any) => !filterKeysToHide.includes(item.key));
    }
    return allFilters;
  };

  const getMappedDropdownOptions = useMemo(() => {
    let options: any[] = [...dropDownOptions];
    const mappedCustomFieldData = map(transformedCustomFields, custom => ({
      selected: false,
      label: custom.name,
      key: custom.key
    }));
    if (mappedCustomFieldData.length && !hideCustomField) {
      options = [...dropDownOptions, ...mappedCustomFieldData];
    }
    options = removeHiddenFilters(uniqBy(options, "key"));
    return options
      .sort((a: any, b: any) => {
        const aLabel = lowerCase(a.label);
        const bLabel = lowerCase(b.label);
        if (aLabel < bLabel) return -1;
        if (aLabel > bLabel) return 1;
        return 0;
      })
      .map((item: any) => ({ ...item, label: (toTitleCase(item.label) as string).toUpperCase() }));
  }, [transformedCustomFields, dropDownOptions, hideCustomField]);

  const updateFilterToOrderMap = (orderedFilters: string[], filterPayload?: any) => {
    const newOrderMap = orderedFilters.reduce((acc: Record<string, number>, curr: string, currIndex: number) => {
      acc = { ...acc, [curr]: currIndex + 1 };
      return acc;
    }, {});
    props.handleBulkAddToMetadata?.({ [WIDGET_FILTER_TAB_ORDER]: newOrderMap }, filterPayload);
  };

  const filterOptionSelected = (key: string) => {
    const index = getMappedDropdownOptions.findIndex(filter => filter.key === key);
    if (index !== -1) {
      let updatedOptions = [...getMappedDropdownOptions];
      updatedOptions[index].selected = true;
      let value = updatedOptions[index].defaultValue;
      if (requiredOneFiltersKeys.indexOf(key) !== -1) {
        const allConfig = getWidgetConstant(report, REPORT_FILTERS_CONFIG);
        const selectedConfig = allConfig.find((config: any) => config?.beKey === key);
        value = selectedConfig?.defaultValue?.absolute;
        handleTimeRangeFilterValueChange(key, selectedConfig?.defaultValue);
      }
      setDropDownOptions(updatedOptions);
      setShowFiltersPopOver(false);
      const newOrder = [...orderedFilters, key];
      updateFilterToOrderMap(newOrder, { value, type: updatedOptions[index].key });
      setOrderedFilters(newOrder);
      setSearchQuery("");
    }
  };

  const removeFilterOptionSelected = (key: any, parentKey?: string) => {
    let _key = key;
    if (typeof key === "object") {
      _key = Object.keys(key)[0];
    }

    if (_key.includes("customfield")) {
      _key = _key.split("@")[0];
    }
    const index = getMappedDropdownOptions.findIndex(filter => filter.key === _key);
    if (index !== -1) {

      const filterConfig = allFiltersConfig.find((filterItem: LevelOpsFilter) => filterItem.beKey === _key);
      let updatedOptions = [...getMappedDropdownOptions];
      updatedOptions[index].selected = false;
      const dashboardTimeKeys = Object.keys(
        updateTimeFiltersValue(props.dashboardMetaData, props.metadata, { ...(filters || {}) })
      );
      if (dashboardTimeKeys.includes(_key) || dashboardTimeKeys.includes("custom_fields")) {
        if (_key.includes("customfield")) {
          delete props.metadata?.["dashBoard_time_keys"]?.["custom_fields"]?.[_key];
        }
        delete props.metadata?.["dashBoard_time_keys"]?.[_key];
      }
      setDropDownOptions(updatedOptions);
      const newOrder = orderedFilters.filter((orderedKey: any) => orderedKey !== _key);
      const type = parentKey ? "childFilterRemove" : "remove";
      // REMOVE CHILD FILTER KEY IF PARENT IS REMOVED
      const childKeys =
        updatedOptions[index]?.childKeys && updatedOptions[index]?.childKeys.length > 0
          ? { childKeys: updatedOptions[index]?.childKeys?.map((item: { key: string }) => item.key) }
          : "";

      if (childKeys && childKeys?.childKeys) {
        let newChildKeys = orderedFiltersChild.filter((child: string) => !childKeys?.childKeys.includes(child));
        setorderedFiltersChild(newChildKeys);
      }

      updateFilterToOrderMap(newOrder, {
        value: _key,
        type: type,
        parentKey: parentKey,
        isCustomSprint: isCustomSprint(_key),
        ...childKeys
      });
      setOrderedFilters(newOrder);
      if(filterConfig){
        const filterConfigMetaData = get(filterConfig, ["filterMetaData"], {});
        const filterConfigUrl = get(filterConfigMetaData, ["uri"], '');
        const filterConfigMethod = get(filterConfigMetaData, ["method"], '');
        const filterConfigAdditionalFilter = get(filterConfigMetaData, ["additionalFilter"], {});
        const filterConfigSpecialKey = get(filterConfigMetaData, ["specialKey"], '');
        const filterConfigId = `${integrationIds.sort().join(",")}_${filterConfigUrl}_${filterConfigSpecialKey}_${JSON.stringify(filterConfigAdditionalFilter)}`;
        dispatch(restapiClear(filterConfigUrl, filterConfigMethod, filterConfigId));
      }
    } else {
      // REMOVED CHILD KEY TO STATE TO DELETE FROM LIST
      if (orderedFiltersChild && orderedFiltersChild.length > 0) {
        let newChildKeys = orderedFiltersChild.filter((childKey: any) => childKey !== _key);
        const newOrder = orderedFilters.filter((orderedKey: any) => orderedKey !== _key);
        updateFilterToOrderMap(newOrder, {
          value: _key,
          type: "remove",
          parentKey: "",
          isCustomSprint: isCustomSprint(_key)
        });
        setorderedFiltersChild(newChildKeys);
      }
    }
  };

  const dropdownOptionList = useMemo(
    () =>
      getMappedDropdownOptions.filter(filter => {
        return (
          !get(filter, ["isFilterHidden"], false) &&
          !filter.selected &&
          filter.label.toLowerCase().includes(searchQuery.toLowerCase())
        );
      }),
    [getMappedDropdownOptions, searchQuery]
  );

  const menu = () => {
    return (
      <div style={memoizedStyle}>
        <SearchInput value={searchQuery} onChange={(query: string) => setSearchQuery(query)} />
        <div className={`widget_defaults_reports_list ${dropdownButtonClass}`}>
          {dropdownOptionList.map(filter => (
            <div
              key={filter.key}
              className={"widget_defaults_reports_list_name"}
              onClick={() => filterOptionSelected(filter.key)}>
              <AntText className={"widget_defaults_reports_list_name_select"}>{filter.label}</AntText>
            </div>
          ))}
        </div>
      </div>
    );
  };

  const handleRemoveFilter = useCallback(
    (key: string, parentKey?: string) => {
      removeFilterOptionSelected(key, parentKey);
    },
    [removeFilterOptionSelected]
  );

  const handleAddOrderedFiltersChild = useCallback(
    (key: string) => {
      setorderedFiltersChild([...orderedFiltersChild, key]);
    },
    [setorderedFiltersChild, orderedFiltersChild]
  );

  const handleActivePopkey = (key: string | undefined) => setActivePopKey(key);
  // Filters Tab
  const renderFilters = () => {
    return orderedFilters.map((item: any) => {
      const filterConfig = allFiltersConfig.find((filterItem: LevelOpsFilter) => filterItem.beKey === item);
      if (filterConfig) {
        //FOR THE CHILD COMPONENT RENDER
        let childComponentArr: LevelOpsFilter[] = [];
        let childComponentFilter = {};

        if (
          filterConfig?.childFilterKeys &&
          typeof filterConfig.childFilterKeys === "function" &&
          filterConfig?.renderChildComponent &&
          typeof filterConfig?.renderChildComponent === "function"
        ) {
          let childFilterKeysData = filterConfig.childFilterKeys(filterConfig.beKey);
          if (childFilterKeysData && childFilterKeysData.length > 0) {
            childComponentArr = filterConfig?.renderChildComponent(childFilterKeysData);
            childComponentFilter = filters?.[filterConfig?.beKey]
              ? { [filterConfig?.beKey]: filters?.[filterConfig?.beKey] }
              : {};
          }
        }

        let getExcludeWithPartialMatchKey = getWidgetConstant(report, "getExcludeWithPartialMatchKey");
        let getExcludeWithPartialMatchKeyFlag =
          typeof getExcludeWithPartialMatchKey === "function"
            ? getExcludeWithPartialMatchKey(filterConfig.beKey)
            : false;

        return (
          <>
            {React.createElement(filterConfig.apiContainer ?? filterConfig.renderComponent, {
              filterProps: {
                ...filterConfig,
                allFilters: {
                  ...props.filters,
                  ...(props.metadata || {}),
                  reportApplication: application
                },
                metadata: props.metadata,
                filterMetaData: {
                  ...filterConfig?.filterMetaData,
                  dashboardMetaData: props.dashboardMetaData,
                  reportType: report,
                  integration_ids: integrationIds,
                  customFieldsRecords: transformedCustomFields,
                  customHygienes,
                  customEpics,
                  filters
                },
                labelCase: filterLableCase ?? filterConfig.labelCase,
                allowExcludeWithPartialMatch: getExcludeWithPartialMatchKeyFlag
              },
              handleRemoveFilter,
              activePopKey,
              handleActivePopkey,
              ...props
            })}

            {/* RENDER CHILD FILTER COMPONENT  */}
            {childComponentArr &&
              childComponentArr.length > 0 &&
              childComponentArr.map((childRender: any) => {
                if (orderedFiltersChild.includes(childRender.beKey)) {
                  return (
                    <div style={{ marginLeft: "30px" }}>
                      {React.createElement(childRender.apiContainer ?? childRender.renderComponent, {
                        filterProps: {
                          ...childRender,
                          allFilters: {
                            ...props.filters,
                            ...(props.metadata || {}),
                            reportApplication: application
                          },
                          metadata: props.metadata,
                          filterMetaData: {
                            ...childRender?.filterMetaData,
                            dashboardMetaData: props.dashboardMetaData,
                            reportType: report,
                            integration_ids: integrationIds,
                            customFieldsRecords: transformedCustomFields,
                            customHygienes,
                            customEpics,
                            childComponentFilter: childComponentFilter,
                            callApiOnParentValueChange: true
                          },
                          labelCase: filterLableCase ?? childRender.labelCase
                        },
                        handleRemoveFilter,
                        activePopKey,
                        handleActivePopkey,
                        ...props
                      })}
                    </div>
                  );
                } else {
                  //ADD BUTTON FOR RE ADD THE CHILD FILTER COMPONENT AFTER DELETED
                  return React.createElement(childRender.renderAddChildComponent, {
                    filterProps: {
                      ...childRender,
                      parentKey: filterConfig?.beKey,
                      parentKeyData: filters?.[filterConfig?.beKey]
                    },
                    handleAddOrderedFiltersChild
                  });
                }
              })}
          </>
        );
      }
      return null;
    });
  };

  const renderParentTabFilters = () => {
    return (parentTabFilterConfig || []).map((item: LevelOpsFilter) => {
      if (isVisibleOnTab(item.tab as WIDGET_CONFIGURATION_PARENT_KEYS, item?.isParentTab)) {
        return React.createElement(item.apiContainer ?? item.renderComponent, {
          filterProps: {
            ...item,
            allFilters: { ...props.filters, ...(props.metadata || {}), reportApplication: application },
            metadata: props.metadata,
            filterMetaData: {
              ...item?.filterMetaData,
              dashboardMetaData: props.dashboardMetaData,
              reportType: report,
              integration_ids: integrationIds,
              customFieldsRecords: transformedCustomFields,
              customHygienes,
              customEpics
            },
            disabled: disabledFieldsList().includes(item?.beKey)
          },
          activePopKey,
          handleActivePopkey,
          metaData: props.metadata,
          ...props
        });
      }
      return null;
    });
  };
  // All the Other Tabs
  const renderOtherFilters = () => {
    return baseReportsFiltersConfig.map((item: LevelOpsFilter) => {
      if (isVisibleOnTab(item.tab as WIDGET_CONFIGURATION_KEYS)) {
        return React.createElement(item.apiContainer ?? item.renderComponent, {
          filterProps: {
            ...item,
            allFilters: { ...props.filters, ...(props.metadata || {}), reportApplication: application },
            filterMetaData: {
              ...item?.filterMetaData,
              dashboardMetaData: props.dashboardMetaData,
              reportType: report,
              integration_ids: integrationIds,
              customFieldsRecords: transformedCustomFields,
              customHygienes,
              customEpics
            }
          },
          activePopKey,
          handleActivePopkey,
          metaData: props.metadata,
          customEpics,
          ...props
        });
      }
      return null;
    });
  };

  const isVisibleOnOtherTabs = useMemo(() => {
    return (
      isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.AGGREGATIONS) ||
      isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) ||
      isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) ||
      isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.WEIGHTS) ||
      isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.OTHERS)
    );
  }, [isVisibleOnTab]);

  const effortInvestmentProfileConfig = useMemo(
    () => baseReportsFiltersConfig.find((config: LevelOpsFilter) => config.id === "effort_investment_profile"),
    [baseReportsFiltersConfig]
  );

  const hasEffortInvestmentProfile = useMemo(() => {
    if (effortInvestmentProfileConfig) {
      return (effortInvestmentProfileConfig.filterMetaData as EffortInvestmentProfileFilterData).showDefaultScheme;
    }
    return false;
  }, [effortInvestmentProfileConfig]);

  const renderAllFilters = isParentTabData
    ? renderParentTabFilters()
    : isVisibleOnOtherTabs
    ? renderOtherFilters()
    : renderFilters();

  useEffect(() => {
    if (
      currentFilterKeyValueChange &&
      orderedFiltersChild &&
      orderedFiltersChild.length > 0 &&
      orderedFilters.includes(currentFilterKeyValueChange)
    ) {
      let getChildFilter =
        dropDownOptions
          .filter(data => data.key === currentFilterKeyValueChange && data.childKeys)
          .map(item => item.childKeys || []) || [];
      if (getChildFilter && getChildFilter.length > 0) {
        let newFilterData = filters[currentFilterKeyValueChange];
        let oldFilterData = filterRefData[currentFilterKeyValueChange];

        getChildFilter[0].map((data: any) => {
          if (!isEqual(newFilterData, oldFilterData) && orderedFiltersChild.includes(data.key)) {
            handleRemoveFilter(data.key);
          }
        });
      }
    }
    setFilterRefData(filters);
  }, [filters, currentFilterKeyValueChange]);

  return (
    <div className={"add-widget-filter-container"}>
      {!isVisibleOnOtherTabs && !isParentTabData && (
        <div className={"my-10"}>
          <span className={"report_category_dropdown"}>
            <Popover
              className={"search-popover"}
              placement={"bottomLeft"}
              content={menu()}
              trigger="click"
              visible={showFiltersPopOver}
              onVisibleChange={handleVisibleChange}>
              <AntTooltip title={(dropdownOptionList || []).length == 0 ? "No Filter to add." : null}>
                <Button
                  disabled={(dropdownOptionList || []).length == 0}
                  className={"mb-10 add-filter-btn"}
                  onClick={() => setShowFiltersPopOver(!showFiltersPopOver)}>
                  Add Filter <Icon type="down" />
                </Button>
              </AntTooltip>
            </Popover>
          </span>
        </div>
      )}
      {hasEffortInvestmentProfile && (
        <SetDefaultEffortProfile
          filters={filters}
          onFilterValueChange={props.onFilterValueChange}
          report={report}
          filterConfig={effortInvestmentProfileConfig!}
        />
      )}
      <div className={"add-widget-filter-container-list"}>
        {renderAllFilters}
        {Object.keys(filters).length === 0 && <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />}
      </div>
    </div>
  );
};

export default React.memo(AddWidgetFilterContainer);
