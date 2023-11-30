import { ReportTable } from "configurations/components/reports";
import React, { useMemo, useRef, useState } from "react";
import { useReactToPrint } from "react-to-print";
import { AntButton, AntCol, AntRow } from "shared-resources/components";
import { SnykHeader, SnykSummary } from ".";
import { SnykPrintReportComponent } from "./snyk-print-report.component";
import ViewReportDetailModal from "./view-report-detail-modal";

interface SnykReportProps {
  report: any;
}

const SnykReport: React.FC<SnykReportProps> = (props: SnykReportProps) => {
  const { report } = props;

  const [viewDetails, setViewDetails] = useState<string | undefined>();

  const reportRef = useRef<HTMLDivElement | null>(null);
  const handlePrint = useReactToPrint({
    content: () => reportRef.current,
    copyStyles: true
  });

  const printReportDiv = useMemo(() => <SnykPrintReportComponent reports={[report]} ref={reportRef} />, [report]);

  return (
    <>
      {!!viewDetails && (
        <ViewReportDetailModal
          report={report}
          visible={viewDetails}
          onCancel={() => setViewDetails(undefined)}
          onOk={() => setViewDetails(undefined)}
        />
      )}
      <div className="mb-10 text-right">
        <AntButton icon={"printer"} onClick={handlePrint}>
          Print Report
        </AntButton>
      </div>
      {printReportDiv}
      <>
        <SnykHeader
          products={report?.products}
          integrations={report?.integrations}
          type={report?.type}
          created_at={report?.created_at}
        />
        <AntRow gutter={[10, 10]}>
          <AntCol span={6}>
            <SnykSummary report={report} />
          </AntCol>
          {report.type === "SNYK" && (
            <AntCol span={18}>
              <ReportTable
                data={report.suppressed_issues}
                type={"snyk"}
                details={(id: any) => setViewDetails(id)}
                title={"Suppressed Issues"}
              />
              <ReportTable data={report.new_vulns} title={"New Issues"} type={"snyk"} />
            </AntCol>
          )}
          {report?.type === "TENABLE" && report?.agg_by_status && (
            <AntCol span={18}>
              <ReportTable
                data={report?.agg_by_status.OPEN}
                type={"TENABLE"}
                details={(id: any) => setViewDetails(id)}
                title={"Open"}
              />
              <ReportTable
                data={report?.agg_by_status.REOPENED}
                details={(id: any) => setViewDetails(id)}
                title={"Reopened"}
                type={"TENABLE"}
              />
            </AntCol>
          )}
          {report?.type === "TENABLE" && !report?.agg_by_status && <AntCol span={18}>No status data found</AntCol>}
        </AntRow>
      </>
    </>
  );
};

export default React.memo(SnykReport);
