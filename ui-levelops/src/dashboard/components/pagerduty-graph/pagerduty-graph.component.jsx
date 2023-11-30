import React from "react";
import { Row, Col } from "antd";
import { AntCard, IntegrationIcon } from "shared-resources/components";

export const PagerDutyComponent = props => {
  return (
    <AntCard
      title={`PagerDuty ( ${props.product} )`}
      extra={<IntegrationIcon type="pagerduty" style={{ width: "auto", height: "30px" }} />}>
      <Row type={"flex"} justify={"space-between"} gutter={[0, 10]}>
        <Col span={24}>{JSON.stringify(props.report, null, 4)}</Col>
      </Row>
    </AntCard>
  );
};
