import { get } from "lodash";
import moment from "moment";

import { convertToDate, getDayFromEvery2WeekBetweenTimeStamps } from "../../utils/dateUtils";
import { sprintReportDataKeyTypes } from "../../dashboard/graph-filters/components/sprintFilters.constant";
import { sprintStatCalculateBasedOnMetric } from "../../custom-hooks/helpers/sprintStatReporthelper";
import { AZURE_SPRINT_REPORTS } from "../../dashboard/constants/applications/names";

function getAvailableMondays(data: any) {
  const { filters } = data;
  const $gt = filters?.completed_at?.["$gt"];
  const $lt = filters?.completed_at?.["$lt"];
  const interval = filters.across || "sprint";
  if (!$gt || !$lt || !interval) {
    return [];
  }
  return getDayFromEvery2WeekBetweenTimeStamps($gt, $lt, "mon");
}

function getMappedData(data: any) {
  const { apiData, filters } = data;
  const interval = filters.across || "sprint";
  const _apiData = apiData ?? [];
  const mappedData: any = {};
  let availableRanges: any[] = [];
  if (interval === "bi_week") {
    availableRanges = getAvailableMondays(data);
  }

  _apiData.forEach((item: any) => {
    const {
      committed_story_points,
      creep_story_points,
      delivered_creep_story_points,
      delivered_story_points,
      commit_delivered_story_points,
      key,
      additional_key,
      sprint_id,
      delivered_keys,
      delivered_creep_keys,
      commit_delivered_keys,
      creep_keys,
      committed_keys
    } = item;
    const updateMappedData = (
      timestamp: number,
      additional_key: string | null = null,
      sprint_id: string | null = null
    ) => {
      if (!mappedData[timestamp]) {
        mappedData[timestamp] = {};
      }
      mappedData[timestamp] = {
        key: timestamp,
        additional_key,
        sprint_id,
        delivered_story_points: delivered_story_points + (mappedData[timestamp]["delivered_story_points"] ?? 0),
        committed_story_points: committed_story_points + (mappedData[timestamp]["committed_story_points"] ?? 0),
        delivered_creep_story_points:
          delivered_creep_story_points + (mappedData[timestamp]["delivered_creep_story_points"] ?? 0),
        creep_story_points: creep_story_points + (mappedData[timestamp]["creep_story_points"] ?? 0),
        commit_delivered_story_points:
          commit_delivered_story_points + (mappedData[timestamp]["commit_delivered_story_points"] ?? 0),

        delivered_keys: delivered_keys?.length + (mappedData?.[timestamp]?.["delivered_keys"] ?? 0),
        committed_keys: committed_keys?.length + (mappedData?.[timestamp]?.["committed_keys"] ?? 0),
        delivered_creep_keys: delivered_creep_keys?.length + (mappedData?.[timestamp]?.["delivered_creep_keys"] ?? 0),
        creep_keys: creep_keys?.length + (mappedData?.[timestamp]?.["creep_keys"] ?? 0),
        commit_delivered_keys: commit_delivered_keys?.length + (mappedData?.[timestamp]?.["commit_delivered_keys"] ?? 0)
      };
    };
    const updateSprintMappedData = (
      timestamp: number,
      additional_key: string | null = null,
      sprint_id: string | null = null
    ) => {
      if (!mappedData[sprint_id as any]) {
        mappedData[sprint_id as any] = {};
      }
      mappedData[sprint_id as any] = {
        key: timestamp,
        additional_key,
        sprint_id,
        delivered_story_points: delivered_story_points + (mappedData[sprint_id as any]["delivered_story_points"] ?? 0),
        committed_story_points: committed_story_points + (mappedData[sprint_id as any]["committed_story_points"] ?? 0),
        delivered_creep_story_points:
          delivered_creep_story_points + (mappedData[sprint_id as any]["delivered_creep_story_points"] ?? 0),
        creep_story_points: creep_story_points + (mappedData[sprint_id as any]["creep_story_points"] ?? 0),
        commit_delivered_story_points:
          commit_delivered_story_points + (mappedData[sprint_id as any]["commit_delivered_story_points"] ?? 0),

        delivered_keys: delivered_keys?.length + (mappedData?.[timestamp]?.["delivered_keys"] ?? 0),
        committed_keys: committed_keys?.length + (mappedData?.[timestamp]?.["committed_keys"] ?? 0),
        delivered_creep_keys: delivered_creep_keys?.length + (mappedData?.[timestamp]?.["delivered_creep_keys"] ?? 0),
        creep_keys: creep_keys?.length + (mappedData?.[timestamp]?.["creep_keys"] ?? 0),
        commit_delivered_keys: commit_delivered_keys?.length + (mappedData?.[timestamp]?.["commit_delivered_keys"] ?? 0)
      };
    };
    if (interval === "sprint") {
      updateSprintMappedData(key, additional_key, sprint_id);
    } else if (interval === "bi_week") {
      const _sprintDate = parseInt(key);
      const selectedRange = availableRanges.find((item: any) => item.$gt <= _sprintDate && item.$lt >= _sprintDate);
      if (selectedRange) {
        updateMappedData(selectedRange.$gt, null, sprint_id);
      }
    } else {
      const timestamp = moment
        .unix(parseInt(key))
        .utc()
        .startOf(interval === "month" ? "month" : "isoWeek")
        .unix();
      updateMappedData(timestamp, null, sprint_id);
    }
  });
  return mappedData;
}

export const sprintMetricsPercentReportTransformer = (data: any) => {
  const { widgetFilters } = data;

  const metrics = get(widgetFilters, ["filter", "metric"], []);
  const viewBy = get(widgetFilters, ["filter", "view_by"], undefined);

  const mappedData: any = getMappedData(data);

  if (!mappedData || Object.keys(mappedData).length === 0) {
    return { data: [] };
  }

  const transformedData: any[] = [];
  Object.values(mappedData).forEach((data: any, index: number) => {
    let _item = {};
    const { key, additional_key, newKeys = [key] } = data;
    const { num: creep_to_commit_ratio } = sprintStatCalculateBasedOnMetric("avg_creep", 0, data, 0, viewBy);
    const { num: creep_done_to_commit_ratio } = sprintStatCalculateBasedOnMetric(
      "avg_creep_done_to_commit",
      0,
      data,
      0,
      viewBy
    );
    const { num: done_to_commit_ratio } = sprintStatCalculateBasedOnMetric("avg_commit_to_done", 0, data, 0, viewBy);
    const { num: avg_creep_to_done } = sprintStatCalculateBasedOnMetric("avg_creep_to_done", 0, data, 0, viewBy);
    const { num: avg_creep_to_miss } = sprintStatCalculateBasedOnMetric("avg_creep_to_miss", 0, data, 0, viewBy);
    const { num: commit_to_done } = sprintStatCalculateBasedOnMetric("commit_to_done", 0, data, 0, viewBy);
    const { num: avg_commit_to_miss } = sprintStatCalculateBasedOnMetric("avg_commit_to_miss", 0, data, 0, viewBy);

    if (metrics.includes("creep_to_commit_ratio")) {
      _item = {
        creep_to_commit_ratio: creep_to_commit_ratio ? Math.round(creep_to_commit_ratio * 100) : 0
      };
    }

    if (metrics.includes("done_to_commit_ratio")) {
      _item = {
        ..._item,
        done_to_commit_ratio: done_to_commit_ratio ? Math.round(done_to_commit_ratio * 100) : 0
      };
    }

    if (metrics.includes("creep_done_to_commit_ratio")) {
      _item = {
        ..._item,
        creep_done_to_commit_ratio: creep_done_to_commit_ratio ? Math.round(creep_done_to_commit_ratio * 100) : 0
      };
    }

    if (metrics.includes("creep_done_ratio")) {
      _item = {
        ..._item,
        creep_done_ratio: avg_creep_to_done ? Math.round(avg_creep_to_done * 100) : 0
      };
    }

    if (metrics.includes("creep_missed_ratio")) {
      _item = {
        ..._item,
        creep_missed_ratio: avg_creep_to_miss ? Math.round(avg_creep_to_miss * 100) : 0
      };
    }

    if (metrics.includes("commit_missed_ratio")) {
      _item = {
        ..._item,
        commit_missed_ratio: avg_commit_to_miss ? Math.round(avg_commit_to_miss * 100) : 0
      };
    }

    if (metrics.includes("commit_done_ratio")) {
      _item = {
        ..._item,
        commit_done_ratio: commit_to_done ? Math.round(commit_to_done * 100) : 0
      };
    }

    transformedData.push({
      key,
      name: additional_key ?? convertToDate(key),
      ..._item
    });
  });
  return { data: transformedData };
};

export const sprintMetricsTrendTransformer = (data: any) => {
  const { widgetFilters } = data;

  const metrics = get(widgetFilters, ["filter", "metric"], []);

  const viewBy = get(widgetFilters, ["filter", "view_by"], "Points");
  const mappedData: any = getMappedData(data);

  if (!mappedData || Object.keys(mappedData).length === 0) {
    return { data: [] };
  }
  const transformedData: any[] = [];
  Object.values(mappedData).forEach((data: any, index: number) => {
    let _item: any = {};
    let {
      key,
      delivered_creep_story_points,
      creep_story_points,
      committed_story_points,
      additional_key,
      sprint_id,
      commit_delivered_story_points,

      delivered_creep_keys,
      commit_delivered_keys,
      creep_keys,
      committed_keys
    } = data;

    if (viewBy === "Points") {
      if (metrics.includes("commit_not_done_points")) {
        if (commit_delivered_story_points > committed_story_points) {
          _item["commit_over_done_points"] = Math.abs(commit_delivered_story_points - committed_story_points);
        } else {
          _item["commit_not_done_points"] = Math.abs(committed_story_points - commit_delivered_story_points);
        }
      }

      if (metrics.includes("commit_done_points")) {
        _item["commit_done_points"] = commit_delivered_story_points;
      }

      if (metrics.includes("creep_done_points")) {
        _item["creep_done_points"] = -delivered_creep_story_points;
      }

      if (metrics.includes("creep_not_done_points")) {
        if (delivered_creep_story_points > creep_story_points) {
          _item["creep_over_done_points"] = -Math.abs(delivered_creep_story_points - creep_story_points);
        } else {
          _item["creep_not_done_points"] = -Math.abs(creep_story_points - delivered_creep_story_points);
        }
      }
    }

    if (viewBy === "Tickets") {
      if (metrics.includes("commit_not_done_points")) {
        if (commit_delivered_keys > committed_keys) {
          _item["commit_over_done_points"] = Math.abs(commit_delivered_keys - committed_keys);
        } else {
          _item["commit_not_done_points"] = Math.abs(committed_keys - commit_delivered_keys);
        }
      }

      if (metrics.includes("commit_done_points")) {
        _item["commit_done_points"] = commit_delivered_keys;
      }

      if (metrics.includes("creep_done_points")) {
        _item["creep_done_points"] = -delivered_creep_keys;
      }

      if (metrics.includes("creep_not_done_points")) {
        if(delivered_creep_keys > creep_keys){
          _item["creep_over_done_points"] = -Math.abs(delivered_creep_keys - creep_keys);
        }else{
          _item["creep_not_done_points"] = -Math.abs(creep_keys - delivered_creep_keys);
        }
      }
    }

    transformedData.push({
      key,
      sprint_id,
      name: additional_key ?? convertToDate(key),
      ..._item
    });
  });
  return { data: transformedData };
};

export const sprintImpactTransformer = (data: any) => {
  const { apiData, reportType, widgetFilters } = data;
  const viewBy = get(widgetFilters, ["filter", "view_by"], undefined);

  const transformedData: any[] = [];
  (apiData || []).forEach((data: any, index: number) => {
    let val1: number = 0,
      val2: number = 0,
      val3: number = 0,
      val4: number = 0,
      num: number = 0;

    const { key, additional_key } = data;
    const isViewByPoints = viewBy === "Points";
    const unestimatedItemKey =
      reportType === AZURE_SPRINT_REPORTS.SPRINT_IMPACT
        ? "unestimated_workitems_by_type"
        : "unestimated_issues_by_type";

    val1 = isViewByPoints
      ? data[sprintReportDataKeyTypes.COMMIT_DELIVERED_STORY_POINTS]
      : data?.[sprintReportDataKeyTypes.COMMIT_DELIVERED_KEYS]?.length;
    val2 = isViewByPoints
      ? data[sprintReportDataKeyTypes.DELIVERED_CREEP_STORY_POINTS]
      : data[sprintReportDataKeyTypes.DELIVERED_CREEP_KEYS]?.length;
    val3 = isViewByPoints
      ? data[sprintReportDataKeyTypes.COMMITED_STORY_POINTS]
      : data[sprintReportDataKeyTypes.COMMITTED_KEYS]?.length;
    val4 = isViewByPoints
      ? data[sprintReportDataKeyTypes.CREEP_STORY_POINTS]
      : data[sprintReportDataKeyTypes.CREEP_KEYS]?.length;

    if (val1 && val2 && val3 && val4) {
      num = Math.max(0, val3 - val1) + Math.max(0, val4 - val2);
    }

    transformedData.push({
      name: convertToDate(key),
      [isViewByPoints ? "missed_points" : "missed_tickets"]: -1 * num,
      ...(data?.[unestimatedItemKey] || {})
    });
  });
  return { data: transformedData };
};
