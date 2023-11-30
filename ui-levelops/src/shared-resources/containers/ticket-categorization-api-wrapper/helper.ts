export const reloadOnMetaDataChangeKeys: string[] = ["effort_type"];

export const trimPreviewFromId = (widgetId: string) => {
  if (!widgetId.includes("preview")) return widgetId;
  return widgetId.substring(0, widgetId.indexOf("preview") - 1);
};
