import deploymentFrequencyReport from "./deployment-frequency/report";
import doraLeadTimeForChange from "./DoraLeadTimeForChange/report";
import doraMeanTimeToRestore from "./DoraMeanTimeReport/reports";
import failureRateReport from "./failure-rate/report";
import leadTimeForChangesReport from "./Leadtime-changes/report";
import meanTimeToRestoreReport from "./meantime-restore/report";

const doraReports = {
  ...leadTimeForChangesReport,
  ...deploymentFrequencyReport,
  ...meanTimeToRestoreReport,
  ...failureRateReport,
  ...doraLeadTimeForChange,
  ...doraMeanTimeToRestore
};

export default doraReports;
