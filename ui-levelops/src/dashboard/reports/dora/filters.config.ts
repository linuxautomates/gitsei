import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import genericApiFilterProps from "dashboard/report-filters/common/common-api-filter-props";
import {
  generateParentStoryPointsFilterConfig,
  generateStoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import { DoraAdvanceSettingsButtonConfig } from "dashboard/report-filters/dora/dora-advanced-setting-button";
import { ShowDoraGradingConfig } from "dashboard/report-filters/dora/show-dora-grading";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
import { azureCommonFiltersConfig } from "../azure/azure-specific-filter-config.constant";
import { JENKINS_CICD_FILTER_KEY_MAPPING, JENKINS_CICD_FILTER_LABEL_MAPPING } from "../jenkins/constants";
import { scmDoraSupportedFilters } from "./supported-filters.constants";
import { OrgUnitDataViewFilterConfig } from "dashboard/report-filters/dora/DoraOrgUnitFilterViewConfig.ts";
import { ProfileFilterViewConfig } from "dashboard/report-filters/dora/DoraProfileFilter.config";
import { DoraCalculationTimeConfig } from "dashboard/report-filters/dora/CalculationTimeFilter.config";
import { SCM_COMMON_FILTER_KEY_MAPPING } from "../scm/constant";
import {
  HARNESS_CICD_FILTER_LABEL_MAPPING,
  HARNESS_COMMON_FILTER_KEY_MAPPING,
  OUHarnessngJobsCommonFiltersConfig,
  ROLLBACK_KEY
} from "configurations/pages/Organization/Filters/harnessng/harnessng-job-filter.config";
import { jenkinsJobApiFilterProps } from "dashboard/report-filters/jenkins/jenkins-common-filter-props.config";
import { getCICDSuportedFilters } from "./helper";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { IntegrationTypes } from "constants/IntegrationTypes";

const commonDoraPRFilters = scmDoraSupportedFilters.map((filter: string) => ({
  key: filter,
  label: toTitleCase(filter.replace(/_/g, " "))
}));

const commonDoraCommitFilters = ["repo_id", "committer"].map((filter: string) => ({
  key: filter,
  label: toTitleCase(filter.replace(/_/g, " "))
}));

const scmFilterConfig = (uri: string) => (item: { key: string; label: string }) =>
  baseFilterConfig((SCM_COMMON_FILTER_KEY_MAPPING as any)[item.key], {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    deleteSupport: true,
    label: item.label,
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "title_case",
    supportPaginatedSelect: true,
    isParentTab: true,
    apiFilterProps: genericApiFilterProps,
    filterMetaData: {
      selectMode: "multiple",
      uri,
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: [item.key],
          filter: { integration_ids: get(args, "integrationIds", []) }
        };
      },
      specialKey: item.key,
      options: (args: any) => {
        const filterMetaData = get(args, ["filterMetaData"], {});
        const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
        const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === item.key);
        if (currData) {
          return (Object.values(currData)[0] as Array<any>)
            ?.map((item: any) => ({
              label: item.additional_key ?? item.key,
              value: item.key
            }))
            .filter((item: { label: string; value: string }) => !!item.value);
        }
        return [];
      },
      sortOptions: true,
      createOption: true
    } as ApiDropDownData
  });

const scmDoraPRFiltersConfig: LevelOpsFilter[] = [
  ...commonDoraPRFilters.map(scmFilterConfig("github_prs_filter_values"))
];

const scmDoraCommitFiltersConfig: LevelOpsFilter[] = [
  ...commonDoraCommitFilters.map(scmFilterConfig("github_commits_filter_values"))
];

const cicdCommonFiltersConfig = (
  filters: Array<{ key: string; label: string }>,
  keyMapping: basicMappingType<string>,
  lableMapping: basicMappingType<string>
): LevelOpsFilter[] =>
  filters.map((item: { key: string; label: string }) =>
    baseFilterConfig(keyMapping[item.key], {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: lableMapping[item.key] ?? item.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: true,
      excludeSupport: true,
      partialKey: item.key,
      supportPaginatedSelect: true,
      isParentTab: true,
      apiFilterProps: item.key === "job_normalized_full_name" ? genericApiFilterProps : jenkinsJobApiFilterProps,
      filterMetaData: {
        selectMode: item.key === ROLLBACK_KEY ? "default" : "multiple",
        uri: "jenkins_jobs_filter_values",
        method: "list",
        payload: (args: Record<string, any>) => {
          const additionalFilter = get(args, "additionalFilter", {});
          return {
            integration_ids: get(args, "integrationIds", []),
            fields: [item.key],
            filter: {
              integration_ids: get(args, "integrationIds", []),
              ...additionalFilter
            }
          };
        },
        specialKey: item.key,
        options: (args: any) => {
          const filterMetaData = get(args, ["filterMetaData"], {});
          const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
          const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === item.key);
          if (currData) {
            return (Object.values(currData)[0] as Array<any>)
              ?.map((item: any) => ({
                label: item.additional_key ?? item.key,
                value: item.key
              }))
              .filter((item: { label: string; value: string }) => !!item.value);
          }
          return [];
        },
        sortOptions: true,
        createOption: true
      } as ApiDropDownData
    })
  );

export const SCMPRFiltersConfig: LevelOpsFilter[] = [
  ShowDoraGradingConfig,
  OrgUnitDataViewFilterConfig,
  ProfileFilterViewConfig,
  DoraCalculationTimeConfig,
  DoraAdvanceSettingsButtonConfig,
  ...scmDoraPRFiltersConfig
];

export const SCMCommitFiltersConfig: LevelOpsFilter[] = [
  ShowDoraGradingConfig,
  OrgUnitDataViewFilterConfig,
  ProfileFilterViewConfig,
  DoraCalculationTimeConfig,
  DoraAdvanceSettingsButtonConfig,
  ...scmDoraCommitFiltersConfig
];

export const CICDHarnessFiltersConfig: LevelOpsFilter[] = [
  ShowDoraGradingConfig,
  OrgUnitDataViewFilterConfig,
  ProfileFilterViewConfig,
  DoraCalculationTimeConfig,
  DoraAdvanceSettingsButtonConfig,
  ...OUHarnessngJobsCommonFiltersConfig
];

export const IMJiraFiltersConfig: LevelOpsFilter[] = [
  ShowDoraGradingConfig,
  ...jiraCommonFiltersConfig,
  OrgUnitDataViewFilterConfig,
  ProfileFilterViewConfig,
  DoraCalculationTimeConfig,
  DoraAdvanceSettingsButtonConfig,
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig
];
export const IMAzureFiltersConfig: LevelOpsFilter[] = [
  ShowDoraGradingConfig,
  OrgUnitDataViewFilterConfig,
  ProfileFilterViewConfig,
  DoraCalculationTimeConfig,
  DoraAdvanceSettingsButtonConfig,
  ...azureCommonFiltersConfig,
  generateStoryPointsFilterConfig("workitem_story_points", "AZURE STORY POINTS"),
  generateParentStoryPointsFilterConfig("workitem_parent_story_points", "Workitem Parent Story Points")
];

export const getCICDFilterConfig = (supportedFilters: string[], application?: string): LevelOpsFilter[] => {
  const cicdSupportedFilters = supportedFilters.map((item: string) => ({
    label: item.replace(/_/g, " ")?.toUpperCase(),
    key: item
  }));

  const cicdFinalFilterData =
    application === IntegrationTypes.HARNESSNG
      ? cicdCommonFiltersConfig(
          cicdSupportedFilters,
          HARNESS_COMMON_FILTER_KEY_MAPPING,
          HARNESS_CICD_FILTER_LABEL_MAPPING
        )
      : cicdCommonFiltersConfig(
          cicdSupportedFilters,
          JENKINS_CICD_FILTER_KEY_MAPPING,
          JENKINS_CICD_FILTER_LABEL_MAPPING
        );
  return [
    ShowDoraGradingConfig,
    OrgUnitDataViewFilterConfig,
    ProfileFilterViewConfig,
    DoraCalculationTimeConfig,
    DoraAdvanceSettingsButtonConfig,
    ...cicdFinalFilterData
  ];
};

export const getFilters = (integrationType?: string, deployment_route?: string, application?: string) => {
  switch (integrationType) {
    case "SCM":
      return deployment_route === "commit" ? SCMCommitFiltersConfig : SCMPRFiltersConfig;
    case "CICD":
      return getCICDFilterConfig(getCICDSuportedFilters(application || ""), application);
    case "IM":
      if (application === IntegrationTypes.JIRA) {
        return IMJiraFiltersConfig;
      }
      if (application === IntegrationTypes.AZURE) {
        return IMAzureFiltersConfig;
      }
    default:
      return [
        OrgUnitDataViewFilterConfig,
        ProfileFilterViewConfig,
        DoraCalculationTimeConfig,
        DoraAdvanceSettingsButtonConfig
      ];
  }
};
