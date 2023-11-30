import React from "react";
import * as PropTypes from "prop-types";
import { AntCard, AntCol, AntRow, AntStatistic, IntegrationIcon } from "shared-resources/components";
import { LineChart } from "shared-resources/charts";
import { Divider } from "antd";
import { get } from "lodash";

import moment from "moment";

export class TenableComponent extends React.PureComponent {
  constructor(props) {
    super(props);

    this.state = {};
  }

  get stats() {
    const { report } = this.props;

    const totalCount = Object.values(get(report, "agg_by_severity", [])).reduce(
      (total, next = []) => total + next.length,
      0
    );

    return (
      <AntRow gutter={[10, 10]} type={"flex"} justify={"start"}>
        <AntCol span={6}>
          <AntCol span={24}>
            <AntStatistic title={"TOTAL VULNERABILITIES"} value={totalCount} />
          </AntCol>
        </AntCol>
        <AntCol span={1}>
          <Divider type="vertical" className="h-100" />
        </AntCol>
        <AntCol span={8}>
          <AntCol span={8}>
            <AntStatistic title={"HIGH"} value={get(report, "agg_by_severity.high", []).length} />
          </AntCol>
          <AntCol span={8}>
            <AntStatistic title={"MEDIUM"} value={get(report, "agg_by_severity.medium", []).length} />
          </AntCol>
          <AntCol span={8}>
            <AntStatistic title={"LOW"} value={get(report, "agg_by_severity.low", []).length} />
          </AntCol>
        </AntCol>
        <AntCol span={1}>
          <Divider type="vertical" className="h-100" />
        </AntCol>
        <AntCol span={8}>
          <AntCol span={8}>
            <AntStatistic title={"OPEN"} value={get(report, "agg_by_status.OPEN", []).length} />
          </AntCol>
          <AntCol span={8}>
            <AntStatistic title={"REOPEN"} value={get(report, "agg_by_status.REOPENED", []).length} />
          </AntCol>
        </AntCol>
      </AntRow>
    );
  }

  get linegraph() {
    const { report } = this.props;

    if (!Object.keys(report).includes("agg_by_status_time_series")) {
      return "";
    }

    const graphData = Object.keys(report.agg_by_status_time_series).map(time => {
      const timestamp = moment.unix(parseInt(time)).format("MM/DD");
      return {
        name: timestamp,
        open: get(report, `agg_by_status_time_series[${time}].OPEN`, []),
        reopened: get(report, `agg_by_status_time_series[${time}].REOPENED`, [])
      };
    });

    return (
      <AntRow gutter={[10, 10]} type={"flex"} justify={"start"}>
        <AntCol span={24}>
          <div style={{ paddingBottom: "20px", height: "300px" }}>
            <LineChart
              data={graphData}
              showDefaultLegend
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
      <AntCard title={`Tenable ( ${this.props.product} )`} extra={<IntegrationIcon type="tenable" size={"large"} />}>
        {this.stats}
        {this.linegraph}
      </AntCard>
    );
  }
}

TenableComponent.propTypes = {
  report: PropTypes.object
};
