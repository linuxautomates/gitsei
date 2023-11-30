import React from "react";

export const DND_ITEMS = {
  STATS_CARD: "STATS_CARD",
  GRAPH_CARD: "GRAPH_CARD"
};

export const DASHBOARD_ICON_MAP = {
  ["Work"]: <i className="fa fa-cog" />,
  ["Resources"]: <i className="fa fa-cubes" />,
  ["Engineers"]: <i className="fa fa-users" />,
  ["Tools & Identifiers"]: <i className="fa fa-wrench" />
};

export const DASHBOARD_PAGE_SIZE = 8;
export const STATS_WIDGET_HEIGHT = 191;
export const GRAPH_WIDGET_HEIGHT = 495;
export const STATS_WIDGET_IN_ROW = 4;
export const GRAPH_WIDGET_IN_ROW = 2;

export const LINE_GRAPH = "linegraph";
export const BAR_GRAPH = "bargraph";
export const STACKED_BAR_GRAPH = "stackedbargraph";
export const LIST = "list";
export const CHIPS = "chips";

export const CodeVolumeVsDeployementIntervalMapping = {
  weekly: "week",
  "bi-weekly": "biweekly",
  monthly: "month"
};