import { Avatar, Descriptions, Modal } from "antd";
import { ReportSummary, ReportTable } from "configurations/components/reports";
import { ReportPrint } from "configurations/containers/reports";
import { get, unset } from "lodash";
import React, { useCallback, useMemo, useRef, useState } from "react";
import { useReactToPrint } from "react-to-print";
import { AntRow, AntCol, AntButton, AntTitle } from "shared-resources/components";
import CSVReportTable from "../../plugin-result-report-table/PluginResultReportTable";
import DetailsCardComponent from "../../plugin-results-details/DetailsCardComponent";

interface ReportsDetailPageProps {
  pluginResultsState: any;
  productName: string;
  selectedTags: any[];
  onEditTagClick: () => void;
}

const ReportsDetailPage: React.FC<ReportsDetailPageProps> = (props: ReportsDetailPageProps) => {
  const { pluginResultsState, productName, selectedTags, onEditTagClick } = props;
  const [viewDetailsId, setViewDetailsId] = useState<string>("");
  const reportRef = useRef(null);
  const error = get(pluginResultsState, ["error"], false);

  const handlePrint = useReactToPrint({ content: () => reportRef.current, copyStyles: true });

  const report = get(pluginResultsState, ["data"], {});
  if (report.products) {
    report.products = productName;
  }

  const viewDetails = useCallback((id: string) => {
    setViewDetailsId(id);
  }, []);

  const detailsModal = () => {
    const details = get(report, ["results", "data", viewDetailsId], {});

    return (
      <Modal
        title={viewDetailsId}
        visible={viewDetailsId !== ""}
        onOk={(e: any) => setViewDetailsId("")}
        onCancel={(e: any) => setViewDetailsId("")}
        width={"700px"}
        footer={null}>
        <AntRow>
          {report?.tool === "report_praetorian" && (
            <AntCol span={24}>
              <AntRow gutter={[10, 10]}>
                {Object.keys(details?.meta?.score).map((score: any) => {
                  return (
                    <>
                      <AntCol span={6}>
                        <span className="mr-10">{score.replace(/_/g, " ")}</span>
                      </AntCol>
                      <AntCol span={4}>
                        <Avatar>{details?.meta?.score[score]?.value}</Avatar>
                      </AntCol>
                    </>
                  );
                })}
              </AntRow>
            </AntCol>
          )}
          {Object.keys(details).map(detail => {
            if (detail !== "meta") {
              return (
                <AntCol span={24}>
                  <Descriptions title={detail.replace(/_/g, " ").toUpperCase()}>
                    <Descriptions.Item>{details[detail]}</Descriptions.Item>
                  </Descriptions>
                </AntCol>
              );
            }
          })}
        </AntRow>
      </Modal>
    );
  };

  const summary = useMemo(() => {
    if (report?.tool === "report_praetorian") {
      let summary: any = {};
      Object.keys(report?.results?.summary || {}).forEach((item: any) => {
        unset(report, ["results", "summary", item, "total_issues"]);
        let issues: any = {};
        const data = get(report, ["results", "summary", item], {});
        Object.keys(data).forEach(issue => {
          issues[issue.replace("total_", "")] = report?.results?.summary[item][issue];
        });
        summary[item] = { issues_by_severity: issues };
      });
      return {
        summary: summary || {},
        details: report?.results?.metadata || {}
      };
    } else {
      return {
        summary: report?.results?.aggregations || {},
        details: report?.results?.metadata || {}
      };
    }
  }, [report]);

  const rowStyle = useMemo(() => ({ marginTop: "1rem" }), []);
  const errorDivStyle = useRef({
    display: "flex",
    justifyContent: "center",
    border: "1px solid #d7d7d7",
    alignItems: "center",
    height: "200px"
  });

  return (
    <>
      {viewDetailsId && detailsModal()}
      {!error && (
        <div className="mb-10 text-end">
          <AntButton icon={"printer"} onClick={handlePrint}>
            Print Report
          </AntButton>
        </div>
      )}
      {
        //@ts-ignore
        <ReportPrint reports={[report]} ref={reportRef} tags={selectedTags} products={productName} />
      }
      <div>
        <AntRow gutter={[10, 10]} justify={"space-between"}>
          <AntCol span={24}>
            <AntTitle level={4}>{(report.plugin_name || "").replace(/_/g, " ").toUpperCase()}</AntTitle>
          </AntCol>
        </AntRow>
        <AntRow gutter={[10, 10]}>
          <AntCol span={8}>
            <AntRow>
              <DetailsCardComponent
                productName={productName}
                resultData={report || {}}
                handleOnEditClick={onEditTagClick}
                tagArray={selectedTags}
                print={false}
              />
            </AntRow>
            {Object.keys(summary?.summary).length > 0 && (
              <AntRow style={rowStyle}>
                <ReportSummary data={summary} type={report.tool} />
              </AntRow>
            )}
          </AntCol>
          <AntCol span={16}>
            {!error ? (
              <>
                {report.tool === "csv" ? (
                  <div style={{ minWidth: "48rem", maxWidth: "70rem" }}>
                    <CSVReportTable resultData={report.results || {}} print={false} />
                  </div>
                ) : (
                  <ReportTable
                    data={Object.keys(get(report, ["results", "data"], {})).map(record => ({
                      ...report.results.data[record],
                      id: record
                    }))}
                    type={report.tool}
                    details={viewDetails}
                  />
                )}
              </>
            ) : (
              <div style={errorDivStyle.current}>
                <AntTitle type={"secondary"} level={2}>
                  Failed to load Report
                </AntTitle>
              </div>
            )}
          </AntCol>
        </AntRow>
      </div>
    </>
  );
};

export default ReportsDetailPage;
