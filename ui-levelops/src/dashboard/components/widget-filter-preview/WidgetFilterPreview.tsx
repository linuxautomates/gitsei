import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Empty, Tag } from "antd";
import { upperCase, get, remove, isArray, keyBy, uniq } from "lodash";
import moment from "moment";
import { useDispatch, useSelector } from "react-redux";
import CustomFieldsApiComponent from "configurable-dashboard/components/configure-widget/configuration/tabs/CustomFieldsApiComponent";
import { AntText } from "shared-resources/components";
import {
  different_value_format_fields,
  time_Range_Filters_fields,
  lt_gt_format_fields,
  getJiraOrFiltersHelper,
  globalCustomFieldFiltersHelper
} from "../dashboard-header/helper";
import GithubKeysFilters from "../dashboard-header/GithubKeysFilterComponent";
import MicrosoftGlobalFilters from "../dashboard-header/MicrosoftFilterComponent";
import { AZURE_CUSTOM_FIELD_PREFIX, valuesToFilters } from "../../constants/constants";
import widgetConstants from "../../constants/widgetConstants";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { jiraCustomFieldsList } from "configurable-dashboard/helpers/helper";
import { genericList } from "reduxConfigs/actions/restapi";
import { getLtGtFieldValue, getWidgetFilters } from "./helper";
import GenericShowIdsLabelComponent from "../GenericShowIdsLabelMap/GenericShowIdsLabel.component";
import LevelopsFiltersPreview from "../dashboard-header/LevelopsFiltersPreview";
import { updateTimeFiltersValue } from "shared-resources/containers/widget-api-wrapper/helper";
import { getWidgetConstant } from "../../constants/widgetConstants";
import ApiFiltersPreview from "./ApiFiltersPreview";
import { BA_TIME_RANGE_FILTER_KEY } from "dashboard/constants/bussiness-alignment-applications/constants";
import { LEVELOPS_REPORTS } from "dashboard/reports/levelops/constant";
import LevelopsTableFiltersPreview from "../dashboard-header/LevelopsTableFiltersPreview";
import {
  GITHUB_PRS_COMMITTERS_IGNORE_KEYS,
  WIDGET_FILTER_PREVIEW_API_FILTERS_IGNORE_KEYS,
  WIDGET_FILTER_PREVIEW_NORMAL_FILTERS_IGNORE_KEYS
} from "./constant";
import { isSanitizedValue } from "utils/commonUtils";
import { WIDGET_FILTER_PREVIEW_TRANSFORMER } from "dashboard/constants/filter-key.mapping";
import {
  azureLeadTimeIssueReports,
  leadTimeReports,
  REPORT_FILTERS_CONFIG,
  scmDoraReports
} from "dashboard/constants/applications/names";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { FilterConfigBasedPreviewFilterConfigType } from "model/report/baseReport.constant";
import FilterConfigBasedFiltersPreviewContainer from "./FilterConfigBasedFiltersPreviewContainer";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { DateFormats } from "utils/dateUtils";

interface WidgetFilterPreviewProps {
  id: string;
  uri: string;
  filters: any;
  metaData: any;
  reportType: string;
  integrationIds: string[];
  dashboardMetaData?: any;
}

const GENERIC_IDS_LABEL_MAP_FILTER_KEYS = ["ticket_categorization_scheme", "velocity_config_id", "Resolve"];

const WidgetFilterPreview: React.FC<WidgetFilterPreviewProps> = (props: WidgetFilterPreviewProps) => {
  const { uri, filters, reportType, metaData, integrationIds, dashboardMetaData } = props;
  const uuid = integrationIds.sort().join("_");
  const dispatch = useDispatch();
  const selectedOUState = useSelector(getSelectedOU);
  const workspaceProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU: selectedOUState?.id });
  const integrationConfigListState = useParamSelector(getGenericUUIDSelector, {
    uri: "jira_integration_config",
    method: "list",
    uuid: uuid
  });
  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });
  const uniqApplicationName = uniq(integrations.map((integration: { application: string }) => integration.application));

  const [loading, setLoading] = useState(false);

  const application = useMemo(() => {
    let _application = get(widgetConstants, [reportType, "application"], []);
    if (_application === "any") {
      const getDoraProfileIntegrationApplication = get(
        widgetConstants,
        [reportType, "getDoraProfileIntegrationApplication"],
        undefined
      );
      if (getDoraProfileIntegrationApplication) {
        return getDoraProfileIntegrationApplication({
          workspaceOuProfilestate: workspaceProfile,
          reportType: reportType
        });
      }
    }
    return _application;
  }, [reportType]);

  const widgetFilters = useMemo(() => {
    const data = get(integrationConfigListState, ["data", "records"], []);
    let customFieldsList: any[] = jiraCustomFieldsList(data);
    return getWidgetFilters(filters[uri], metaData, reportType, customFieldsList);
  }, [filters, metaData, uri, reportType, integrationConfigListState, application]);
  const normalFilters = useMemo(() => {
    const apiBasedFilters = getWidgetConstant(reportType, "API_BASED_FILTER", []);
    const filterConfigBasedApiFilters: FilterConfigBasedPreviewFilterConfigType[] = getWidgetConstant(
      reportType,
      "filter_config_based_preview_filters",
      []
    );
    const filterConfigBasedApiFilterKeys = filterConfigBasedApiFilters.map(f => f.filter_key);
    let final_filters: { key: string; label: string; value: any; exclude?: boolean; partial?: string }[] = [
      ...(widgetFilters || [])
    ];

    const transformer = getWidgetConstant(reportType, WIDGET_FILTER_PREVIEW_TRANSFORMER);
    if (transformer) {
      final_filters = transformer(final_filters);
    }

    if (
      (([
        "github_prs_filter_values",
        "github_commits_filter_values",
        "microsoft_issues_filter_values",
        "metadata"
      ].includes(uri) &&
        !["jira", "azure_devops"].includes(application)) ||
        application === IntegrationTypes.LEVELOPS) &&
      !scmDoraReports.includes(reportType as any)
    ) {
      return { normal_filters: final_filters, api_filters: [] };
    }
    const normal_filters = Object.keys(filters[uri])
      .filter(
        (key: string) =>
          ![
            ...WIDGET_FILTER_PREVIEW_NORMAL_FILTERS_IGNORE_KEYS,
            ...apiBasedFilters,
            ...filterConfigBasedApiFilterKeys
          ].includes(key)
      )
      .reduce((acc: any, next: any) => ({ ...acc, [next]: filters?.[uri]?.[next] }), {});

    let final_api_based_filters = Object.keys(filters[uri])
      .filter(
        (key: string) =>
          ![...WIDGET_FILTER_PREVIEW_API_FILTERS_IGNORE_KEYS].includes(key) && apiBasedFilters.includes(key)
      )
      .reduce((acc: any, next: any) => ({ ...acc, [next]: filters?.[uri]?.[next] }), {});

    const finalFilterConfigBasedFilters = Object.keys(filters[uri])
      .filter(
        (key: string) =>
          ![...WIDGET_FILTER_PREVIEW_API_FILTERS_IGNORE_KEYS].includes(key) &&
          filterConfigBasedApiFilterKeys.includes(key)
      )
      .reduce((acc: any, next: any) => {
        const filtersConfigs: LevelOpsFilter[] = getWidgetConstant(reportType, REPORT_FILTERS_CONFIG, []);
        const currentFilterConfig = filtersConfigs?.find(config => config?.beKey === next);
        const filter: FilterConfigBasedPreviewFilterConfigType | undefined = filterConfigBasedApiFilters.find(
          f => f.filter_key === next
        );
        if (currentFilterConfig && filter) {
          return [
            ...acc,
            {
              key: next,
              label: currentFilterConfig.label,
              value: filters?.[uri]?.[next],
              filterMetaData: { ...currentFilterConfig.filterMetaData, integration_ids: integrationIds },
              valueKey: filter.valueKey,
              labelKey: filter.labelKey
            }
          ];
        }
        return acc;
      }, []);

    let final_normal_filter = { ...normal_filters };

    if (normal_filters.exclude) {
      final_normal_filter = {
        ...normal_filters,
        exclude: Object.keys(normal_filters?.exclude || {}).reduce((acc: any, next: any) => {
          if (!apiBasedFilters.includes(next)) {
            return {
              ...acc,
              [next]: normal_filters.exclude[next]
            };
          }
          return acc;
        }, {})
      };
      final_api_based_filters = {
        ...final_api_based_filters,
        exclude: Object.keys(normal_filters?.exclude || {}).reduce((acc: any, next: any) => {
          if (apiBasedFilters.includes(next)) {
            return {
              ...acc,
              [next]: normal_filters.exclude[next]
            };
          }
          return acc;
        }, {})
      };
    }
    const allTypesFilters = getJiraOrFiltersHelper(
      final_normal_filter,
      uri,
      reportType,
      metaData,
      dashboardMetaData,
      workspaceProfile,
      uniqApplicationName
    );
    final_filters = [...final_filters, ...allTypesFilters].filter((item: any) => {
      const _val = item?.value;
      if (typeof _val === "string") {
        return !!_val.length;
      }
      if (isArray(_val)) {
        return !!_val.filter((_fValue: any) => isSanitizedValue(_fValue)).length;
      }
      if (typeof _val === "object" && !isArray(_val)) {
        return !!Object.keys(_val || {}).length;
      }
      return isSanitizedValue(_val);
    });

    return {
      normal_filters: final_filters,
      api_filters: getJiraOrFiltersHelper(final_api_based_filters, uri, reportType),
      filter_config_based_api_filters: finalFilterConfigBasedFilters
    };
  }, [filters, metaData, uri, reportType, integrationConfigListState, dashboardMetaData]);

  const globalCustomFieldsFilters = useMemo(() => {
    return globalCustomFieldFiltersHelper(filters || {}, metaData, dashboardMetaData);
  }, [filters, metaData, dashboardMetaData]);

  const handleContainerClick = useCallback((e: any) => e.stopPropagation(), []);

  useEffect(() => {
    const widgetFilter = filters[uri];
    const across = get(filters[uri], "across", "");
    const hasCustomAcross = across.startsWith("customfield") || across.startsWith(AZURE_CUSTOM_FIELD_PREFIX);
    const hasCustomStacks =
      (widgetFilter?.custom_stacks || []).length ||
      !!(widgetFilter?.stacks || []).find(
        (item: string) => item?.startsWith("customfield") || across.startsWith(AZURE_CUSTOM_FIELD_PREFIX)
      );
    const hasCustomFields = Object.keys(widgetFilter || {}).includes("custom_fields");
    if (!hasCustomFields && (hasCustomAcross || hasCustomStacks)) {
      const data = get(integrationConfigListState, ["data"], {});
      if (!Object.keys(data).length && !loading) {
        dispatch(
          genericList(
            "jira_integration_config",
            "list",
            { filter: { integration_ids: integrationIds } } || {},
            null,
            uuid
          )
        );
        setLoading(true);
      }
    }
  }, [filters]);

  const removeUnwantedVelocityConfigId = (appliedFilters: any) => {
    if (![...leadTimeReports, ...azureLeadTimeIssueReports].includes(reportType as any)) {
      delete appliedFilters?.velocity_config_id;
    }
    return appliedFilters;
  };

  const renderFilters = useMemo(() => {
    let widgetFilters: any[] = [];
    const supportedFilters = get(widgetConstants, [reportType, "supported_filters"], []);
    let supportedFilterValues = [];
    if (Array.isArray(supportedFilters) && application !== "levelops") {
      supportedFilters.forEach(filter => {
        supportedFilterValues.push(...filter.values);
      });
    } else if (Array.isArray(supportedFilters) && application === IntegrationTypes.LEVELOPS) {
      supportedFilters.forEach(filter => {
        supportedFilterValues.push(filter.filterField);
      });
    } else {
      supportedFilterValues = get(supportedFilters, ["values"], []);
    }
    const updatedSupportedFilters = supportedFilterValues.map((item: any) => get(valuesToFilters, [item], item));
    if (normalFilters?.normal_filters && normalFilters?.normal_filters.length) {
      widgetFilters = (normalFilters?.normal_filters || []).map((filter: any, index: number) => (
        <div className="widget-filter" key={`${filter.key}-${index}`}>
          <AntText className={"widget-filter_label"}>
            {upperCase(filter.label)} {filter.missing_field && " (Missing Field)"}
          </AntText>
          {(filter.exclude || filter.partial) && (
            <>
              {filter.exclude && <AntText className={"widget-filter_extras"}>Excludes</AntText>}
              {filter.partial && (
                <AntText className={"widget-filter_extras"}>
                  {`Includes all the values that: ${filter.partial}`}
                </AntText>
              )}
            </>
          )}
          <div>
            {GENERIC_IDS_LABEL_MAP_FILTER_KEYS.includes(filter?.key || filter.label) && (
              <GenericShowIdsLabelComponent filterKey={filter?.key} {...filter} />
            )}
            {!different_value_format_fields.includes(filter?.key) &&
              filter.value &&
              Array.isArray(filter.value) &&
              filter?.value?.map((filter_val: any) => {
                return <Tag key={filter_val} className="widget-filter_tags">{`${filter_val}`}</Tag>;
              })}
            {!different_value_format_fields.includes(filter?.key) &&
              filter.value &&
              !Array.isArray(filter.value) &&
              !GENERIC_IDS_LABEL_MAP_FILTER_KEYS.includes(filter?.key) && (
                <Tag key={filter.value} className="widget-filter_tags">{`${filter.value}`}</Tag>
              )}
            {lt_gt_format_fields.includes(filter?.key) && (
              <>
                <Tag key={filter} className="widget-filter_tags">
                  {getLtGtFieldValue(filter?.value)}
                </Tag>
              </>
            )}
            {time_Range_Filters_fields.includes(filter?.key) && (
              <>
                {filter?.key === BA_TIME_RANGE_FILTER_KEY &&
                ["effort_investment_trend_report", "effort_investment_single_stat"].includes(reportType || "") ? (
                  <Tag key={filter.value} className="widget-filter_tags">{`${filter.value}`}</Tag>
                ) : (
                  <Tag key={filter} className="widget-filter_tags">
                    {`${moment.unix(parseInt(filter?.value[0])).utc().format(DateFormats.DAY)} `}-
                    {` ${moment.unix(parseInt(filter?.value[1])).utc().format(DateFormats.DAY)}`}
                  </Tag>
                )}
              </>
            )}
          </div>
        </div>
      ));
    }
    if (normalFilters.api_filters && normalFilters.api_filters.length) {
      widgetFilters.push(
        <ApiFiltersPreview
          filters={normalFilters.api_filters}
          uri={uri}
          reportType={reportType}
          integrationIds={integrationIds}
          widgetId={props.id}
        />
      );
    }

    if (normalFilters?.filter_config_based_api_filters && normalFilters?.filter_config_based_api_filters?.length) {
      widgetFilters.push(
        <FilterConfigBasedFiltersPreviewContainer filtersConfigs={normalFilters?.filter_config_based_api_filters} />
      );
    }

    if (
      Object.keys(globalCustomFieldsFilters || {}).length > 0 &&
      (Object.keys(globalCustomFieldsFilters?.exclude || []).length ||
        Object.keys(globalCustomFieldsFilters?.partial || []).length ||
        Object.keys(globalCustomFieldsFilters?.normalcustomfields || []).length ||
        Object.keys(globalCustomFieldsFilters?.missing_fields || []).length)
    ) {
      widgetFilters.push(
        <div style={{ marginTop: "16px" }} key="filter-preview-custom-fields">
          <CustomFieldsApiComponent
            integrationIds={integrationIds}
            dashboardCustomFields={globalCustomFieldsFilters}
            application={application}
          />
        </div>
      );
    }

    if (uri === "github_prs_filter_values" && !scmDoraReports.includes(reportType as any)) {
      let filter = Object.keys(removeUnwantedVelocityConfigId(filters[uri]) || {})
        .filter((key: string) => !GITHUB_PRS_COMMITTERS_IGNORE_KEYS.includes(key))
        .reduce((acc: any, next: any) => ({ ...acc, [next]: filters?.[uri]?.[next] }), {});
      filter = {
        ...filter,
        ...updateTimeFiltersValue(dashboardMetaData, metaData, { ...filter })
      };
      if (Object.keys(filter || {}).length) {
        widgetFilters.push(
          <div style={{ marginTop: "15px" }} key="filter-preview-github-prs-fields">
            <GithubKeysFilters
              githubPrsFilter={filter}
              githubCommitsFilter={undefined}
              integrationIds={integrationIds}
              acrossValue={filters?.github_commits_filter_values?.across}
              report={reportType}
            />
          </div>
        );
      }
    }

    if (uri === "github_commits_filter_values" && !["jira", "azure_devops"].includes(application)) {
      let filter = Object.keys(removeUnwantedVelocityConfigId(filters[uri]) || {})
        .filter((key: string) => !GITHUB_PRS_COMMITTERS_IGNORE_KEYS.includes(key))
        .reduce((acc: any, next: any) => ({ ...acc, [next]: filters?.[uri]?.[next] }), {});
      filter = {
        ...filter,
        ...updateTimeFiltersValue(dashboardMetaData, metaData, { ...filter })
      };
      if (Object.keys(filter || {}).length) {
        widgetFilters.push(
          <div style={{ marginTop: "15px" }} key="filter-preview-github-commits-fields">
            <GithubKeysFilters
              githubPrsFilter={undefined}
              githubCommitsFilter={filter}
              integrationIds={integrationIds}
              acrossValue={filters?.github_commits_filter_values?.across}
              report={reportType}
            />
          </div>
        );
      }
    }

    if (uri === "microsoft_issues_filter_values") {
      let filter = Object.keys(removeUnwantedVelocityConfigId(filters[uri]) || {}).reduce((acc: any, next: any) => {
        if (updatedSupportedFilters.includes(next)) {
          return {
            ...acc,
            [next]: get(filters, [uri, next], undefined)
          };
        }
        return acc;
      }, {});
      widgetFilters.push(
        <div style={{ marginTop: "15px" }} key="filter-preview-microsoft-fields">
          <MicrosoftGlobalFilters microSoftFilter={filter} integrationIds={integrationIds} />
        </div>
      );
    }

    if (application === IntegrationTypes.LEVELOPS) {
      let filter = Object.keys(removeUnwantedVelocityConfigId(filters[uri]) || {})
        .filter((key: string) => !GITHUB_PRS_COMMITTERS_IGNORE_KEYS.includes(key))
        .reduce((acc: any, next: any) => ({ ...acc, [next]: filters?.[uri]?.[next] }), {});
      widgetFilters.push(
        <div style={{ marginTop: "15px" }} key={`${uri}-filter-preview`}>
          <LevelopsFiltersPreview filters={filter} integrationIds={integrationIds} reportType={reportType} />
        </div>
      );
    }

    if ([LEVELOPS_REPORTS.TABLE_REPORT, LEVELOPS_REPORTS.TABLE_STAT_REPORT].includes(reportType as LEVELOPS_REPORTS)) {
      let filter = Object.keys(removeUnwantedVelocityConfigId(filters[uri]) || {})
        .filter((key: string) => !GITHUB_PRS_COMMITTERS_IGNORE_KEYS.includes(key))
        .reduce((acc: any, next: any) => ({ ...acc, [next]: filters?.[uri]?.[next] }), {});
      widgetFilters = [];
      filter = {
        ...filter,
        ...updateTimeFiltersValue(dashboardMetaData, metaData, { ...filter })
      };
      if (Object.keys(filter || {}).length) {
        widgetFilters.push(
          <div style={{ marginTop: "15px" }} key={`${uri}-filter-preview`}>
            <LevelopsTableFiltersPreview filters={filter} metaData={metaData} />
          </div>
        );
      }
    }

    if (!widgetFilters.length) {
      return <Empty className="filter-placeholder" description="No Filters" />;
    }

    const WidgetFilterPreviewComponent = get(widgetConstants, [reportType, "widget_filters_preview_component"]);
    if (!!WidgetFilterPreviewComponent) {
      let filter = Object.keys(filters[uri] ?? {})
        .filter((key: string) => !GITHUB_PRS_COMMITTERS_IGNORE_KEYS.includes(key))
        .reduce((acc: any, next: any) => ({ ...acc, [next]: filters?.[uri]?.[next] }), {});
      widgetFilters = [];
      filter = {
        ...filter,
        ...updateTimeFiltersValue(dashboardMetaData, metaData, { ...filter })
      };
      widgetFilters.push(
        React.createElement(WidgetFilterPreviewComponent, {
          filters: filter,
          metaData
        })
      );
    }

    return widgetFilters;
  }, [filters, integrationConfigListState]);

  return (
    <div onClick={handleContainerClick} className="widget-filters-list">
      {renderFilters}
    </div>
  );
};

export default React.memo(WidgetFilterPreview);
