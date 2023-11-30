import { ReportSummary } from "configurations/components/reports";
import { get } from "lodash";
import React, { useMemo } from "react";

interface SynkSummaryComponentProps {
  report: any;
  print?: boolean;
}

const SynkSummaryComponent: React.FC<SynkSummaryComponentProps> = ({ report, print }) => {
  const getSummary = useMemo(() => {
    let summary = {};

    switch (report?.type) {
      case "SNYK":
        summary = {
          "": {
            summary: {
              total: report?.total_vuln_count,
              new: report?.new_vulns_count
            },
            issues_by_severity: {
              high: report?.agg_by_severity?.high?.vulns_found || 0,
              medium: report?.agg_by_severity?.medium?.vulns_found || 0,
              low: report?.agg_by_severity?.low?.vulns_found || 0
            },
            issues_supressed_by_severity: {
              high: report?.agg_by_severity?.high?.vulns_suppressed || 0,
              medium: report?.agg_by_severity?.medium?.vulns_suppressed || 0,
              low: report?.agg_by_severity?.low?.vulns_suppressed || 0
            },
            issues_patched_by_severity: {
              high: report?.agg_by_severity?.high?.vulns_patched || 0,
              medium: report?.agg_by_severity?.medium?.vulns_patched || 0,
              low: report?.agg_by_severity?.low?.vulns_patched || 0
            }
          }
        };
        break;
      case "TENABLE":
        summary = {
          "": {
            summary: {
              total: Object.values(get(report, "agg_by_severity", [])).reduce(
                (total, next: any = []) => total + next.length,
                0
              ),
              new: report?.new_vulns_count
            },
            issues_by_severity: {
              high:
                report?.agg_by_severity && report?.agg_by_severity?.high ? report?.agg_by_severity?.high?.length : 0,
              medium:
                report?.agg_by_severity && report?.agg_by_severity?.medium
                  ? report?.agg_by_severity?.medium?.length
                  : 0,
              low: report?.agg_by_severity && report?.agg_by_severity?.low ? report?.agg_by_severity?.low?.length : 0
            },
            issues_by_status: {
              open: report?.agg_by_status && report?.agg_by_status?.OPEN ? report?.agg_by_status?.OPEN?.length : 0,
              reopen:
                report?.agg_by_status && report?.agg_by_status?.REOPENED ? report?.agg_by_status?.REOPENED?.length : 0
            }
          }
        };
        break;
    }

    return {
      summary,
      details: {}
    };
  }, [report]);

  return <ReportSummary data={getSummary} print={print || false} type={report?.type} />;
};

export default React.memo(SynkSummaryComponent);
