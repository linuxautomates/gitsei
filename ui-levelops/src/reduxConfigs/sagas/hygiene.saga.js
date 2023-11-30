import { get } from "lodash";
import { all, put, select, take, takeEvery } from "redux-saga/effects";
import * as formActions from "reduxConfigs/actions/formActions";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { getData, getError } from "utils/loadingUtils";
import widgetConstants from "../../dashboard/constants/widgetConstants";
import {
  AZURE_HYGIENE_REPORT,
  AZURE_HYGIENE_REPORT_TREND,
  HYGIENE_REPORT,
  HYGIENE_TREND
} from "../actions/hygiene.actions";
import {
  azureMergeCustomHygieneFilters,
  mergeCustomHygieneFilters
} from "dashboard/helpers/drilldown-transformers/jiraDrilldownTransformer";
import { hygieneTypes, zendeskSalesForceHygieneTypes } from "../../dashboard/constants/hygiene.constants";
import { convertEpochToDate, DateFormats, getDateFromTimeStampInGMTFormat } from "../../utils/dateUtils";
import { TIME_INTERVAL_TYPES, WEEK_DATE_FORMAT } from "constants/time.constants";
const restapiState = state => state.restapiReducer;

const getScoreFromPercent = val => {
  if (val === 0) {
    return 100;
  }
  if (val > 0 && val <= 11) {
    return 75;
  }
  if (val > 11 && val <= 21) {
    return 55;
  }
  if (val > 21 && val <= 31) {
    return 30;
  }
  if (val > 31 && val <= 41) {
    return 10;
  }
  return 5;
};

const getScore = (tickets, totalTickets, weight) => {
  if (totalTickets === 0) {
    return 0;
  }
  const percent = (tickets / totalTickets) * 100;
  const scoreFromPercent = getScoreFromPercent(percent);
  const score = (scoreFromPercent * weight) / 100;
  //console.log(` ${totalTickets} ${tickets} percent ${scoreFromPercent} weight ${weight} score ${score}`);
  return score;
};

const getHygieneTypeReportConstant = (reportType, key) => {
  return widgetConstants[reportType]?.[key];
};

const getHygieneTypes = report => {
  return report.includes("zendesk") || report.includes("salesforce") ? zendeskSalesForceHygieneTypes : hygieneTypes;
};

export function* hygieneFetchEffectSaga(action) {
  // first get total number of tickets from list action
  // then for each hygiene type as filter, make the call
  yield put(formActions.formClear(`hygiene_score_${action.id}`));
  const uri = getHygieneTypeReportConstant(action.report, "hygiene_uri");
  let calls = [];
  calls.push({
    uri: action.report === "hygiene_report" ? "jira_tickets" : uri,
    method: "list",
    filters: { ...action.filters, across: "hygiene_type" },
    id: action.id
  });
  const usingOU = !!get(action.filters || {}, ["ou_ids"], undefined);
  let custom_hygienes = [];

  if ((action.customHygienes || []).length > 0) {
    const complete = `COMPLETE_INTEG_CONFIG_LIST`;
    const integrationIds = get(action, ["filters", "filter", "integration_ids"], []);
    const integConfig = "jira_integration_config";

    yield put(
      actionTypes.genericList(integConfig, "list", { filter: { integration_ids: integrationIds } }, complete, "0", true)
    );
    yield take(complete);

    const apiState = yield select(restapiState);

    const integData = getData(apiState, integConfig, "list", "0");

    const cHygienes = (integData.records || []).reduce((agg, obj) => {
      const hygienes = get(obj, ["custom_hygienes"], []);
      agg.push(...hygienes);
      return agg;
    }, []);

    cHygienes.forEach(hygiene => {
      if ((action.customHygienes || []).includes(hygiene.id)) {
        const name = get(hygiene, ["name"], "").replace(/ /g, "_");
        const weight = get(action, ["weights", hygiene.id], 0);
        if (weight > 0) {
          custom_hygienes.push({ name, id: hygiene.id });
          const requestFilters = mergeCustomHygieneFilters(hygiene, action.filters);
          let cHygieneFilters = {
            page_size: 0,
            page: 0,
            across: action.filters?.across || "",
            filter: requestFilters.filter
          };
          if (usingOU) {
            cHygieneFilters = {
              ...(cHygieneFilters || {}),
              ou_ids: get(action.filters || {}, ["ou_ids"], []),
              ou_user_filter_designation: get(action.filters || {}, ["ou_user_filter_designation"], {})
            };
          }
          calls.push({
            uri,
            method: "list",
            id: `${hygiene.id}_${action.id}`,
            filters: cHygieneFilters
          });
        }
      }
    });
  }

  const excludedHygienes = get(action.filters, ["filter", "exclude", "hygiene_types"], []);
  const hygieneTypes = getHygieneTypes(action.report).filter(key => !excludedHygienes.includes(key));
  const filteredHygieneTypes = hygieneTypes.filter(hygiene => get(action, ["weights", hygiene], 0) > 0);

  filteredHygieneTypes.forEach(hygiene => {
    let hygieneTypesFilter = {
      across: action.filters?.across || "",
      filter: {
        ...(action.filters.filter || {}),
        hygiene_types: [hygiene]
      }
    };

    if (usingOU) {
      hygieneTypesFilter = {
        ...(hygieneTypesFilter || {}),
        ou_ids: get(action.filters || {}, ["ou_ids"], []),
        ou_user_filter_designation: get(action.filters || {}, ["ou_user_filter_designation"], {})
      };
    }
    calls.push({
      uri,
      method: "list",
      id: `${hygiene}_${action.id}`,
      filters: hygieneTypesFilter
    });
  });

  yield all(
    calls.map(call =>
      put(
        actionTypes.genericList(call.uri, call.method, call.filters, `COMPLETE_${call.uri}_${call.id}`, call.id, false)
      )
    )
  );
  yield all(calls.map(call => take(`COMPLETE_${call.uri}_${call.id}`)));
  // check all calls went through correctly
  const apiState = yield select(restapiState);
  calls.forEach(call => {
    if (getError(apiState, call.uri, call.method, call.id)) {
    }
  });

  const totalTickets = getData(apiState, action.report === "hygiene_report" ? "jira_tickets" : uri, "list", action.id)
    ._metadata.total_count;
  // action.weights has weight for each hygiene score
  let scores = {};
  const allHygienes = [...filteredHygieneTypes, ...custom_hygienes];
  allHygienes.forEach(hygiene => {
    const name = typeof hygiene === "object" ? hygiene.id : hygiene;
    const label = typeof hygiene === "object" ? hygiene.name : hygiene;
    const data = getData(apiState, uri, "list", `${name}_${action.id}`);
    if (data && data._metadata) {
      let hygieneTickets = 0;
      if (action.report === "hygiene_report") {
        hygieneTickets = data.records.reduce((acc, i) => acc + i.total_tickets, 0);
      } else {
        hygieneTickets = data._metadata.total_count;
      }
      const score = getScore(hygieneTickets, totalTickets, action.weights[name] || 12.5);
      const scoreFromPercent = Math.floor((score * 100) / (action.weights[name] || 12.5));
      const weight = get(action, ["weights", name], 12.5);
      scores[label] = {
        id: hygiene.id || undefined,
        weight,
        score: Math.round(score),
        total_tickets: hygieneTickets,
        score_percent: scoreFromPercent,
        all_tickets: totalTickets
      };
      if (action.report === "hygiene_report") {
        scores[label] = {
          ...scores[label],
          stack_data: data.records.reduce((acc, i) => ({ ...acc, [i.key]: i.total_tickets }), {})
        };
      }
    } else {
      scores[label] = { weight: 0, score: 0, total_tickets: 0, score_percent: 0, all_tickets: 0 };
    }
  });
  // now put this somewhere
  //yield all(calls.map(call => put(actionTypes.restapiClear(call.uri, call.method, call.id))));
  yield put(formActions.formUpdateObj(`hygiene_score_${action.id}`, scores));
}

export function* azureHygieneFetchEffectSaga(action) {
  // first get total number of tickets from list action
  // then for each hygiene type as filter, make the
  yield put(formActions.formClear(`hygiene_score_${action.id}`));
  const uri = getHygieneTypeReportConstant(action.report, "hygiene_uri");
  const usingOU = !!get(action.filters || {}, ["ou_ids"], undefined);
  let calls = [];
  calls.push({
    uri: "issue_management_list",
    method: "list",
    filters: { ...action.filters, across: "workitem_hygiene_type" },
    id: action.id
  });

  let custom_hygienes = [];

  if ((action.customHygienes || []).length > 0) {
    const complete = `COMPLETE_INTEG_CONFIG_LIST`;
    const integrationIds = get(action, ["filters", "filter", "integration_ids"], []);
    const integConfig = "jira_integration_config";

    yield put(
      actionTypes.genericList(integConfig, "list", { filter: { integration_ids: integrationIds } }, complete, "0", true)
    );
    yield take(complete);

    const apiState = yield select(restapiState);

    const integData = getData(apiState, integConfig, "list", "0");

    const cHygienes = (integData.records || []).reduce((agg, obj) => {
      const hygienes = get(obj, ["custom_hygienes"], []);
      agg.push(...hygienes);
      return agg;
    }, []);

    cHygienes.forEach(hygiene => {
      if ((action.customHygienes || []).includes(hygiene.id)) {
        const name = get(hygiene, ["name"], "").replace(/ /g, "_");
        const weight = get(action, ["weights", hygiene.id], 0);
        if (weight > 0) {
          custom_hygienes.push({ name, id: hygiene.id });
          const requestFilters = azureMergeCustomHygieneFilters(hygiene, action.filters);
          let cHygieneFilters = {
            page_size: 0,
            page: 0,
            across: action.filters?.across || "",
            filter: requestFilters.filter
          };
          if (usingOU) {
            cHygieneFilters = {
              ...(cHygieneFilters || {}),
              ou_ids: get(action.filters || {}, ["ou_ids"], []),
              ou_user_filter_designation: get(action.filters || {}, ["ou_user_filter_designation"], {})
            };
          }
          calls.push({
            uri,
            method: "list",
            id: `${hygiene.id}_${action.id}`,
            filters: cHygieneFilters
          });
        }
      }
    });
  }

  const excludedHygienes = get(action.filters, ["filter", "exclude", "workitem_hygiene_types"], []);
  const hygieneTypes = getHygieneTypes(action.report).filter(key => !excludedHygienes.includes(key));
  const filteredHygieneTypes = hygieneTypes.filter(hygiene => get(action, ["weights", hygiene], 0) > 0);

  filteredHygieneTypes.forEach(hygiene => {
    let hygieneTypesFilter = {
      across: action.filters?.across || "",
      filter: {
        ...(action.filters.filter || {}),
        workitem_hygiene_types: [hygiene.toLowerCase()]
      }
    };

    if (usingOU) {
      hygieneTypesFilter = {
        ...(hygieneTypesFilter || {}),
        ou_ids: get(action.filters || {}, ["ou_ids"], []),
        ou_user_filter_designation: get(action.filters || {}, ["ou_user_filter_designation"], {})
      };
    }
    calls.push({
      uri,
      method: "list",
      id: `${hygiene}_${action.id}`,
      filters: hygieneTypesFilter
    });
  });

  yield all(
    calls.map(call =>
      put(
        actionTypes.genericList(call.uri, call.method, call.filters, `COMPLETE_${call.uri}_${call.id}`, call.id, false)
      )
    )
  );
  yield all(calls.map(call => take(`COMPLETE_${call.uri}_${call.id}`)));
  // check all calls went through correctly
  const apiState = yield select(restapiState);
  calls.forEach(call => {
    if (getError(apiState, call.uri, call.method, call.id)) {
    }
  });

  const totalTickets = getData(apiState, "issue_management_list", "list", action.id)?._metadata?.total_count;
  // action.weights has weight for each hygiene score
  let scores = {};
  const allHygienes = [...filteredHygieneTypes, ...custom_hygienes];

  allHygienes.forEach(hygiene => {
    const name = typeof hygiene === "object" ? hygiene.id : hygiene;
    const label = typeof hygiene === "object" ? hygiene.name : hygiene;
    const data = getData(apiState, uri, "list", `${name}_${action.id}`);
    if (data && data._metadata) {
      let hygieneTickets = 0;
      const records = get(data, ["records"], []);
      const hygieneRecords = records.length ? get(Object.values(records[0])[0] || {}, ["records"], []) : [];

      hygieneTickets = hygieneRecords.reduce((acc, i) => acc + i.total_tickets, 0);
      const score = getScore(hygieneTickets, totalTickets, action.weights[name] || 12.5);
      const scoreFromPercent = Math.floor((score * 100) / (action.weights[name] || 12.5));
      const weight = get(action, ["weights", name], 12.5);
      scores[label] = {
        id: hygiene.id || undefined,
        weight,
        score: Math.round(score),
        total_tickets: hygieneTickets,
        score_percent: scoreFromPercent,
        all_tickets: totalTickets
      };
      scores[label] = {
        ...scores[label],
        stack_data: hygieneRecords.reduce((acc, i) => ({ ...acc, [i.key]: i.total_tickets }), {})
      };
    } else {
      scores[label] = { weight: 0, score: 0, total_tickets: 0, score_percent: 0, all_tickets: 0 };
    }
  });

  yield put(formActions.formUpdateObj(`hygiene_score_${action.id}`, scores));
}

export function* azureHygieneTrendFetchEffectSaga(action) {
  yield put(formActions.formClear(`hygiene_score_${action.id}`));
  const uri = getHygieneTypeReportConstant(action.report, "hygiene_trend_uri");
  const usingOU = !!get(action.filters || {}, ["ou_ids"], undefined);
  let calls = [];
  calls.push({
    uri,
    method: "list",
    filters: { ...action.filters, across: "trend", page_size: 1, page: 0 },
    id: action.id
  });

  let custom_hygienes = [];

  if ((action.customHygienes || []).length > 0) {
    const complete = `COMPLETE_INTEG_CONFIG_LIST`;
    const integrationIds = get(action, ["filters", "filter", "integration_ids"], []);
    const integConfig = "jira_integration_config";

    yield put(
      actionTypes.genericList(integConfig, "list", { filter: { integration_ids: integrationIds } }, complete, "0", true)
    );
    yield take(complete);

    const apiState = yield select(restapiState);

    const integData = getData(apiState, integConfig, "list", "0");

    const cHygienes = (integData.records || []).reduce((agg, obj) => {
      const hygienes = get(obj, ["custom_hygienes"], []);
      agg.push(...hygienes);
      return agg;
    }, []);

    cHygienes.forEach(hygiene => {
      if ((action.customHygienes || []).includes(hygiene.id)) {
        const name = get(hygiene, ["name"], "").replace(/ /g, "_");
        const weight = get(action, ["weights", hygiene.id], 0);
        if (weight > 0) {
          custom_hygienes.push({ name, id: hygiene.id });
          const requestFilters = azureMergeCustomHygieneFilters(hygiene, action.filters);
          let cHygieneFilters = {
            page_size: 0,
            page: 0,
            across: "trend",
            interval: action?.filters?.interval,
            filter: requestFilters.filter
          };
          if (usingOU) {
            cHygieneFilters = {
              ...(cHygieneFilters || {}),
              ou_ids: get(action.filters || {}, ["ou_ids"], []),
              ou_user_filter_designation: get(action.filters || {}, ["ou_user_filter_designation"], {})
            };
          }
          calls.push({
            uri,
            method: "list",
            id: `${hygiene.id}_${action.id}`,
            filters: cHygieneFilters
          });
        }
      }
    });
  }

  const excludedHygienes = get(action.filters, ["filter", "exclude", "workitem_hygiene_types"], []);
  const hygieneTypes = getHygieneTypes(action.report).filter(key => !excludedHygienes.includes(key));
  const filteredHygieneTypes = hygieneTypes.filter(hygiene => get(action, ["weights", hygiene], 0) > 0);
  const interval = action?.filters?.interval || "month";

  filteredHygieneTypes.forEach(hygiene => {
    let hygieneTypesFilter = {
      page_size: 1,
      page: 0,
      across: "trend",
      interval: interval,
      filter: {
        ...(action.filters.filter || {}),
        workitem_hygiene_types: [hygiene.toLowerCase()]
      }
    };

    if (usingOU) {
      hygieneTypesFilter = {
        ...(hygieneTypesFilter || {}),
        ou_ids: get(action.filters || {}, ["ou_ids"], []),
        ou_user_filter_designation: get(action.filters || {}, ["ou_user_filter_designation"], {})
      };
    }
    calls.push({
      uri,
      method: "list",
      id: `${hygiene}_${action.id}`,
      filters: hygieneTypesFilter
    });
  });

  yield all(
    calls.map(call =>
      put(
        actionTypes.genericList(call.uri, call.method, call.filters, `COMPLETE_${call.uri}_${call.id}`, call.id, false)
      )
    )
  );
  yield all(calls.map(call => take(`COMPLETE_${call.uri}_${call.id}`)));
  const apiState = yield select(restapiState);

  const newRecords = {};
  const allTicketsTrend = getData(apiState, uri, "list", action.id).records || [];
  const hygieneRecords = allTicketsTrend.length ? get(Object.values(allTicketsTrend[0])[0] || {}, ["records"], []) : [];
  hygieneRecords.forEach(trend => {
    newRecords[trend.key] = { total_tickets: trend.total_tickets };
  });

  const allHygienes = [...filteredHygieneTypes, ...custom_hygienes];

  allHygienes.forEach(hygiene => {
    const name = typeof hygiene === "object" ? hygiene.id : hygiene;
    const recordsState = getData(apiState, uri, "list", `${name}_${action.id}`).records || [];
    const records = recordsState.length ? get(Object.values(recordsState[0])[0] || {}, ["records"], []) : [];
    records.forEach(record => {
      const hygieneTickets = record.total_tickets;
      const totalTickets = get(newRecords, [record.key, "total_tickets"], 0);
      const score = getScore(hygieneTickets, totalTickets, action.weights[name] || 12.5);
      newRecords[record.key] = {
        ...(newRecords[record.key] || {}),
        [name]: score
      };
      const totalScore = Object.keys(newRecords[record.key]).reduce((acc, obj) => {
        if (hygieneTypes.includes(obj)) {
          acc = acc + newRecords[record.key][obj];
        }
        return acc;
      }, 0);
      newRecords[record.key].total_score = totalScore;
    });
  });

  const scoreTrend = Object.keys(newRecords).map(key => {
    const item = { ...newRecords[key] };
    delete item.total_tickets;
    delete item.total_score; // if we need total score in graph, remove this line

    let name = key;
    const newEpoch = getDateFromTimeStampInGMTFormat(key);
    switch (interval) {
      case TIME_INTERVAL_TYPES.DAY:
        name = convertEpochToDate(key, DateFormats.DAY_MONTH, true);
        break;
      case TIME_INTERVAL_TYPES.WEEK:
        name = newEpoch.startOf("week").format(DateFormats.DAY);
        break;
      case TIME_INTERVAL_TYPES.MONTH:
        name = newEpoch.startOf("month").format(DateFormats.MONTH);
        break;
      case TIME_INTERVAL_TYPES.QUARTER:
        name = newEpoch.startOf("quarter").format(DateFormats.QUARTER);
        break;
    }

    return {
      name: name,
      key: key,
      ...item
    };
  });

  const hygieneMapping = (custom_hygienes || []).reduce((acc, next) => {
    return {
      ...acc,
      [next.id]: next.name
    };
  }, {});

  yield put(
    formActions.formUpdateObj(`hygiene_score_${action.id}`, {
      hygieneMapping,
      data: scoreTrend
    })
  );
}

export function* hygieneTrendFetchEffectSaga(action) {
  yield put(formActions.formClear(`hygiene_score_${action.id}`));
  const uri = getHygieneTypeReportConstant(action.report, "hygiene_trend_uri");
  const usingOU = !!get(action.filters || {}, ["ou_ids"], undefined);
  let calls = [];
  calls.push({
    uri,
    method: "list",
    filters: { ...action.filters, across: "trend", page_size: 1, page: 0 },
    id: action.id
  });

  const excludedHygienes = get(action.filters, ["filter", "exclude", "hygiene_types"], []);
  const hygieneTypes = getHygieneTypes(action.report).filter(key => !excludedHygienes.includes(key));
  const filteredHygieneTypes = hygieneTypes.filter(hygiene => get(action, ["weights", hygiene], 0) > 0);
  const interval = action?.filters?.interval || "month";

  filteredHygieneTypes.forEach(hygiene => {
    let hygieneTypesFilter = {
      page_size: 1,
      page: 0,
      across: "trend",
      interval: interval,
      filter: {
        ...(action.filters.filter || {}),
        hygiene_types: [hygiene]
      }
    };

    if (usingOU) {
      hygieneTypesFilter = {
        ...(hygieneTypesFilter || {}),
        ou_ids: get(action.filters || {}, ["ou_ids"], []),
        ou_user_filter_designation: get(action.filters || {}, ["ou_user_filter_designation"], {})
      };
    }
    calls.push({
      //uri: "hygiene_report",
      uri,
      method: "list",
      id: `${hygiene}_${action.id}`,
      filters: hygieneTypesFilter
    });
  });

  let custom_hygienes = [];

  if ((action.customHygienes || []).length > 0) {
    const complete = `COMPLETE_INTEG_CONFIG_LIST`;
    const integrationIds = get(action, ["filters", "integration_ids"], []);
    const integConfig = "jira_integration_config";

    yield put(
      actionTypes.genericList(integConfig, "list", { filter: { integration_ids: integrationIds } }, complete, "0", true)
    );
    yield take(complete);

    const apiState = yield select(restapiState);

    const integData = getData(apiState, integConfig, "list", "0");

    const cHygienes = (integData.records || []).reduce((agg, obj) => {
      const hygienes = get(obj, ["custom_hygienes"], []);
      agg.push(...hygienes);
      return agg;
    }, []);

    cHygienes.forEach(hygiene => {
      if ((action.customHygienes || []).includes(hygiene.id)) {
        const name = get(hygiene, ["name"], "").replace(/ /g, "_");
        const weight = get(action, ["weights", hygiene.id], 0);
        if (weight > 0) {
          custom_hygienes.push({ name, id: hygiene.id });
          const requestFilters = azureMergeCustomHygieneFilters(hygiene, action.filters);
          let cHygieneFilters = {
            page_size: 0,
            page: 0,
            across: "trend",
            interval: action?.filters?.interval,
            filter: requestFilters.filter
          };
          if (usingOU) {
            cHygieneFilters = {
              ...(cHygieneFilters || {}),
              ou_ids: get(action.filters || {}, ["ou_ids"], []),
              ou_user_filter_designation: get(action.filters || {}, ["ou_user_filter_designation"], {})
            };
          }
          calls.push({
            uri,
            method: "list",
            id: `${hygiene.id}_${action.id}`,
            filters: cHygieneFilters
          });
        }
      }
    });
  }

  yield all(
    calls.map(call =>
      put(
        actionTypes.genericList(call.uri, call.method, call.filters, `COMPLETE_${call.uri}_${call.id}`, call.id, false)
      )
    )
  );
  yield all(calls.map(call => take(`COMPLETE_${call.uri}_${call.id}`)));
  const apiState = yield select(restapiState);
  calls.forEach(call => {
    if (getError(apiState, call.uri, call.method, call.id)) {
    }
  });
  const newRecords = {};
  const allTicketsTrend = getData(apiState, uri, "list", action.id).records;
  allTicketsTrend.forEach(trend => {
    newRecords[trend.key] = { total_tickets: trend.total_tickets };
  });

  const allHygienes = [...filteredHygieneTypes, ...custom_hygienes];

  allHygienes.forEach(hygiene => {
    const name = typeof hygiene === "object" ? hygiene.id : hygiene;
    const records = getData(apiState, uri, "list", `${name}_${action.id}`).records || [];
    records.forEach(record => {
      const hygieneTickets = record.total_tickets;
      const totalTickets = get(newRecords, [record.key, "total_tickets"], 0);
      const score = getScore(hygieneTickets, totalTickets, action.weights[name] || 12.5);

      newRecords[record.key] = {
        ...(newRecords[record.key] || {}),
        [name]: score
      };
      const totalScore = Object.keys(newRecords[record.key]).reduce((acc, obj) => {
        if (hygieneTypes.includes(obj)) {
          acc = acc + newRecords[record.key][obj];
        }
        return acc;
      }, 0);
      newRecords[record.key].total_score = totalScore;
    });
  });

  const scoreTrend = uri.includes("zendesk")
    ? Object.keys(newRecords).map(key => ({
        name: convertEpochToDate(key, DateFormats.DAY_MONTH, true),
        total_score: newRecords[key].total_score || 0
      }))
    : Object.keys(newRecords).map(key => {
        const item = { ...newRecords[key] };
        delete item.total_tickets;
        delete item.total_score; // if we need total score in graph, remove this line
        let name = key;
        const newEpoch = getDateFromTimeStampInGMTFormat(key);
        const week_date_format = get(action.metadata, ["weekdate_format"], "day");
        switch (interval) {
          case TIME_INTERVAL_TYPES.DAY:
            name = convertEpochToDate(key, DateFormats.DAY_MONTH, true);
            break;
          case TIME_INTERVAL_TYPES.WEEK:
            if (week_date_format === WEEK_DATE_FORMAT.NUMBER) {
              name = convertEpochToDate(key, DateFormats.WEEK, true);
            } else {
              name = newEpoch.startOf("week").format(DateFormats.DAY);
            }
            break;
          case TIME_INTERVAL_TYPES.MONTH:
            name = newEpoch.startOf("month").format(DateFormats.MONTH);
            break;
          case TIME_INTERVAL_TYPES.QUARTER:
            name = newEpoch.startOf("quarter").format(DateFormats.QUARTER);
            break;
        }

        return {
          name: name,
          ...item,
          key: key
        };
      });

  const hygieneMapping = (custom_hygienes || []).reduce((acc, next) => {
    return {
      ...acc,
      [next.id]: next.name
    };
  }, {});
  //yield all(calls.map(call => put(actionTypes.restapiClear(call.uri, call.method, call.id))));
  yield put(
    formActions.formUpdateObj(`hygiene_score_${action.id}`, {
      hygieneMapping,
      data: scoreTrend
    })
  );
}

export function* hygieneFetchWatcherSaga() {
  yield takeEvery([HYGIENE_REPORT], hygieneFetchEffectSaga);
}

export function* hygieneTrendFetchWatcherSaga() {
  yield takeEvery([HYGIENE_TREND], hygieneTrendFetchEffectSaga);
}

export function* azureHygieneReportSagaWatcher() {
  yield takeEvery([AZURE_HYGIENE_REPORT], azureHygieneFetchEffectSaga);
}

export function* azureHygieneReportTrendSagaWatcher() {
  yield takeEvery([AZURE_HYGIENE_REPORT_TREND], azureHygieneTrendFetchEffectSaga);
}
