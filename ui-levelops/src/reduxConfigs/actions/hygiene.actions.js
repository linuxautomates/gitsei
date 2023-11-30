export const HYGIENE_REPORT = "HYGIENE_REPORT";
export const HYGIENE_TREND = "HYGIENE_TREND";
export const AZURE_HYGIENE_REPORT = "AZURE_HYGIENE_REPORT";
export const AZURE_HYGIENE_REPORT_TREND = "AZURE_HYGIENE_REPORT_TREND";

export const hygieneReport = (report, id, filters, weights, customHygienes = []) => ({
  type: HYGIENE_REPORT,
  id: id,
  filters: filters,
  report,
  weights: weights,
  customHygienes
});

export const azureHygieneReport = (report, id, filters, weights, customHygienes = []) => ({
  type: AZURE_HYGIENE_REPORT,
  id: id,
  filters: filters,
  report,
  weights: weights,
  customHygienes
});

export const hygieneTrend = (report, id, filters, weights, customHygienes = [], metadata = {}) => ({
  type: HYGIENE_TREND,
  id: id,
  filters: filters,
  report,
  weights: weights,
  customHygienes,
  metadata
});

export const azureHygieneTrend = (report, id, filters, weights, customHygienes = []) => ({
  type: AZURE_HYGIENE_REPORT_TREND,
  id: id,
  filters: filters,
  report,
  weights: weights,
  customHygienes
});
