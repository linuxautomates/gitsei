import { RestTicketCategorizationProfileJSONType } from "configurations/pages/ticket-categorization/types/ticketCategorization.types";
import React from "react";
import { WIDGET_CONFIGURATION_KEYS, WIDGET_CONFIGURATION_PARENT_KEYS } from "../../constants/widgets";

export const WidgetFilterContext = React.createContext({
  filters: {},
  setFilters: (id: string, filters: Object) => {}
});
export const WidgetPreviewFilterContext = React.createContext({
  filters: {},
  setFilters: (id: string, filters: Object) => {}
});

export const WidgetLoadingContext = React.createContext({
  setWidgetLoading: (id: string, loading: boolean) => {},
  isThisWidgetLoading: false
});

export const WidgetsLoadingContext = React.createContext({
  setWidgetsLoading: (id: string, loading: boolean) => {},
  widgetsLoadingDict: {}
});

export const WidgetSvgContext = React.createContext({
  haveToTakeSnapshot: false,
  setSvg: (id: string, svg: any) => {}
});

export const SelectWidgetToBeCopiedContext = React.createContext({
  setWidgetToBeCopied: (widgetId: string) => {}
});

export const WidgetTabsContext = React.createContext<{
  isVisibleOnTab: (tab: WIDGET_CONFIGURATION_KEYS | WIDGET_CONFIGURATION_PARENT_KEYS, parentTab?: boolean) => boolean;
}>({
  isVisibleOnTab: (tab: WIDGET_CONFIGURATION_KEYS | WIDGET_CONFIGURATION_PARENT_KEYS, parentTab?: boolean) => {
    return false;
  }
});
export const ProfileUpdateContext = React.createContext({
  handleUpdate: (updatedProfile: RestTicketCategorizationProfileJSONType) => {}
});

export const DashboardWidgetResolverContext = React.createContext<{
  dashboardId: string;
}>({
  dashboardId: ""
});
export const WidgetIntervalContext = React.createContext<{
  tempWidgetInterval: Record<string, string>;
  setTempWidgetInterval: (widgetId: string, setTempWidgetInterval: string) => void;
}>({
  tempWidgetInterval: {},
  setTempWidgetInterval: (widgetId: string, setTempWidgetInterval: string) => {}
});
export const WidgetBGColorContext = React.createContext<{
  setWidgetBGColor: (widgetId: string, color: string) => void;
  widgetBGColor: Record<string, string>;
}>({ setWidgetBGColor: (widgetId: string, color: string) => {}, widgetBGColor: {} });

export const CacheWidgetPreview = React.createContext(false);

export const WidgetPayloadContext = React.createContext<{
  payload: Object;
  setPayload: (payload: Object) => void;
}>({
  payload: {},
  setPayload: () => {}
});

export const WidgetDrilldownHandlerContext = React.createContext<{
  setDrilldown: (payload: any) => void;
  isDrilldownOpen: boolean;
  drilldownWidgetId: string;
}>({
  setDrilldown: () => {},
  isDrilldownOpen: false,
  drilldownWidgetId: ""
});

export const DashboardColorSchemaContext = React.createContext<{
  colorSchema: Record<string, string>;
}>({
  colorSchema: {}
});

export const DevWidgetTimeRangeContext = React.createContext<{
  dashboardTimeRange: string;
}>({
  dashboardTimeRange: ""
});

export const widgetOtherKeyDataContext = React.createContext({
  otherKeyData: {},
  setOtherKey: (id: string, otherKey: Object) => {}
});
