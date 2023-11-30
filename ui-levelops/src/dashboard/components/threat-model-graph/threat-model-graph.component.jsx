import React from "react";
import * as PropTypes from "prop-types";
import {
  AntCard,
  AntRow,
  AntCol,
  MultiProgressBar,
  AntStatistic,
  AntList,
  AntBadge,
  AntText,
  IntegrationIcon
} from "shared-resources/components";
import { Divider, List } from "antd";

const STATUSES = ["not_started", "not_applicable", "needs_investigation", "mitigation_implemented", "total_migrated"];

export class ThreatModelGraphComponent extends React.PureComponent {
  /*eslint-disable-next-line*/
  constructor(props) {
    super(props);
  }

  get statsBar() {
    const { report } = this.props;
    const sample = Object.keys(report.results.aggregations)[0];
    const priorityNumbers = {
      low: 0,
      medium: 0,
      high: 0,
      ...report.results.aggregations[sample].by_priority
    };
    const total = Object.keys(priorityNumbers).reduce((acc, curr) => {
      return acc + priorityNumbers[curr];
    }, 0);
    return (
      <AntRow gutter={[10, 10]} type={"flex"} justify={"start"}>
        <AntCol span={8}>
          <AntStatistic title={"TOTAL THREATS"} value={total} />
        </AntCol>
        <AntCol span={1}>
          <Divider type="vertical" className="h-100" />
        </AntCol>
        {Object.keys(priorityNumbers).map(priority => (
          <AntCol span={5}>
            <AntStatistic title={priority.toUpperCase()} value={priorityNumbers[priority]} />
          </AntCol>
        ))}
        <AntCol span={24}>
          <MultiProgressBar {...priorityNumbers} />
        </AntCol>
      </AntRow>
    );
  }

  get statusList() {
    const { aggregations } = this.props.report.results;
    const sample = Object.keys(aggregations)[0];
    const reportStatus = aggregations[sample].by_state;

    const statuses = STATUSES.map(status => ({ status: status, count: reportStatus ? reportStatus[status] || 0 : 0 }));

    return (
      <>
        <AntText type={"secondary"}>THREATS BY STATUS</AntText>
        <AntList
          className="ant-list-bottom-border"
          dataSource={statuses}
          itemLayout="horizontal"
          renderItem={item => (
            <List.Item actions={[<AntBadge count={item.count} style={{ backgroundColor: "#2968dc" }} />]}>
              {item.status.replace(/_/g, " ").toUpperCase()}
            </List.Item>
          )}
        />
      </>
    );
  }

  render() {
    return (
      <AntCard
        title={`Microsoft Threat Model ( ${this.props.product} )`}
        extra={<IntegrationIcon type={"microsoft"} size={"medium"} />}>
        <AntRow gutter={[20, 20]}>
          <AntCol span={12}>{this.statsBar}</AntCol>
          <AntCol span={12}>{this.statusList}</AntCol>
        </AntRow>
      </AntCard>
    );
  }
}

ThreatModelGraphComponent.propTypes = {
  report: PropTypes.object
};
