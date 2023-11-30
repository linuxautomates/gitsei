import React from "react";
import * as PropTypes from "prop-types";
import { AntCard, AntRow, AntCol, AntStatistic, IntegrationIcon } from "shared-resources/components";
import { LineChart } from "shared-resources/charts";
import { Divider } from "antd";

import moment from "moment";

export class SnykTimeSeriesComponent extends React.PureComponent {
  get stats() {
    const { report } = this.props;

    return (
      <AntRow gutter={[10, 10]} type={"flex"} justify={"start"}>
        <AntCol span={8}>
          <AntCol span={12}>
            <AntStatistic title={"TOTAL VULNERABILITIES"} value={report?.total_vuln_count} />
          </AntCol>
          <AntCol span={12}>
            <AntStatistic title={"SUPPRESSED VULNERABILITIES"} value={report?.suppressed_issues?.length} />
          </AntCol>
        </AntCol>
        <AntCol span={1}>
          <Divider type="vertical" className="h-100" />
        </AntCol>
        <AntCol span={8}>
          <AntCol span={8}>
            <AntStatistic title={"HIGH"} value={report?.agg_by_severity.high?.vulns_found} />
          </AntCol>
          <AntCol span={8}>
            <AntStatistic title={"MEDIUM"} value={report?.agg_by_severity.medium?.vulns_found} />
          </AntCol>
          <AntCol span={8}>
            <AntStatistic title={"LOW"} value={report?.agg_by_severity.low?.vulns_found} />
          </AntCol>
        </AntCol>
      </AntRow>
    );
  }

  get linegraph() {
    const { report } = this.props;

    if (!Object.keys(report).includes("agg_by_severity_time_series")) {
      return "";
    }

    const graphData = Object.keys(report.agg_by_severity_time_series).map(time => {
      const timestamp = moment.unix(parseInt(time)).format("MM/DD");
      return {
        name: timestamp,
        high: report.agg_by_severity_time_series[time].high?.vulns_found,
        medium: report.agg_by_severity_time_series[time].medium?.vulns_found,
        low: report.agg_by_severity_time_series[time]?.low?.vulns_found
      };
    });
    return (
      <AntRow gutter={[10, 10]} type={"flex"} justify={"start"}>
        <AntCol span={24}>
          <div style={{ paddingBottom: "20px", height: "300px" }}>
            <LineChart
              data={graphData}
              showDefaultLegend={true}
              hideLegend
              chartProps={{
                margin: { top: 20, right: 20, left: 20, bottom: 20 }
              }}
            />
          </div>
        </AntCol>
      </AntRow>
    );
  }

  render() {
    return (
      <AntCard title={`Snyk ( ${this.props.product} )`} extra={<IntegrationIcon type="snyk" size={"large"} />}>
        {this.stats}
        {this.linegraph}
      </AntCard>
    );
  }
}

SnykTimeSeriesComponent.propTypes = {
  report: PropTypes.object
};
