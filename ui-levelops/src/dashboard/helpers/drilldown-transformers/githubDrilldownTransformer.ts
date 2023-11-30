import { getGroupByRootFolderKey } from "configurable-dashboard/helpers/helper";
import { SCM_REPORTS } from "dashboard/constants/applications/names";
import { valuesToFilters } from "dashboard/constants/constants";
import widgetConstants from "dashboard/constants/widgetConstants"
import { cloneDeep, forEach, get, unset } from "lodash";
import moment from "moment";
import { combineAllFilters } from "shared-resources/containers/widget-api-wrapper/helper";
import { DateFormats } from "utils/dateUtils";
import { genericDrilldownTransformer } from ".";
import { convertChildKeysToSiblingKeys } from "./../../../shared-resources/containers/widget-api-wrapper/helper";

export const githubFilesDrilldownTransformer = (data: any) => {
  const { drillDownProps, widget, dashboardQuery, metaData } = data;
  const { x_axis } = drillDownProps;
  let { across, ...remainData } = widget.query;
  across = "repo_id";
  const widgetFilter = get(widgetConstants, [widget.type, "filters"], {});
  const hiddenFilters = get(widgetConstants, [widget.type, "hidden_filters"], {});

  const initialFilters = combineAllFilters(remainData, widgetFilter, hiddenFilters);

  let filters = {
    filter: {
      ...(initialFilters || {}),
      ...(dashboardQuery || {})
    },
    across
  };

  if (typeof x_axis === "string" && !get(metaData, [getGroupByRootFolderKey(widget.type)], undefined)) {
    let filterValue = get(valuesToFilters, [across], across);
    let acrossFilterValue: any = [x_axis && x_axis.includes("UNASSIGNED") ? "_UNASSIGNED_" : x_axis] || [];
    filters = {
      ...filters,
      filter: {
        ...(filters.filter || {}),
        [filterValue]: acrossFilterValue
      }
    };
  }

  if (get(metaData, [getGroupByRootFolderKey(widget.type)], undefined)) {
    const key = "scm_files_report" === widget.type ? "module" : "scm_module";
    filters = {
      ...filters,
      filter: {
        ...(filters.filter || {}),
        [key]: drillDownProps.x_axis
      }
    };
  }

  return { acrossValue: across, filters };
};

export const scmIssueTimeAcrossStagesDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  const { drillDownProps } = data;
  const { x_axis } = drillDownProps;
  if (["issue_created", "issue_closed"].includes(acrossValue)) {
    const filterKey = `${acrossValue}_at`;
    const xaxisTimestamp = moment.utc(x_axis, DateFormats.DAY).unix();
    const timeRangeFilter = {
      $gt: `${xaxisTimestamp}`,
      $lt: `${xaxisTimestamp + 86399}`
    };
    filters = {
      ...filters,
      filter: {
        ...(filters.filter || {}),
        [filterKey]: timeRangeFilter
      }
    };
  }

  if (acrossValue === "column") {
    filters = {
      ...filters,
      filter: {
        ...(filters.filter || {}),
        columns: [x_axis]
      }
    };
  }

  return { acrossValue, filters };
};

export const enhancedSCMDrilldownTransformer = (data: any) => {
  const { metaData: widgetMetaData, widget, drillDownProps } = data;
  const { x_axis, scmGlobalSettings } = drillDownProps;
  const reportType = widget?.type;
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  let finalFilters = filters;
  const code_change_size_unit = get(widgetMetaData, "code_change_size_unit", scmGlobalSettings.code_change_size_unit);

  const code_change_small = get(widgetMetaData, "code_change_size_small", scmGlobalSettings.code_change_size_small);
  const code_change_medium = get(widgetMetaData, "code_change_size_medium", scmGlobalSettings.code_change_size_medium);

  const comment_density_small = get(widgetMetaData, "comment_density_small", scmGlobalSettings.comment_density_small);
  const comment_density_medium = get(
    widgetMetaData,
    "comment_density_medium",
    scmGlobalSettings.comment_density_medium
  );

  let newMappedFilters: any = {
    code_change_size_unit,
    code_change_size_config: {
      small: code_change_small.toString(),
      medium: code_change_medium.toString()
    }
  };

  if (!["github_commits_report", "github_commits_single_stat"].includes(reportType)) {
    newMappedFilters = {
      ...newMappedFilters,
      comment_density_size_config: {
        shallow: comment_density_small.toString(),
        good: comment_density_medium.toString()
      }
    };
  }

  if (acrossValue === "trend" && ["github_commits_report", "github_commits_single_stat"].includes(reportType)) {
    const interval = get(finalFilters, ["filter", "interval"], undefined);
    if (interval) {
      let dayOfWeek = {};
      if (interval === "day" && typeof x_axis === "string") {
        dayOfWeek = { days_of_week: [x_axis] };
      }
      finalFilters = {
        ...(finalFilters || {}),
        filter: {
          ...(finalFilters?.filter || {}),
          ...dayOfWeek,
          no_update_time_field: true
        },
        interval
      };
    }
    unset(finalFilters, ["filter", "interval"]);
  }

  finalFilters = {
    ...(finalFilters || {}),
    filter: {
      ...(finalFilters?.filter || {}),
      ...newMappedFilters
    }
  };

  return { acrossValue, filters: finalFilters };
};

export const scmPrsMergeTrendsDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = enhancedSCMDrilldownTransformer(data);

  const finalFilters = {
    ...(filters || {}),
    filter: {
      ...(filters?.filter || {}),
      missing_fields: {
        pr_merged: false
      }
    }
  };

  return { acrossValue, filters: finalFilters };
};

export const scmIssuesFirstReponseTrendDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  const { drillDownProps } = data;
  let { x_axis } = drillDownProps || {};
  const filterKey = `${acrossValue}_at`;
  const xaxisTimestamp = moment.utc(x_axis, DateFormats.DAY).unix();
  const timeRangeFilter = {
    $gt: `${xaxisTimestamp}`,
    $lt: `${xaxisTimestamp + 86399}`
  };

  const finalFilters = {
    ...(filters || {}),
    filter: {
      ...(filters?.filter || {}),
      no_update_time_field: true,
      [filterKey]: timeRangeFilter
    }
  };
  return { acrossValue, filters: finalFilters };
};

export const SCMPRSFirstReviewTrendsDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  let finalFilters = filters;
  if (["pr_merged", "pr_closed"].includes(acrossValue)) {
    finalFilters.filter = {
      ...finalFilters.filter,
      missing_fields: {
        [acrossValue]: false
      }
    };
  }

  finalFilters = {
    ...(finalFilters || {}),
    filter: {
      ...(finalFilters?.filter || {}),
      has_comments: true
    }
  };

  return { acrossValue, filters: finalFilters };
};

export const SCMPRSFirstReviewToMergeTrendsDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  let finalFilters = filters;
  ["pr_closed"].includes(acrossValue)
    ? (finalFilters.filter = {
        ...finalFilters.filter,
        missing_fields: {
          ["pr_merged"]: false,
          [acrossValue]: false
        }
      })
    : (finalFilters.filter = {
        ...finalFilters.filter,
        missing_fields: {
          ["pr_merged"]: false
        }
      });

  finalFilters = {
    ...(finalFilters || {}),
    filter: {
      ...(finalFilters?.filter || {}),
      has_comments: true
    }
  };

  return { acrossValue, filters: finalFilters };
};

export const scmReworkDrilldownTransformer = (data: any) => {
  const { metaData: widgetMetaData } = data;
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  let finalFilters = filters;

  const interval: number = get(widgetMetaData, "legacy_update_interval_config", 30);
  const now = moment().startOf("day").unix();
  const prev = now - interval * 86400;
  finalFilters = {
    ...(finalFilters || {}),
    filter: {
      ...finalFilters.filter,
      include_metrics: true,
      legacy_update_interval_config: prev.toString()
    }
  };
  return { acrossValue, filters: finalFilters };
};

export const SCMReviewCollaborationDrilldownTransformer = (data: any) => {
  const { drillDownProps, contextFilters } = data;

  let newMappedFilters = {};
  let rawData = { ...data };

  if (drillDownProps.hasOwnProperty("scm_review_collaboration")) {
    newMappedFilters = drillDownProps?.scm_review_collaboration;
  } else if (typeof drillDownProps.x_axis === "string") {
    newMappedFilters = JSON.parse(drillDownProps.x_axis);
    rawData = {
      ...rawData,
      x_axis: newMappedFilters,
      x: newMappedFilters
    };
  }

  let { filters } = genericDrilldownTransformer(rawData);
  let finalFilters = filters;

  unset(newMappedFilters, "name");

  finalFilters = {
    ...(finalFilters || {}),
    filter: {
      ...(finalFilters?.filter || {}),
      ...newMappedFilters,
      ...(contextFilters || {})
    }
  };

  if (newMappedFilters.hasOwnProperty("creators")) {
    unset(finalFilters, ["filter", "approvers"]);
  } else if (newMappedFilters.hasOwnProperty("approvers")) {
    unset(finalFilters, ["filter", "creators"]);
  }

  finalFilters = convertChildKeysToSiblingKeys(finalFilters, "filter", ["stacks", "sort", "ou_exclusions"]);

  const keysToUnset = ["across", "stacks"];

  forEach(keysToUnset, key => {
    unset(finalFilters, [key]);
  });

  return { acrossValue: "", filters: finalFilters };
};

export const scmIssueResolutionTimeDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  const interval = filters?.filter?.interval ?? filters?.interval;
  /* 
    LFE-2185 :  We were storing the value for Issue Last Closed By Week (month , quarter) as 
    across = issue_created interval = week /month /quarter 
    checking the case for the same.
    We also have Issue Created By Date where across = issue_created but in that case we have
    interval = day 
  */
  if (acrossValue === "issue_created" && ["week", "quarter", "month"].includes(interval)) {
    filters = { ...filters, across: "issue_closed" };
    acrossValue = "issue_closed";
  }
  return { acrossValue, filters };
};

export const scmIssueTrendTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  const { drillDownProps } = data;
  const { x_axis } = drillDownProps;
  const finalFilters = {
    ...filters,
    filter: {
      ...filters?.filter,
      [`${acrossValue}_at`]: {
        $gte: x_axis,
        $lte: `${parseInt(x_axis) + 86399}`
      },
      no_update_time_field: true
    }
  };
  return { acrossValue, filters: finalFilters };
};

export const scmDrilldownTranformerForIncludesFilter = (data: any) => {
  const { widget, drillDownProps } = data || {};
  let allFilters: any = {};
  switch (widget?.type) {
    case SCM_REPORTS.COMMITS_REPORT:
      allFilters = enhancedSCMDrilldownTransformer(data);
      break;
    case SCM_REPORTS.CODING_DAYS_REPORT:
      allFilters = genericDrilldownTransformer(data);
      break;
    case SCM_REPORTS.REWORK_REPORT:
      allFilters = scmReworkDrilldownTransformer(data);
      break;
  }
  const include_metrics = allFilters?.filters?.filter.include_metrics || drillDownProps.isOpenReport || false;
  allFilters = {
    ...(allFilters || {}),
    filters: {
      ...(allFilters?.filters || {}),
      filter: {
        ...(allFilters?.filters?.filter || {}),
        include_metrics
      }
    }
  };

  return allFilters;
};
