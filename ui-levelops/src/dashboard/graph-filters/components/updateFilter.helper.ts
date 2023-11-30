import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { cloneDeep, get } from "lodash";
import { getValueFromTimeRange, rangeMap, timeBoundFilterKeys } from "./helper";
import { TIME_FILTER_RANGE_CHOICE_MAPPER } from "dashboard/constants/applications/names";
import * as AppNames from "dashboard/constants/applications/names";

const defaultTimeRange = {
  type: "absolute",
  relative: {
    next: {
      unit: "days"
    },
    last: {
      unit: "days"
    }
  }
};

const getDaysCount = (value: { $gt: string; $lt: string }) => {
  const diff = parseInt(value.$lt) - parseInt(value.$gt);
  return Math.round(diff / 86400);
};

export const updateIssueCreatedAndUpdatedFilters = (
  _finalFilters: any,
  metadata: any,
  widgetType: string = "",
  uri: string = ""
) => {
  const finalFilters = cloneDeep(_finalFilters);
  // dont do any filters conversion for levelops widgets
  if (widgetType && widgetType.includes("levelops")) {
    return finalFilters;
  }

  // Don't transform this widget's filters.
  if (widgetType === AppNames.MICROSOFT_ISSUES_REPORT_NAME) {
    return finalFilters;
  }

  const filterKeys = Object.keys(finalFilters?.filter || {});
  let filters = finalFilters;
  const rangeFilterChoice = get(metadata, ["range_filter_choice"], {});

  // TODO: maybe we can remove this now
  // hack for partial filters here, send out plural for labels
  // remove this part when BE is fixed
  const pluralCases = {
    label: "labels",
    component: "components",
    version: "versions",
    fix_version: "fix_versions"
  };
  Object.keys(get(filters, ["filter", "partial_match"], {})).forEach((partialMatch: string) => {
    if (Object.keys(pluralCases).includes(partialMatch)) {
      const value = filters.filter.partial_match[partialMatch];
      delete filters.filter.partial_match[partialMatch];
      // @ts-ignore
      filters.filter.partial_match[pluralCases[partialMatch]] = value;
    }
  });

  const blockTimeFilterTransformation = getWidgetConstant(widgetType, ["blockTimeFilterTransformation"]);

  //check this array for the filter key , if the key is not present
  //then the filter will always be treated as absolute
  timeBoundFilterKeys.forEach((key: string) => {
    if (blockTimeFilterTransformation?.({ timeFilterName: key })) {
      return;
    }

    let rangeKey = (rangeMap as any)[key];
    //check the range choice key from the mapper report has
    if (!rangeKey) {
      const rangeChoiceKeyMapper = getWidgetConstant(widgetType, TIME_FILTER_RANGE_CHOICE_MAPPER);
      rangeKey = (rangeChoiceKeyMapper || {})[key];
    }
    //if there is no mapper then use the same key
    if (!rangeKey) rangeKey = key;

    let _rangeChoice: any = get(rangeFilterChoice, [rangeKey], defaultTimeRange);

    if (!Object.keys(rangeFilterChoice).length) {
      // no key present in metadata, treat filter as slicing
      _rangeChoice = "slicing";
    }

    // checking for existing filter
    if (typeof _rangeChoice === "string" && filters.filter[key]) {
      _rangeChoice = {
        type: _rangeChoice === "absolute" ? "absolute" : "relative",
        absolute: filters.filter[key],
        relative: {
          next: { num: 0, unit: "days" },
          last: { num: getDaysCount(filters.filter[key]), unit: "days" }
        }
      };
    }

    if (filterKeys.includes(key)) {
      if (key === "ingested_at") {
        _rangeChoice = { ..._rangeChoice, type: "absolute" };
      }

      if (isNaN(filters?.filter?.[key] || "") && Object.keys(filters?.filter?.[key] || {}).length === 0) {
        delete filters.filter[key];
        filters = {
          ...filters,
          filter: {
            ...(filters.filter || {})
          }
        };
      } else {
        filters = {
          ...filters,
          filter: {
            ...(filters.filter || {}),
            [key]:
              _rangeChoice.type === "relative"
                ? getValueFromTimeRange(_rangeChoice.relative, true)
                : filters.filter[key]
          }
        };
      }
    }
  });

  Object.keys(finalFilters?.filter || {})
    .filter(key => key.includes("custom_fields"))
    .forEach(_key => {
      Object.keys(finalFilters?.filter?.[_key])
        .filter(
          key =>
            key?.toLowerCase()?.startsWith("custom") &&
            Object.keys(finalFilters?.filter?.[_key]?.[key] || "").includes("$gt") &&
            Object.keys(finalFilters?.filter?.[_key]?.[key] || "").includes("$lt")
        )
        .forEach(key => {
          let _rangeChoice: any = get(rangeFilterChoice, [key], undefined);

          // no data in meta data, treat filter as relative
          if (!_rangeChoice) {
            _rangeChoice = {
              type: "relative",
              absolute: filters.filter[key],
              relative: {
                next: { num: 0, unit: "days" },
                last: { num: getDaysCount(filters.filter?.custom_fields[key]), unit: "days" }
              }
            };
          }

          filters = {
            ...filters,
            filter: {
              ...(filters.filter || {}),
              [_key]: {
                ...(filters.filter?.[_key] || {}),
                [key]:
                  _rangeChoice.type === "relative"
                    ? getValueFromTimeRange(_rangeChoice.relative, true)
                    : filters.filter[_key][key]
              }
            }
          };
        });
    });

  // Transforming date for Jira Zendesk
  if (["jira_zendesk_report"].includes(widgetType)) {
    const rangeChoice = get(rangeFilterChoice, ["jirazendesk_issue_created_at"], defaultTimeRange);

    const createdAtValue = get(filters, ["filter", "created_at"], undefined);

    filters = {
      ...filters,
      filter: {
        ...(filters.filter || {}),
        jira_issue_created_at:
          rangeChoice.type === "relative"
            ? getValueFromTimeRange(rangeChoice.relative || {}, true)
            : filters.filter["jira_issue_created_at"],
        zendesk_created_at: createdAtValue
      }
    };
  }

  if (
    widgetType === "tickets_report" &&
    filters.filter &&
    filters.filter.issue_due_at &&
    Object.keys(filters.filter.issue_due_at || {}).length > 0
  ) {
    filters = {
      ...filters,
      filter: {
        ...filters.filter,
        issue_due_at: {
          $gte: filters.filter.issue_due_at.$gt,
          $lte: filters.filter.issue_due_at.$lt
        }
      }
    };
  }

  return filters;
};