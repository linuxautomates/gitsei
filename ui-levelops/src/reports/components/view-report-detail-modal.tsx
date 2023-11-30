import { Descriptions, Modal, Typography } from "antd";
import { get } from "lodash";
import React, { useMemo } from "react";
import { AntCol, AntParagraph, AntRow, AntText } from "shared-resources/components";
import { tableCell } from "utils/tableUtils";

interface ViewReportDetailModalProps {
  visible: any;
  onOk: () => void;
  onCancel: () => void;
  report: any;
}

const ViewReportDetailModal: React.FC<ViewReportDetailModalProps> = (props: ViewReportDetailModalProps) => {
  const { report, visible, onOk, onCancel } = props;

  const reportDetail = useMemo(() => {
    let details: any = {};
    let meta: any = {};
    let ignored: any = {};

    if (report.type === "SNYK") {
      details = get(report, "suppressed_issues", []).find((record: any) => record.id === visible);
      details.cve = get(details, ["identifiers", "CVE"], []).join(",");
      details.cwe = get(details, ["identifiers", "CWE"], []).join(",");
      meta = {
        version: details.version,
        severity: details.severity,
        type: details.type,
        language: details.language,
        exploit: details.exploitMaturity,
        cve: get(details, ["identifiers", "CVE"], []).join(","),
        cwe: get(details, ["identifiers", "CWE"], []).join(",")
      };
      if (details && details.ignored && details.ignored.length > 0) {
        ignored.by = details.ignored[0].ignoredBy.name;
        ignored.email = details.ignored[0].ignoredBy.email;
        ignored.reason = details.ignored[0].reason;
        ignored.reason_type = details.ignored[0].reasonType;
        ignored.date = tableCell("created_at", details.ignored[0].created / 1000);
        ignored.expires = tableCell("created_at", details.ignored[0].expires / 1000);
      }
    } else if (report.type === "TENABLE") {
      details = [...get(report, "agg_by_status.OPEN", []), ...get(report, "agg_by_status.REOPENED", [])].find(
        record => record.id === visible
      );

      meta = {
        type: details.vuln_type,
        severity: details.vuln_severity,
        cve: get(details, "cve", []).join(","),
        ips: get(details, "ips", []),
        fqdns: get(details, "fqdns", [])
      };
    }
    return { details, meta, ignored };
  }, [report]);

  const { details, meta, ignored } = reportDetail;

  return (
    <Modal title={"Issue Details"} visible={!!visible} onOk={onOk} onCancel={onCancel} width={"700px"} footer={null}>
      <AntRow gutter={[10, 10]}>
        <AntCol span={24}>
          <>
            <AntParagraph>{visible}</AntParagraph>
            <AntText type={"secondary"}>{report?.type === "TENABLE" ? details?.asset_name : details?.title}</AntText>
          </>
        </AntCol>
        <AntCol span={24}>
          <Descriptions title={"MORE INFO"}>
            <Descriptions.Item>
              {report?.type !== "TENABLE" && (
                <AntText copyable>
                  <a href={details?.url} target={"_blank"}>
                    {details?.url}
                  </a>
                </AntText>
              )}
              {report?.type === "TENABLE" && !!details?.vuln_desciption && (
                <Typography.Paragraph ellipsis={{ rows: 3, expandable: true }}>
                  {details?.vuln_desciption}
                </Typography.Paragraph>
              )}
            </Descriptions.Item>
          </Descriptions>
        </AntCol>
        {report?.type !== "TENABLE" && (
          <AntCol span={24}>
            <Descriptions
              title={"DETAILS"}
              layout={"vertical"}
              bordered={false}
              column={3}
              colon={false}
              size={"small"}>
              {Object.keys(meta || {}).map(item => (
                <Descriptions.Item label={<AntText type={"secondary"}>{(item || "").toUpperCase()}</AntText>}>
                  {meta[item]}
                </Descriptions.Item>
              ))}
            </Descriptions>
          </AntCol>
        )}
        {report?.type === "TENABLE" && (
          <AntCol span={24}>
            <Descriptions
              title={"DETAILS"}
              layout={"vertical"}
              bordered={false}
              column={3}
              colon={false}
              size={"small"}>
              {Object.keys(meta || {}).map(item => (
                <Descriptions.Item label={<AntText type={"secondary"}>{(item || "").toUpperCase()}</AntText>}>
                  {["ips", "fdns"].includes(item) && meta[item] && meta[item].length >= 0 && meta[item][0]}
                  {!["ips", "fdns"].includes(item) && meta[item]}
                </Descriptions.Item>
              ))}
            </Descriptions>
          </AntCol>
        )}
        {report.type !== "TENABLE" && details?.ignored?.length > 0 && (
          <AntCol span={24}>
            <Descriptions
              title={"IGNORED"}
              layout={"vertical"}
              bordered={false}
              column={3}
              colon={false}
              size={"small"}>
              {Object.keys(ignored || {}).map(item => (
                <Descriptions.Item label={<AntText type={"secondary"}>{(item || "").toUpperCase()}</AntText>}>
                  {ignored[item]}
                </Descriptions.Item>
              ))}
            </Descriptions>
          </AntCol>
        )}
      </AntRow>
    </Modal>
  );
};

export default React.memo(ViewReportDetailModal);
