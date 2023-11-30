import React from "react";
import { AntRow, AntCol, AntTitle } from "shared-resources/components";
import { ReportSummary, ReportTable } from "configurations/components/reports";
import { Divider } from "antd";
import ReactJson from "react-json-view";
import "./report-print.css";
import { get } from "lodash";
import CSVReportTable from "../../pages/plugin-results/plugin-result-report-table/PluginResultReportTable";
import DetailsCardComponent from "configurations/pages/plugin-results/plugin-results-details/DetailsCardComponent";

export const ReportPrintContainer = React.forwardRef((props: any, ref) => {
  const { reports } = props;
  const getSummary = (report: any) => {
    if (report.tool === "report_praetorian") {
      let summary: any = {};
      Object.keys(get(report, ["results", "summary"], {})).forEach(item => {
        delete report.results.summary[item].total_issues;
        let issues: any = {};
        Object.keys(report.results.summary[item]).forEach((issue: any) => {
          issues[issue.replace("total_", "")] = report.results.summary[item][issue];
        });
        summary[item] = { issues_by_severity: issues };
      });
      return {
        summary: summary,
        details: get(report, ["results", "metadata"], {})
      };
    } else {
      return {
        summary: get(report, ["results", "aggregations"], {}),
        details: get(report, ["results", "metadata"], {})
      };
    }
  };
  return (
    <div style={{ display: "none" }}>
      <div
        // @ts-ignore
        ref={ref}
        style={{ margin: "30px", breakBefore: "page" }}
        //className={"container"}
      >
        {reports.map((report: any) => {
          const summary = getSummary(report);
          if (report.class === "report_file" && report.successful === true) {
            return (
              <AntRow>
                <div style={{ height: "90vh", padding: "5vh", breakAfter: "page" }}>
                  <AntCol span={24}>
                    <AntTitle level={4}>{(report.plugin_name || "").replace(/_/g, " ").toUpperCase()}</AntTitle>
                  </AntCol>
                  <Divider />
                  <AntCol span={24}>
                    <DetailsCardComponent
                      productName={props.products}
                      resultData={report || {}}
                      handleOnEditClick={() => {}}
                      tagArray={props.tags}
                      print={true}
                    />
                  </AntCol>
                  <Divider />
                  <AntCol span={24}>
                    <ReportSummary data={summary} print={true} type={report.tool} />
                  </AntCol>
                </div>
                <div
                  style={{
                    //height: "100vh",
                    margin: "10px",
                    breakAfter: "page"
                  }}>
                  <AntCol span={24}>
                    {report.tool === "csv" ? (
                      <CSVReportTable resultData={report.results || {}} print={true} />
                    ) : (
                      <ReportTable
                        data={Object.keys(get(report, ["results", "data"], {})).map(record => ({
                          ...report.results.data[record],
                          id: record
                        }))}
                        print={true}
                        type={report.tool}
                      />
                    )}
                  </AntCol>
                </div>
              </AntRow>
            );
          } else {
            return (
              <div
                style={{
                  //height: "100vh",
                  padding: "5vh",
                  breakAfter: "page"
                }}>
                <ReactJson src={report.results} name={report.name} sortKeys={true} />
              </div>
            );
          }
        })}
      </div>
    </div>
  );
});
