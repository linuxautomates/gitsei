import { get } from "lodash";
import uuidv1 from "uuid/v1";
import { defaultWeights } from "../dashboard/constants/constants";
import widgetConstants from "../dashboard/constants/widgetConstants"
import { WidgetType, widgetTypes } from "../dashboard/helpers/helper";
import { Model } from "../model/Model";
import {
  SHOW_AGGREGATIONS_TAB,
  SHOW_FILTERS_TAB,
  SHOW_METRICS_TAB,
  SHOW_SETTINGS_TAB,
  SHOW_WEIGHTS_TAB
} from "../dashboard/constants/filter-key.mapping";
import { LEVELOPS_MULTITIME_SERIES_REPORT } from "dashboard/constants/applications/multiTimeSeries.application";
import {
  REPORT_FILTERS_CONFIG,
  ForceFullWidth,
  HygieneReports,
  MULTI_SERIES_REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import { ALLOWED_REPORTS_FOR_FILTER_CONFIG } from "dashboard/report-filters/constant";

export const DEFAULT_MAX_RECORDS = 20;
export const DEFAULT_WIDGET_WIDTH = "half";

export class RestWidget extends Model {
  constructor(restData) {
    super(restData);
    this._id = undefined;
    this._dashboard_id = undefined;
    this._name = undefined;
    this._type = undefined;
    this._endpoint = undefined;
    this._query = undefined;
    this._order = 1;
    this._old_order = 1;
    this._width = DEFAULT_WIDGET_WIDTH;
    this._weights = { ...defaultWeights };
    this._max_records = DEFAULT_MAX_RECORDS;
    this._widget_type = "graph";
    this._hidden = false;
    this._children = [];
    this._metadata = {};
    this._description = undefined;
    this._draft = false;

    if (restData) {
      this._id = restData.id || uuidv1();
      this._dashboard_id = restData.dashboard_id;
      this._name = restData.name;
      this._type = restData.type;
      this._endpoint = restData.endpoint;
      this._query = restData.query;
      this._draft = !!restData.draft; //setting draft value from the restData

      if (!!restData.metadata) {
        this._metadata = restData.metadata;
        this._description = restData.metadata.description;
        this._order = restData.metadata.order;
        this._old_order = restData.old_order || restData.metadata.order;
        this._width = restData.metadata.width;

        if (restData.metadata.weights) {
          this._weights = restData.metadata.weights;
        }
        if (restData.metadata.widget_type) {
          this._widget_type = widgetTypes.includes(restData.metadata.widget_type)
            ? restData.metadata.widget_type
            : "graph";
        }
        if (restData.metadata.max_records) {
          this._max_records = restData.metadata.max_records;
        }

        if (restData.metadata.hidden) {
          this._hidden = restData.metadata.hidden;
        }

        if (restData.metadata.children) {
          this._children = restData.metadata.children;
        }

        if (restData.metadata.description || get(widgetConstants, [this.type, "description"], "")) {
          this._description = restData.metadata.description || get(widgetConstants, [this.type, "description"], "");
        }

        if (ForceFullWidth.includes(this.type)) {
          // We don't want to display JiraHygiene widget in
          // half width. This is to forcefully show old Jira
          // Hygiene Widget in full width for previous data...
          this.width = "full";
        }
      }
    }
  }

  get reportMetaData() {
    return get(widgetConstants, [this.reportType], null);
  }

  get reportName() {
    return get(this.reportMetaData, ["name"], "");
  }

  get hasFilterConfigs() {
    // TODO : revert when the new configs for multi-time series are ready
    if (this.isMultiTimeSeriesReport) return true;
    if (ALLOWED_REPORTS_FOR_FILTER_CONFIG.includes(this.reportType)) {
      return !!get(this.reportMetaData, REPORT_FILTERS_CONFIG);
    }
    return false;
  }

  get filterConfig() {
    if (this.hasFilterConfigs) {
      const config = get(this.reportMetaData, REPORT_FILTERS_CONFIG);
      if (this.isMultiTimeSeriesReport) {
        return get(this.reportMetaData, MULTI_SERIES_REPORT_FILTERS_CONFIG);
      }
      if (typeof config === "function") return config({});
      return config;
    }
    return undefined;
  }

  isTabVisibilityHandled = tab => {
    const reportData = this.reportMetaData;
    return reportData && reportData.hasOwnProperty(tab);
  };

  isTabVisible = tab => {
    if (this.isTabVisibilityHandled(tab)) {
      return get(widgetConstants, [this.reportType, tab], false);
    }
    // // By default settings tab is hidden.
    if (tab === SHOW_SETTINGS_TAB) {
      return false;
    }
    // By default all other tabs are visible.
    return true;
  };

  get showSettingsTab() {
    return this.isTabVisible(SHOW_SETTINGS_TAB);
  }

  get showFiltersTab() {
    return this.isTabVisible(SHOW_FILTERS_TAB);
  }

  get showAggregationTab() {
    return this.isTabVisible(SHOW_AGGREGATIONS_TAB) && !this.isStat;
  }

  get showWeightsTab() {
    return this.isTabVisible(SHOW_WEIGHTS_TAB) && this.isHygieneType;
  }

  get showMetricsTab() {
    return this.isTabVisible(SHOW_METRICS_TAB);
  }

  get isHygieneType() {
    return HygieneReports.includes(this.type);
  }

  get isStat() {
    return this.widget_type === WidgetType.STATS;
  }

  get isStatFromTable() {
    return this.widget_type === WidgetType.CONFIGURE_WIDGET_STATS;
  }

  get isWidthConfigurable() {
    return !ForceFullWidth.includes(this.type) && !this.isStat && !this.isStatFromTable;
  }

  get draft() {
    return this._draft ?? false;
  }

  set draft(draft) {
    this._draft = draft;
  }

  get dashboard_id() {
    return this._dashboard_id;
  }

  set dashboard_id(dashboard_id) {
    this._dashboard_id = dashboard_id;
  }

  get children() {
    return this._children;
  }

  set children(children) {
    this._children = children;
  }

  get hidden() {
    return this._hidden;
  }

  set hidden(hidden) {
    this._hidden = hidden;
  }

  get widget_type() {
    return this._widget_type;
  }

  set widget_type(type) {
    this._widget_type = type;
  }

  get max_records() {
    return this._max_records;
  }

  set max_records(max) {
    this._max_records = max;
  }

  get description() {
    return this._description;
  }

  set description(description) {
    this._description = description;
  }

  get id() {
    return this._id;
  }

  set id(id) {
    this._id = id;
  }

  get name() {
    return this._name;
  }

  set name(name) {
    this._name = name;
  }

  get type() {
    return this._type;
  }

  get reportType() {
    return this._type;
  }

  set type(t) {
    this._type = t;
  }

  get endpoint() {
    return this._endpoint;
  }

  set endpoint(e) {
    this._endpoint = e;
  }

  get query() {
    return this._query;
  }

  set query(q) {
    this._query = q;
  }

  get order() {
    return this._order;
  }

  set order(order) {
    this._order = order;
  }

  get oldOlder() {
    return this._old_order;
  }

  set oldOlder(order) {
    this._old_order = order;
  }

  get width() {
    return this._width;
  }

  set width(width) {
    this._width = width;
  }

  get weights() {
    return this._weights;
  }

  set weights(weights) {
    this._weights = weights;
  }

  get metadata() {
    return this._metadata;
  }

  set metadata(metadata) {
    this._metadata = metadata;
  }

  get custom_hygienes() {
    return get(this._metadata, ["custom_hygienes"], []);
  }

  set custom_hygienes(cHygienes) {
    if (!!this._metadata) {
      this._metadata = {};
    }
    this._metadata.custom_hygienes = cHygienes;
  }

  get drilldown_columns() {
    return get(this._metadata, ["drilldown_columns"], undefined);
  }

  set drilldown_columns(columnList) {
    this._metadata.drilldown_columns = columnList;
  }

  get selected_columns() {
    return get(this._metadata, ["selected_columns"], undefined);
  }

  set selected_columns(columnList) {
    this._metadata.selected_columns = columnList;
  }

  get table_filters() {
    return get(this._metadata, ["table_filters"], undefined);
  }

  set table_filters(filters) {
    this._metadata.table_filters = filters;
  }

  get isComposite() {
    return this.widget_type === WidgetType.COMPOSITE_GRAPH;
  }

  get tableId() {
    return this.metadata?.tableId;
  }

  get isChartClickEnabled() {
    return get(this.reportMetaData, ["chart_click_enable"], true);
  }

  get widgetFilters() {
    return get(this.reportMetaData, ["filters"], {});
  }

  get hiddenFilters() {
    return get(this.reportMetaData, ["hidden_filters"], {});
  }

  get chartType() {
    return get(this.reportMetaData, ["chart_type"], undefined);
  }

  get uri() {
    return get(this.reportMetaData, ["uri"], undefined);
  }

  get method() {
    return get(this.reportMetaData, ["method"], undefined);
  }

  get reportDataTransformationFunction() {
    return get(this.reportMetaData, ["transformFunction"], undefined);
  }

  set resetWidgetType(widgetType) {
    if (this._widget_type !== widgetType) {
      this._widget_type = widgetType;
      this._query = {};
      this._type = this._type === LEVELOPS_MULTITIME_SERIES_REPORT ? LEVELOPS_MULTITIME_SERIES_REPORT : "";
      this._weights = {};
      this._max_records = DEFAULT_MAX_RECORDS;
      this._width = DEFAULT_WIDGET_WIDTH;
    }
  }

  get isMultiTimeSeriesReport() {
    return this._metadata?.isMultiTimeSeriesReport;
  }

  set isMultiTimeSeriesReport(multiTimeSeries) {
    this._metadata = { ...this._metadata, isMultiTimeSeriesReport: multiTimeSeries };
  }

  set setMultiSeriesTime(defaultValue) {
    this._metadata = { ...this._metadata, multi_series_time: defaultValue };
  }

  get multiSeriesTime() {
    return this._metadata?.multi_series_time || "quarter";
  }

  get effort_type() {
    return this._metadata?.effort_type || "COMPLETED_EFFORT";
  }

  set effort_type(effortType) {
    this._metadata = { ...this._metadata, effort_type: effortType };
  }

  get json() {
    return {
      ...super.json,
      id: this._id,
      name: this._name,
      dashboard_id: this._dashboard_id,
      endpoint: this._endpoint,
      type: this._type,
      query: this._query,
      old_order: this._old_order,
      draft: this._draft, // Remove this during api call
      metadata: {
        ...(this._metadata || {}),
        order: this._order,
        width: this._width,
        description: this._description,
        weights: this._weights,
        max_records: this._max_records,
        widget_type: this._widget_type,
        children: this._children,
        hidden: this._hidden,
        custom_hygienes: this.custom_hygienes
      }
    };
  }

  static validate(data) {
    let valid = true;
    valid = valid && data.hasOwnProperty("id");
    valid = valid && data.hasOwnProperty("name");
    valid = valid && data.hasOwnProperty("type");
    valid = valid && data.hasOwnProperty("endpoint");
    valid = valid && data.hasOwnProperty("query");
    return valid;
  }

  static isValidWidget(widget) {
    if (widget.draft) {
      return true;
    }
    if (!!widget.type) {
      return true;
    }
    return widget.widget_type === WidgetType.COMPOSITE_GRAPH;
  }

  static newInstance(
    dashboard,
    id,
    widgetType,
    name = "",
    hidden = false,
    type = undefined || "",
    widgets = undefined || ""
  ) {
    let order = 0;
    let _widgetType = [
      WidgetType.GRAPH,
      WidgetType.COMPOSITE_GRAPH,
      WidgetType.CONFIGURE_WIDGET,
      WidgetType.GRAPH_NOTES
    ];

    if (widgetType.includes("stats")) {
      _widgetType = [WidgetType.STATS, WidgetType.CONFIGURE_WIDGET_STATS, WidgetType.STATS_NOTES];
    }

    if (widgets && widgetType !== WidgetType.GRAPH_NOTES) {
      order =
        (widgets
          .filter(w => _widgetType.includes(w?.widget_type) && !w?.hidden)
          ?.reduce((acc, next) => Math.max(acc, next._order), 0) || 0) + 1;
    }
    return new RestWidget({
      id,
      dashboard_id: dashboard.id,
      name: name,
      type: type,
      draft: true,
      metadata: {
        width: DEFAULT_WIDGET_WIDTH,
        order: order,
        widget_type: widgetType,
        children: [],
        description: get(widgetConstants, [type, "description"], ""),
        hidden: hidden
      },
      query: {}
    });
  }
}

export class RestDashboard {
  constructor(restData) {
    this._id = undefined;
    this._name = undefined;
    this._widgets = [];
    this._query = {};
    this._owner_id = null;
    this._public = false;
    this._default = false;
    this._metadata = {};
    this._type = undefined;
    this._public = false;
    this._created_at = null;
    this._demo = false;
    this._category = [];
    //this._global_filters = {};

    if (restData) {
      this._id = restData.id;
      this._name = restData.name;
      this._query = restData.query;
      this._owner_id = restData.owner_id;
      this._default = restData.default;
      this._public = restData.public;
      this._type = restData.type;
      this._created_at = restData.created_at;
      this._demo = restData.demo;
      //this._global_filters = restData.global_filters;

      this._widgets = (restData.widgets || [])
        .map(rawWidget => new RestWidget(rawWidget))
        .filter(w => RestWidget.isValidWidget(w))
        .sort((a, b) => {
          return a.order - b.order;
        });

      if (restData.metadata) {
        this._metadata = restData.metadata;
      }

      if (restData.category) {
        this._category = restData.category;
      }
    }
  }

  get created_at() {
    return this._created_at;
  }

  set created_at(createdAt) {
    this._created_at = createdAt;
  }

  get id() {
    return this._id;
  }

  set id(id) {
    this._id = id;
  }

  get public() {
    return this._public;
  }

  get demo() {
    return this._demo;
  }

  set public(value) {
    if (typeof value === "boolean") {
      this._public = value;
    }
  }

  get name() {
    return this._name;
  }

  get metadata() {
    return this._metadata;
  }

  set name(name) {
    this._name = name;
  }

  get description() {
    return this._metadata.description;
  }

  set description(description) {
    if (!!this._metadata) {
      this._metadata = {};
    }
    this._metadata.description = description;
  }

  get widgets() {
    return this._widgets;
  }

  set widgets(wid) {
    this._widgets = wid;
  }

  get query() {
    return this._query;
  }

  set query(query) {
    this._query = query;
  }

  get owner_id() {
    return this._owner_id;
  }

  set owner_id(owner_id) {
    this._owner_id = owner_id;
  }

  get default() {
    return this._default;
  }

  set default(_default) {
    this._default = _default;
  }

  get type() {
    return this._type;
  }

  set type(type) {
    this._type = type;
  }

  get global_filters() {
    return get(this._metadata, ["global_filters"], {});
  }

  set global_filters(global_filters) {
    if (!!this._metadata) {
      this._metadata = {};
    }
    this._metadata.global_filters = global_filters;
  }

  get jira_or_query() {
    return get(this._metadata, ["jira_or_query"], {});
  }

  set jira_or_query(jira_or_query) {
    if (!!this._metadata) {
      this._metadata = {};
    }
    this._metadata.jira_or_query = jira_or_query;
  }

  get ou_user_filter_designation() {
    return get(this._metadata, ["ou_user_filter_designation"], {});
  }

  set ou_user_filter_designation(ouUserFilterDesignation) {
    if (!!this._metadata) {
      this._metadata = {};
    }
    this._metadata.ou_user_filter_designation = ouUserFilterDesignation;
  }

  static validate(data) {
    let valid = true;
    valid = valid && data.hasOwnProperty("name");
    // valid = valid && data.hasOwnProperty("description");
    valid = valid && data.hasOwnProperty("widgets");
    data.widgets.forEach(widget => {
      valid = valid && RestWidget.validate(widget);
    });
    return valid;
  }

  get ou_ids() {
    return get(this._metadata, ["ou_ids"], "");
  }

  set ou_ids(ouIds) {
    if (!!this._metadata) {
      this._metadata = {};
    }
    this._metadata.ou_ids = ouIds;
  }

  get json() {
    // change widget order
    const _widgets = this.widgets
      .sort((a, b) => {
        return a.order - b.order;
      })
      .map((widget, index) => {
        widget.order = index + 1;
        return widget;
      });

    return {
      id: this._id,
      name: this._name,
      default: this._default,
      public: this._public,
      type: "dashboard", // TODO: Remove/Replace me
      query: this._query,
      owner_id: this._owner_id,
      metadata: this._metadata,
      widgets: this._widgets,
      created_at: this._created_at,
      demo: this._demo,
      category: this._category
    };
  }
}
