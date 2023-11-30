import React from "react";
import { AntCard, AntRow, AntCol, AntText } from "shared-resources/components";
import { Statistic } from "antd";

interface ReportSummaryComponentProps {
  data: any;
  print?: boolean;
  type?: any;
}

const ReportSummaryComponent: React.FC<ReportSummaryComponentProps> = (props: ReportSummaryComponentProps) => {
  const buildStatsCard = (title: any, values: any) => {
    const spanWidth = Math.max(24 / Object.keys(values).length, 8); // this cannot be more than 4, thats the issue right now
    const displayTitle = (title: any) => {
      title = title?.replace(/_/g, " ").toUpperCase();
      if (title.length > 8 && !title.includes(" ") && !title.includes("-")) {
        let ar = title.split("");
        ar.splice(8, 0, "-");
        title = ar.join("");
      }
      return title;
    };
    return (
      <AntCol span={24}>
        <div>
          <div style={{ marginBottom: "10px" }}>
            <AntText strong style={{ textTransform: "capitalize" }}>
              {title.replace(/_/g, " ")}
            </AntText>
          </div>
          <AntCard style={{ marginTop: "10px", maxHeight: "1000px" }}>
            <AntRow type={"flex"} gutter={[10, 10]}>
              {Object.keys(values).map(value => (
                <AntCol span={spanWidth}>
                  <Statistic title={displayTitle(value)} value={values[value]} />
                </AntCol>
              ))}
            </AntRow>
          </AntCard>
        </div>
      </AntCol>
    );
  };

  const buildPrintStatsCard = (title: any, values: any) => {
    const spanWidth = Math.max(24 / Object.keys(values).length, 8); // this cannot be more than 4, thats the issue right now
    return (
      <AntCol span={24}>
        <div>
          <div style={{ marginBottom: "10px" }}>
            <AntText strong style={{ textTransform: "capitalize" }}>
              {title.replace(/_/g, " ")}
            </AntText>
          </div>
          <div className={`border-allaround`} style={{ padding: "10px" }}>
            <AntRow gutter={[10, 10]}>
              {Object.keys(values).map(value => (
                <AntCol span={spanWidth}>
                  <Statistic title={value.replace(/_/g, " ").toUpperCase()} value={values[value]} />
                </AntCol>
              ))}
            </AntRow>
          </div>
        </div>
      </AntCol>
    );
  };

  const buildDetailsCard = (details: any) => {
    return (
      <AntCol span={24}>
        <AntRow gutter={[10, 10]}>
          {Object.keys(details).map(detail => (
            <AntCol span={12}>
              <Statistic
                title={detail.replace(/_/g, " ").toUpperCase()}
                value={details[detail]}
                valueStyle={{ fontSize: "12px" }}
              />
            </AntCol>
          ))}
        </AntRow>
      </AntCol>
    );
  };

  const { data, print, type } = props;
  let summary = {};

  if (data.summary !== undefined && type !== "report_nccgroup") {
    Object.keys(data.summary).forEach(item => {
      Object.keys(data.summary[item]).forEach((label: any) => {
        //@ts-ignore
        summary[`${item}_${label}`] = data.summary[item][label];
      });
    });
  } else {
    summary = data.summary;
  }

  return (
    <>
      <AntRow gutter={[10, 20]} type={"flex"}>
        {Object.keys(summary).map((item: any) =>
          //@ts-ignore
          print === true ? buildPrintStatsCard(item, summary[item]) : buildStatsCard(item, summary[item])
        )}
        {buildDetailsCard(data.details)}
      </AntRow>
    </>
  );
};

export default React.memo(ReportSummaryComponent);
