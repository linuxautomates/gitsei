import { csvDownloadDrilldown, csvDownloadTriageGridView } from "../actions/csvDownload.actions";

export const csvDownloadDrilldownToProps = dispatch => {
  return {
    csvDownloadDrilldown: (uri, method, filters, columns) =>
      dispatch(csvDownloadDrilldown(uri, method, filters, columns)),
    csvDownloadTriageGridView: (uri, method, filters, columns) =>
      dispatch(csvDownloadTriageGridView(uri, method, filters, columns))
  };
};
