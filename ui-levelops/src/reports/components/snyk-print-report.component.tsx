import React from "react";
import { AntCol, AntRow } from "shared-resources/components";
import { ReportTable } from "configurations/components/reports";
import { SnykHeader, SnykSummary } from "./index";
import "configurations/containers/reports/report-print.css";

export const SnykPrintReportComponent = React.forwardRef<HTMLDivElement, any>((props, ref) => {
  const { reports } = props;
  return (
    <div style={{ display: "none" }}>
      <div ref={ref} style={{ margin: "30px", breakBefore: "page" }}>
        {(reports || [])?.map((report: any) => {
          return (
            <>
              <SnykHeader type={report.type} products={report.products} integrations={report.integrations} />
              <AntRow>
                <div style={{ height: "90vh", padding: "5vh", breakAfter: "page" }}>
                  <AntCol span={24}>
                    <SnykSummary report={report} print={true} />
                  </AntCol>
                </div>
                <div
                  style={{
                    margin: "10px",
                    breakAfter: "page"
                  }}>
                  {report.type === "SNYK" && (
                    <>
                      <AntCol span={24}>
                        <ReportTable
                          data={report.suppressed_issues}
                          type={"snyk"}
                          title={"Suppressed Issues"}
                          print={true}
                        />
                      </AntCol>
                      <AntCol span={24}>
                        <ReportTable data={report.new_vulns} title={"New Issues"} type={"snyk"} print={true} />
                      </AntCol>
                    </>
                  )}
                  {report.type === "TENABLE" && report.agg_by_status && (
                    <AntCol span={18}>
                      <ReportTable data={report.agg_by_status.OPEN} type={"TENABLE"} title={"Open"} />
                      <ReportTable data={report.agg_by_status.REOPENED} title={"Reopened"} type={"TENABLE"} />
                    </AntCol>
                  )}
                  {report.type === "TENABLE" && !report.agg_by_status && (
                    <AntCol span={18}>No status data found</AntCol>
                  )}
                </div>
              </AntRow>
            </>
          );
        })}
      </div>
    </div>
  );
});
