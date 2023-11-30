import * as actions from "reduxConfigs/actions/hygiene.actions";

export const mapHygieneScoresDispatchToProps = dispatch => {
  return {
    hygieneReport: (report, id, filters, weights, customHygienes = []) =>
      dispatch(actions.hygieneReport(report, id, filters, weights, customHygienes)),
    hygieneTrend: (report, id, filters, weights, customHygienes = [], metadata = {}) =>
      dispatch(actions.hygieneTrend(report, id, filters, weights, customHygienes, metadata)),
    azureHygieneReport: (report, id, filters, weights, customHygienes = []) =>
      dispatch(actions.azureHygieneReport(report, id, filters, weights, customHygienes)),
    azureHygieneTrend: (report, id, filters, weights, customHygienes = []) =>
      dispatch(actions.azureHygieneTrend(report, id, filters, weights, customHygienes))
  };
};
