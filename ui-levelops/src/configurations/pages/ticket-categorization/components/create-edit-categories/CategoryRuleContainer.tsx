import React, { useCallback, useState, useEffect, useMemo } from "react";
import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";
import { buildExcludeQuery, buildWidgetQuery } from "configurable-dashboard/helpers/queryHelper";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { getOrderedFilterKeys } from "dashboard/components/dashboard-application-filters/AddFiltersComponent/helpers";
import { AZURE_CUSTOM_FIELD_PREFIX, valuesToFilters } from "dashboard/constants/constants";
import { issueManagementSupportedFilters, jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { cloneDeep, forEach, get, set, unset } from "lodash";
import { useParams } from "react-router-dom";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  ticketCategorizationSchemesRestGetSelector,
  ticketCategorizationSchemesRestCreateSelector
} from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { sanitizeFilterObject } from "utils/filtersUtils";
import {
  IssueManagementType,
  NEW_SCHEME_ID,
  ticketCategorizationFiltersIgnoreKeys,
  WORKITEM_ATTRIBUTE_KEYS
} from "../../constants/ticket-categorization.constants";
import { DashboardFiltersReportType } from "../../types/ticketCategorization.types";
import "./CategoryRuleContainer.styles.scss";
import { RANGE_FILTER_CHOICE } from "dashboard/constants/filter-key.mapping";
import { useDispatch } from "react-redux";
import { integrationsList, restapiClear } from "reduxConfigs/actions/restapi";
import { Integration } from "model/entities/Integration";
import AddCategoryFiltersContainer from "../../filters/AddCategoryFiltersContainer";
import { EIJiraFiltersConfig } from "../../filters/jira/jira-filter-config";
import { EIAzureCommonFiltersConfig } from "../../filters/azure/azure-filter-config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { buildPartialQuery } from "configurable-dashboard/helpers/queryHelper";
interface CategoryRuleContainerProps {
  category: RestTicketCategorizationCategory;
  handleUpdate: (updatedCategory: any) => void;
}

const JIRA_PLACEHOLDER_REPORT = "bounce_report";
const AZURE_PLACEHOLDER_REPORT = "azure_tickets_counts_stat";
const INTEGRAION_LIST_ID = "effort_investment_integration_list";

const jiraReportObj: DashboardFiltersReportType = {
  application: IntegrationTypes.JIRA,
  filter: jiraSupportedFilters,
  name: "Jira Filters",
  report: JIRA_PLACEHOLDER_REPORT,
  uri: jiraSupportedFilters.uri
};

const azureReportObj: DashboardFiltersReportType = {
  application: IntegrationTypes.AZURE,
  filter: issueManagementSupportedFilters,
  name: "Azure Filters",
  report: AZURE_PLACEHOLDER_REPORT,
  uri: issueManagementSupportedFilters.uri
};

const issueManagementToFilterConfigMapping: Record<IssueManagementType, LevelOpsFilter[]> = {
  jira: EIJiraFiltersConfig,
  azure_devops: EIAzureCommonFiltersConfig
};
const CategoryRuleContainer: React.FC<CategoryRuleContainerProps> = ({ category, handleUpdate }) => {
  const [partialFilterError, setPartialFilterError] = useState<any>({});
  const [orderedFilters, setOrderedFilters] = useState<string[]>([]);
  const dispatch = useDispatch();
  const params = useParams();
  const profileId = (params as any).id;
  const profileSelector =
    profileId === NEW_SCHEME_ID
      ? ticketCategorizationSchemesRestCreateSelector
      : ticketCategorizationSchemesRestGetSelector;
  const profile: RestTicketCategorizationScheme = useParamSelector(profileSelector, {
    scheme_id: profileId
  });
  const issueManagementIntegration = profile?.issue_management_integration;
  const reportType = issueManagementIntegration === "jira" ? jiraReportObj : azureReportObj;

  const integrationState = useParamSelector(getGenericUUIDSelector, {
    uri: "integrations",
    method: "list",
    uuid: INTEGRAION_LIST_ID
  });

  const integrationIds = useMemo(() => {
    const integrationRecords: Integration[] = get(integrationState, ["data", "records"], []);
    return integrationRecords.map(int => int.id);
  }, [integrationState]) as string[];

  useEffect(() => {
    if ((category?.filter || {})?.hasOwnProperty("workitem_custom_fields")) {
      const customFieldKeys = Object.keys((category?.filter || {}).workitem_custom_fields);
      let newFilters = { ...category?.filter };
      forEach(customFieldKeys, key => {
        if (!key.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
          newFilters = {
            ...(newFilters ?? {}),
            workitem_custom_fields: {
              ...get(newFilters, ["workitem_custom_fields"]),
              [`${AZURE_CUSTOM_FIELD_PREFIX}${key}`]: get(category?.filter, ["workitem_custom_fields", key])
            }
          };
          unset(newFilters, ["workitem_custom_fields", key]);
        } else {
          newFilters = {
            ...(newFilters ?? {}),
            workitem_custom_fields: {
              ...get(newFilters, ["workitem_custom_fields"]),
              [key]: get(category?.filter, ["workitem_custom_fields", key])
            }
          };
        }
      });
      category.filter = newFilters;
      handleUpdate(category?.json);
    }
  }, []);

  useEffect(() => {
    if (!!Object.keys(category?.filter || {}).length) {
      setOrderedFilters(
        getOrderedFilterKeys(
          category?.filter ?? {},
          issueManagementIntegration === "jira" ? jiraSupportedFilters.values : issueManagementSupportedFilters.values
        )
      );
    }

    dispatch(integrationsList({ filter: { applications: [issueManagementIntegration] } }, null, INTEGRAION_LIST_ID));

    return () => {
      dispatch(restapiClear("integrations", "list", INTEGRAION_LIST_ID));
    };
  }, [issueManagementIntegration]);

  const handleSetGlobalOrderedFilters = useCallback(
    (keys: string[]) => {
      setOrderedFilters([...keys]);
    },
    [issueManagementIntegration]
  );

  const handleFilterSelectChange = useCallback(
    (value: any, type: any, exclude?: boolean) => {
      let newFilters = cloneDeep(category?.filter);
      newFilters = buildWidgetQuery(
        category?.filter || {},
        value,
        type,
        exclude,
        issueManagementIntegration !== "jira" ? "workitem_custom_fields" : "custom_fields"
      );
      let codeAreasFilter = get(newFilters, ["workitem_attributes", "code_area"], []);
      if (codeAreasFilter.length) {
        codeAreasFilter = codeAreasFilter.map((filter: any) => {
          if (typeof filter === "string") return filter;
          return filter?.child ?? "";
        });
        set(newFilters, ["workitem_attributes", "code_area"], codeAreasFilter);
      }
      category.filter = newFilters;
      handleUpdate(category?.json);
      setPartialFilterError((prev: any) => ({ ...prev, [type]: undefined }));
    },
    [category, issueManagementIntegration]
  );

  const handleExcludeFilter = (key: string, value: boolean) => {
    let newFilters = buildExcludeQuery(
      cloneDeep(category?.filter || {}),
      key,
      value,
      issueManagementIntegration !== "jira" ? "workitem_custom_fields" : "custom_fields"
    );
    category.filter = newFilters;
    handleUpdate(category?.json);
    setPartialFilterError((prev: any) => ({ ...prev, [key]: undefined }));
  };

  const handlePartialFilters = (key: string, value: any) => {
    let { filters, error } = buildPartialQuery(
      category?.filter || {},
      key,
      value,
      issueManagementIntegration === "jira" ? JIRA_PLACEHOLDER_REPORT : AZURE_PLACEHOLDER_REPORT,
      issueManagementIntegration !== "jira" ? "workitem_custom_fields" : "custom_fields"
    );
    if (!!error) {
      setPartialFilterError((prev: any) => ({ ...prev, [key]: error }));
    } else {
      category.filter = filters;
      handleUpdate(category?.json);
      setPartialFilterError((prev: any) => ({ ...prev, [key]: undefined }));
    }
  };

  const handleCategoryFiltersRemoved = (filterKey: string) => {
    let categoryFilters = cloneDeep(category?.filter);
    let categoryMetadata = cloneDeep(category?.metadata);
    let _partialMatchKey = filterKey;
    forEach(Object.keys(valuesToFilters ?? {}), key => {
      const filterValue = get(valuesToFilters, [key], "");
      if (filterValue === filterKey) {
        _partialMatchKey = key;
      }
    });

    if (WORKITEM_ATTRIBUTE_KEYS.includes(filterKey)) {
      unset(categoryFilters, ["exclude", "workitem_attributes", filterKey]);
      unset(categoryFilters, ["workitem_attributes", filterKey]);
    }
    // handling custom field delete
    const customFieldPrefix =
      issueManagementIntegration === IntegrationTypes.JIRA ? "custom_fields" : "workitem_custom_fields";

    if (get(categoryFilters, ["exclude"], {}).hasOwnProperty(customFieldPrefix)) {
      unset(categoryFilters, ["exclude", customFieldPrefix, filterKey]);
      const excludeCustomFilterLeft = get(categoryFilters, ["exclude", customFieldPrefix], {});
      if (!Object.keys(excludeCustomFilterLeft).length) {
        unset(categoryFilters, ["exclude", customFieldPrefix]);
      }
    }

    unset(categoryMetadata, [RANGE_FILTER_CHOICE, filterKey]);
    unset(categoryFilters, [customFieldPrefix, filterKey]);
    unset(categoryFilters, ["exclude", filterKey]);
    unset(categoryFilters, ["partial_match", _partialMatchKey]);
    unset(categoryFilters, [filterKey]);
    category.metadata = categoryMetadata;
    categoryFilters = sanitizeFilterObject(categoryFilters);
    category.filter = categoryFilters;
    handleUpdate(category?.json);
  };

  const handleTimeRangeTypeChange = (key: string, value: string) => {
    const updatedMetaData = {
      ...(category.metadata || {}),
      range_filter_choice: {
        ...(category?.metadata?.range_filter_choice || {}),
        [key]: value
      }
    };

    category.metadata = updatedMetaData;
    handleUpdate(category?.json);
  };

  const handleTimeRangeFilterValueChange = (
    value: any,
    type: string,
    rangeType?: string | undefined,
    isCustom = false
  ) => {
    const updatedMetadata = {
      ...category.metadata,
      range_filter_choice: {
        ...(category?.metadata?.range_filter_choice || {}),
        [type]: { type: value.type, relative: value.relative }
      }
    };
    category.metadata = updatedMetadata;
    const customKey = issueManagementIntegration !== "jira" ? "workitem_custom_fields" : "custom_fields";
    if (!isCustom) {
      const updatedFilters = {
        ...(category.filter || {}),
        [type]: value.absolute
      };
      category.filter = updatedFilters;
    } else {
      const updatedFilters = {
        ...(category.filter || {}),
        [customKey]: {
          ...(category?.filter?.[customKey] || {}),
          [type]: value.absolute
        }
      };
      category.filter = updatedFilters;
    }
    handleUpdate(category?.json);
  };

  const modifiedFilters = useMemo(() => {
    let newFilters = cloneDeep(category?.filter);
    let codeAreasFilter = get(newFilters, ["workitem_attributes", "code_area"], []);
    if (codeAreasFilter.length) {
      codeAreasFilter = codeAreasFilter.map((filter: any) => ({ child: filter }));
      set(newFilters, ["workitem_attributes", "code_area"], codeAreasFilter);
    }
    return newFilters;
  }, [category?.filter]);

  return (
    <div className="ticket-categorization-filters-container">
      <AddCategoryFiltersContainer
        integrationIds={integrationIds}
        application={issueManagementIntegration as IssueManagementType}
        filtersConfigs={issueManagementToFilterConfigMapping[issueManagementIntegration as IssueManagementType]}
        key={jiraSupportedFilters.uri}
        filters={modifiedFilters}
        onExcludeFilterChange={handleExcludeFilter}
        onFilterValueChange={handleFilterSelectChange}
        handlePartialValueChange={handlePartialFilters}
        handleTimeRangeTypeChange={handleTimeRangeTypeChange}
        onFilterRemoved={handleCategoryFiltersRemoved}
        handleTimeRangeFilterValueChange={handleTimeRangeFilterValueChange}
        orderedFilters={orderedFilters}
        setOrderedFilters={handleSetGlobalOrderedFilters}
        partialFiltersErrors={partialFilterError}
        ignoreFilterKeys={ticketCategorizationFiltersIgnoreKeys}
        metadata={category.metadata}
      />
    </div>
  );
};

export default CategoryRuleContainer;
