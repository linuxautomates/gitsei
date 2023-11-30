import bullseyeBranchCoverageReport from "./branch-coverage-report/report";
import bullseyeBranchCoverageTrendReport from "./branch-coverage-trend-report/report";
import bullseyeCodeCoverageReport from "./code-coverage-report/report";
import bullseyeCodeCoverageTrendReport from "./code-coverage-trend-report/report";
import bullseyeDecisionCoverageReport from "./decision-coverage-report/report";
import bullseyeDecisionCoverageTrendReport from "./decision-coverage-trend-report/report";
import bullseyeFunctionCoverageReport from "./function-coverage-report/report";
import bullseyeFunctionCoverageTrendReport from "./function-coverage-trend-report/report";

const bullseyeReports = {
  ...bullseyeFunctionCoverageReport,
  ...bullseyeBranchCoverageReport,
  ...bullseyeDecisionCoverageReport,
  ...bullseyeCodeCoverageReport,
  ...bullseyeFunctionCoverageTrendReport,
  ...bullseyeBranchCoverageTrendReport,
  ...bullseyeDecisionCoverageTrendReport,
  ...bullseyeCodeCoverageTrendReport
};

export default bullseyeReports;
