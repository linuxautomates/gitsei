import { get, upperCase } from "lodash";
import React from "react";
import { useState } from "react";
import { useMemo } from "react";
import { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { widgetFilterValuesGet } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { CONTAINS, STARTS_WITH, valuesToFilters } from "dashboard/constants/constants";
import { AntText } from "shared-resources/components";
import { Tag } from "antd";
import { hasValue } from "./helper";
import { defaultTimePeriodOptions } from "../../graph-filters/components/Constants";
import { toTitleCase } from "utils/stringUtils";
import { fileUpdateTimeStampOptions } from "dashboard/constants/FE-BASED/github-commit.FEbased";
import { getLtGtFieldValue } from "../widget-filter-preview/helper";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { DORA_REPORT_TO_KEY_MAPPING } from "dashboard/graph-filters/components/helper";
import {
  DORA_CALCULATION_FIELD_LABEL,
  getCalculationFieldLabel
} from "dashboard/graph-filters/components/GenericFilterComponents/DoraGenericFilter/constants";
import { DORA_REPORTS } from "dashboard/constants/applications/names";
import { convertToDate, DateFormats } from "utils/dateUtils";
import moment from "moment";

const dora_time_filters = Object.values(DORA_CALCULATION_FIELD_LABEL);
// Todo - the filter names should be unique coming from the backend.
const github_time_filters = [
  "pr_created_at",
  "pr_closed_at",
  "pr_merged_at",
  "committed_at",
  "ISSUE CREATED AT",
  "COMMITTED IN",
  "PR CREATED IN",
  "PR CLOSED TIME",
  "PR MERGED AT",
  "TIME",
  ...dora_time_filters
];

const github_lt_gt_filters = ["num_approvers", "num_reviewers"];

const partial_key_label_map = {
  github_prs_filter_values: {
    project: "PROJECT"
  },
  github_commits_filter_values: {
    project: "PROJECT NAME"
  }
};

const DIFFERENT_KEY_FILTER_MAPPING = {
  github_prs_filter_values: {
    labels: "SCM LABELS",
    projects: "PROJECTS",
    agg_type: "AGGREGATION TYPE"
  },
  github_commits_filter_values: {
    labels: "SCM LABELS",
    projects: "AZURE PROJECTS",
    agg_type: "AGGREGATION TYPE"
  }
};
interface GithubKeysFiltersProps {
  githubPrsFilter: any;
  githubCommitsFilter: any;
  integrationIds: any;
  acrossValue?: string;
  report?: string;
}

const GithubKeysFilters: React.FC<GithubKeysFiltersProps> = ({
  githubPrsFilter,
  githubCommitsFilter,
  integrationIds,
  acrossValue,
  report
}) => {
  const githubPrsFiltersKeys = useMemo(() => Object.keys(githubPrsFilter || {}), [githubPrsFilter]);

  const githubCommitsFilterKeys = useMemo(() => Object.keys(githubCommitsFilter || {}), [githubCommitsFilter]);

  const [prsValuesFilters, setprsValuesFilters] = useState<any>([]);
  const [commitsvaluesFilters, setCommitsvaluesFilters] = useState<any>([]);
  const [prsLoading, setPrsLoading] = useState<boolean>(false);
  const [commitsLoading, setcommitsLoading] = useState<boolean>(false);
  const githubPrsFiltersState = useParamSelector(getGenericRestAPISelector, {
    uri: "github_prs_filter_values",
    method: "list",
    uuid: "scm_prs"
  });

  const scmCommitsFiltersState = useParamSelector(getGenericRestAPISelector, {
    uri: "github_commits_filter_values",
    method: "list",
    uuid: "scm_commits"
  });
  const selectedOUState = useSelector(getSelectedOU);
  const workspaceProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU: selectedOUState?.id });

  const dispatch = useDispatch();
  useEffect(() => {
    const loading = get(githubPrsFiltersState, ["loading"], true);
    const error = get(githubPrsFiltersState, ["error"], true);
    if (!loading && !error) {
      const data = get(githubPrsFiltersState, ["data", "records"], []);
      setprsValuesFilters(data);
      setPrsLoading(false);
    }
  }, [githubPrsFiltersState]);

  useEffect(() => {
    const loading = get(scmCommitsFiltersState, ["loading"], true);
    const error = get(scmCommitsFiltersState, ["error"], true);
    if (!loading && !error) {
      const data = get(scmCommitsFiltersState, ["data", "records"], []);
      setCommitsvaluesFilters(data);
      setcommitsLoading(false);
    }
  }, [scmCommitsFiltersState]);

  useEffect(() => {
    const prsdata = get(githubPrsFiltersState, ["data", "records"], []);
    if (prsdata.length === 0 && !prsLoading) {
      if (
        githubPrsFiltersKeys.includes("creators") ||
        githubPrsFiltersKeys.includes("reviewers") ||
        githubPrsFiltersKeys.includes("assignees") ||
        (githubPrsFiltersKeys.includes("exclude") &&
          (githubPrsFilter["exclude"]["creators"] ||
            githubPrsFilter["exclude"]["reviewers"] ||
            githubPrsFilter["exclude"]["assignees"]))
      ) {
        dispatch(
          widgetFilterValuesGet(
            "github_prs_filter_values" || "",
            { fields: ["creator", "assignee", "reviewer"], integration_ids: integrationIds },
            null,
            "scm_prs"
          )
        );
        setPrsLoading(true);
      }
    }

    const commitsData = get(scmCommitsFiltersState, ["data", "records"], []);
    if (commitsData.length === 0 && !commitsLoading) {
      if (
        githubCommitsFilterKeys.includes("authors") ||
        githubCommitsFilterKeys.includes("committers") ||
        (githubCommitsFilterKeys.includes("exclude") &&
          (githubCommitsFilter["exclude"]["authors"] || githubCommitsFilter["exclude"]["committers"]))
      ) {
        dispatch(
          widgetFilterValuesGet(
            "github_commits_filter_values" || "",
            { fields: ["author", "committer"], integration_ids: integrationIds },
            null,
            "scm_commits"
          )
        );
        setcommitsLoading(true);
      }
    }
  }, [githubPrsFiltersKeys, githubCommitsFilterKeys, githubPrsFiltersState, scmCommitsFiltersState]);

  const excludeCaseHelper = (filterObject: any, categoryType: string) => {
    const final_filters: { key: string; label: string; value: any; exclude?: boolean; partial?: string }[] = [];
    Object.keys(filterObject || {})
      .filter(key => key !== "custom_fields")
      .forEach((exclude_filter_label: string) => {
        switch (exclude_filter_label) {
          case "creators":
          case "reviewers":
          case "assignees":
            if (prsValuesFilters.length > 0 && categoryType === "github_prs_filter_values") {
              const values: any = [];
              githubPrsFilter["exclude"][exclude_filter_label].forEach((key: any) => {
                const keyIndex = prsValuesFilters.findIndex((item: any) =>
                  Object.keys(item)
                    .map((_key: string) => (valuesToFilters as any)[_key])
                    .includes(exclude_filter_label)
                );
                if (keyIndex !== -1) {
                  const _key = Object.keys(prsValuesFilters[keyIndex])[0];
                  const list = prsValuesFilters[keyIndex][_key];
                  const data = list.find((options: any) => options.key === key);
                  values.push(data ? data["additional_key"] : key);
                }
              });
              if (hasValue(values)) {
                final_filters.push({
                  key: exclude_filter_label,
                  label: exclude_filter_label,
                  value: values,
                  exclude: true
                });
              }
            }
            break;
          case "authors":
          case "committers":
            if (commitsvaluesFilters.length > 0 && categoryType === "github_commits_filter_values") {
              const values: any = [];
              githubCommitsFilter["exclude"][exclude_filter_label].forEach((key: any) => {
                const keyIndex = commitsvaluesFilters.findIndex((item: any) =>
                  Object.keys(item)
                    .map((_key: string) => (valuesToFilters as any)[_key])
                    .includes(exclude_filter_label)
                );
                if (keyIndex !== -1) {
                  const _key = Object.keys(commitsvaluesFilters[keyIndex])[0];
                  const list = commitsvaluesFilters[keyIndex][_key];
                  const data = list.find((options: any) => options.key === key);
                  values.push(data ? data["additional_key"] : key);
                }
              });
              if (hasValue(values)) {
                final_filters.push({
                  key: exclude_filter_label,
                  label: exclude_filter_label,
                  value: values,
                  exclude: true
                });
              }
            }
            break;

          default:
            if (hasValue(filterObject[exclude_filter_label])) {
              final_filters.push({
                key: exclude_filter_label,
                label: get(DIFFERENT_KEY_FILTER_MAPPING, [categoryType, exclude_filter_label], exclude_filter_label),
                value: filterObject[exclude_filter_label],
                exclude: true
              });
            }
        }
      });
    return final_filters;
  };

  const scmrenderFilters = useMemo(() => {
    let finalFilters: any = [];

    githubPrsFiltersKeys.forEach((filter_label: any) => {
      switch (filter_label) {
        case "creators":
        case "reviewers":
        case "assignees":
          if (prsValuesFilters.length > 0) {
            const values: any = [];
            githubPrsFilter[filter_label].forEach((key: any) => {
              const keyIndex = prsValuesFilters.findIndex((item: any) =>
                Object.keys(item)
                  .map((_key: string) => (valuesToFilters as any)[_key])
                  .includes(filter_label)
              );
              if (keyIndex !== -1) {
                const _key = Object.keys(prsValuesFilters[keyIndex])[0];
                const list = prsValuesFilters[keyIndex][_key];
                const data = list.find((options: any) => options.key === key);
                values.push(data ? data["additional_key"] : key);
              }
            });
            if (hasValue(values)) {
              finalFilters.push({
                label: filter_label,
                value: values,
                partial: false
              });
            }
          }
          break;
        //Not present in the UI but sometimes backend send in the global filters
        case "story_points":
        case "parent_story_points":
          break;
        case "exclude":
          const excludefilters = excludeCaseHelper(githubPrsFilter[filter_label], "github_prs_filter_values");
          finalFilters = [...finalFilters, ...excludefilters];
          break;
        case "partial_match":
          const partialmatchKeys = Object.keys(githubPrsFilter[filter_label]);
          partialmatchKeys.forEach((keys: any) => {
            if (
              hasValue(githubPrsFilter[filter_label][keys][STARTS_WITH]) ||
              hasValue(githubPrsFilter[filter_label][keys][CONTAINS])
            ) {
              finalFilters.push({
                label: get(partial_key_label_map, ["github_prs_filter_values", keys], keys),
                value: [
                  githubPrsFilter[filter_label][keys][STARTS_WITH] || githubPrsFilter[filter_label][keys][CONTAINS]
                ],
                partial: githubPrsFilter[filter_label][keys][STARTS_WITH] ? "Start With" : "Contain"
              });
            }
          });
          break;

        case "pr_created_at":
          if (hasValue(githubPrsFilter[filter_label]["$gt"]) || hasValue(githubPrsFilter[filter_label]["$lt"])) {
            finalFilters.push({
              label: "PR CREATED IN",
              value: [githubPrsFilter[filter_label]["$gt"] || "NA", githubPrsFilter[filter_label]["$lt"] || "NA"],
              partial: false
            });
          }
          break;

        case "pr_merged_at":
          if (hasValue(githubPrsFilter[filter_label]["$gt"]) || hasValue(githubPrsFilter[filter_label]["$lt"])) {
            finalFilters.push({
              label: "PR MERGED AT",
              value: [githubPrsFilter[filter_label]["$gt"] || "NA", githubPrsFilter[filter_label]["$lt"] || "NA"],
              partial: false
            });
          }
          break;

        case "issue_created_at":
          if (hasValue(githubPrsFilter[filter_label]["$gt"]) || hasValue(githubPrsFilter[filter_label]["$lt"])) {
            finalFilters.push({
              label: "ISSUE CREATED AT",
              value: [githubPrsFilter[filter_label]["$gt"] || "NA", githubPrsFilter[filter_label]["$lt"] || "NA"],
              partial: false
            });
          }
          break;
        case "time_period":
          if (hasValue(githubPrsFilter[filter_label])) {
            finalFilters.push({
              label: get(DIFFERENT_KEY_FILTER_MAPPING, ["github_prs_filter_values", filter_label], filter_label),
              value: defaultTimePeriodOptions.find(option => option.value === githubPrsFilter[filter_label])?.label,
              partial: false
            });
          }
          break;
        case "pr_closed_at":
          if (hasValue(githubPrsFilter[filter_label]["$gt"]) || hasValue(githubPrsFilter[filter_label]["$lt"])) {
            finalFilters.push({
              label: "PR CLOSED TIME",
              value: [githubPrsFilter[filter_label]["$gt"] || "NA", githubPrsFilter[filter_label]["$lt"] || "NA"],
              partial: false
            });
          }
          break;
        case "pr_merged_at":
          if (hasValue(githubPrsFilter[filter_label]["$gt"]) || hasValue(githubPrsFilter[filter_label]["$lt"])) {
            finalFilters.push({
              label: "PR MERGED AT",
              value: [githubPrsFilter[filter_label]["$gt"] || "NA", githubPrsFilter[filter_label]["$lt"] || "NA"],
              partial: false
            });
          }
          break;
        case "time_range":
          if (
            hasValue(githubPrsFilter[filter_label]["$gt"]) ||
            (hasValue(githubPrsFilter[filter_label]["$lt"]) &&
              [DORA_REPORTS.CHANGE_FAILURE_RATE, DORA_REPORTS.DEPLOYMENT_FREQUENCY_REPORT].includes(report as any))
          ) {
            const label = getCalculationFieldLabel(workspaceProfile, DORA_REPORT_TO_KEY_MAPPING[report as string]);
            finalFilters.push({
              label: label,
              value: [githubPrsFilter[filter_label]["$gt"] || "NA", githubPrsFilter[filter_label]["$lt"] || "NA"],
              partial: false
            });
          }
          break;
        case "missing_fields":
          if (hasValue(githubPrsFilter[filter_label])) {
            const pr_merged = get(githubPrsFilter, [filter_label, "pr_merged"], false);
            finalFilters.push({
              key: filter_label,
              label: "PR Filter",
              value: pr_merged ? "PR CLOSED" : "PR MERGED"
            });
          }
          break;
        default:
          if (hasValue(githubPrsFilter[filter_label])) {
            finalFilters.push({
              label: get(DIFFERENT_KEY_FILTER_MAPPING, ["github_prs_filter_values", filter_label], filter_label),
              value: githubPrsFilter[filter_label],
              partial: false
            });
          }

          break;
      }
    });

    githubCommitsFilterKeys.forEach((filter_label: any) => {
      switch (filter_label) {
        case "partial_match":
          const partialmatchKeys = Object.keys(githubCommitsFilter["partial_match"]);
          partialmatchKeys.forEach((keys: any) => {
            if (
              hasValue(githubCommitsFilter[filter_label][keys][STARTS_WITH]) ||
              hasValue(githubCommitsFilter[filter_label][keys][CONTAINS])
            ) {
              finalFilters.push({
                label: get(partial_key_label_map, ["github_commits_filter_values", keys], keys),
                value: [
                  githubCommitsFilter[filter_label][keys][STARTS_WITH] ||
                    githubCommitsFilter[filter_label][keys][CONTAINS]
                ],
                partial: githubCommitsFilter[filter_label][keys][STARTS_WITH] ? "Start With" : "Contain"
              });
            }
          });
          break;
        case "authors":
        case "committers":
          if (commitsvaluesFilters.length > 0) {
            const values: any = [];
            githubCommitsFilter[filter_label].forEach((key: any) => {
              const keyIndex = commitsvaluesFilters.findIndex((item: any) =>
                Object.keys(item)
                  .map((_key: string) => (valuesToFilters as any)[_key])
                  .includes(filter_label)
              );
              if (keyIndex !== -1) {
                const _key = Object.keys(commitsvaluesFilters[keyIndex])[0];
                const list = commitsvaluesFilters[keyIndex][_key];
                const data = list.find((options: any) => options.key === key);
                values.push(data ? data["additional_key"] : key);
              }
            });
            if (hasValue(values)) {
              finalFilters.push({
                label: filter_label,
                value: values
              });
            }
          }
          break;

        case "committed_at":
          if (
            hasValue(githubCommitsFilter[filter_label]["$gt"]) ||
            hasValue(githubCommitsFilter[filter_label]["$lt"])
          ) {
            finalFilters.push({
              label: "COMMITTED IN",
              value: [
                githubCommitsFilter[filter_label]["$gt"] || "NA",
                githubCommitsFilter[filter_label]["$lt"] || "NA"
              ],
              partial: false
            });
          }

          break;
        case "time_range":
          if (
            hasValue(githubCommitsFilter[filter_label]["$gt"]) ||
            hasValue(githubCommitsFilter[filter_label]["$lt"])
          ) {
            finalFilters.push({
              label: "TIME",
              value: [
                githubCommitsFilter[filter_label]["$gt"] || "NA",
                githubCommitsFilter[filter_label]["$lt"] || "NA"
              ],
              partial: false
            });
          }
          break;
        case "days_count":
          const label = acrossValue === "author" ? "SHOW AUTHORS WITH " : "SHOW COMMITTERS WITH";
          if (
            hasValue(githubCommitsFilter[filter_label]["$gt"]) ||
            hasValue(githubCommitsFilter[filter_label]["$lt"])
          ) {
            finalFilters.push({
              label: label,
              value: getLtGtFieldValue(githubCommitsFilter[filter_label])
            });
          }
          break;
        //Not present in the UI but sometimes backend send in the global filters
        case "story_points":
        case "parent_story_points":
          break;
        case "exclude":
          const excludefilters = excludeCaseHelper(githubCommitsFilter[filter_label], "github_commits_filter_values");
          finalFilters = [...finalFilters, ...excludefilters];
          break;
        case "time_period":
          if (hasValue(githubCommitsFilter[filter_label])) {
            finalFilters.push({
              label: get(DIFFERENT_KEY_FILTER_MAPPING, ["github_commits_filter_values", filter_label], filter_label),
              value: defaultTimePeriodOptions.find(option => option.value === githubCommitsFilter[filter_label])?.label,
              partial: false
            });
          }
          break;
        case "visualization":
        case "code_change_size_unit":
          if (hasValue(githubCommitsFilter[filter_label])) {
            finalFilters.push({
              label: filter_label,
              value: toTitleCase(githubCommitsFilter[filter_label] || ""),
              partial: false
            });
          }
          break;
        case "legacy_update_interval_config":
          if (hasValue(githubCommitsFilter[filter_label])) {
            finalFilters.push({
              label: filter_label,
              value:
                fileUpdateTimeStampOptions.find(val => val.value === githubCommitsFilter[filter_label])?.label ??
                githubCommitsFilter[filter_label],
              partial: false
            });
          }
          break;
        case "agg_type":
          if (hasValue(githubCommitsFilter[filter_label])) {
            finalFilters.push({
              label: get(DIFFERENT_KEY_FILTER_MAPPING, ["github_commits_filter_values", filter_label], filter_label),
              value: toTitleCase(githubCommitsFilter[filter_label] || ""),
              partial: false
            });
          }
          break;
        default:
          if (hasValue(githubCommitsFilter[filter_label])) {
            finalFilters.push({
              label: get(DIFFERENT_KEY_FILTER_MAPPING, ["github_commits_filter_values", filter_label], filter_label),
              value: githubCommitsFilter[filter_label],
              partial: false
            });
          }
          break;
      }
    });

    return finalFilters;
  }, [commitsvaluesFilters, prsValuesFilters, githubPrsFilter, githubCommitsFilter]);

  return (
    <>
      {scmrenderFilters.length > 0 &&
        scmrenderFilters.map((item: any) => (
          <div className="global-filters-div-wrapper" key={item.label}>
            <AntText style={{ color: "#595959", fontSize: "10px", fontWeight: "bold" }}>
              {upperCase(item.label)}
            </AntText>
            {(item.exclude || item.partial) && (
              <AntText style={{ fontSize: "10px" }}>
                {item.exclude ? "Excludes" : `Includes all the values that: ${item.partial}`}
              </AntText>
            )}
            {github_lt_gt_filters.includes(item?.label) && (
              <div>
                <Tag key={item?.label} className="widget-filter_tags">
                  {getLtGtFieldValue(item?.value)}
                </Tag>
              </div>
            )}
            {![...github_time_filters, ...github_lt_gt_filters].includes(item.label) && (
              <div className="global-filters-div">
                {Array.isArray(item?.value) ? (
                  item?.value?.map((filter: any, index: number) => {
                    return <Tag key={`${filter}-${index}`}>{`${filter}`}</Tag>;
                  })
                ) : (
                  <Tag key={item?.value}>{`${item?.value}`}</Tag>
                )}
              </div>
            )}
             <div className="global-filters-div">
              {github_time_filters.includes(item?.label) && (
                <Tag key={item.label} className="widget-default-filters-list_tags">
                  {`${moment.unix(parseInt(item?.value[0])).utc().format(DateFormats.DAY)} `}-
                  {` ${moment.unix(parseInt(item?.value[1])).utc().format(DateFormats.DAY)}`}
                </Tag>
              )}
            </div>
          </div>
        ))}
    </>
  );
};

export default GithubKeysFilters;
