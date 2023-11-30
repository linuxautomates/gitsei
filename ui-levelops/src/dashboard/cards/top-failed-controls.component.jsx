import React from "react";
import { Icon, List } from "antd";

import { AntCard } from "shared-resources/components";
import SeverityTag from "shared-resources/components/severity-tag/severeity-tag.component";
import ViewDetails from "dashboard/common/view-details.component";
import { getBaseUrl } from 'constants/routePaths'

const TopFailedControls = props => {
  const dataSource = props.data.map(record => ({
    text: record.signature,
    severity: record.severity
  }));
  return (
    <AntCard
      className="ant-list-unbordered"
      title={
        <span>
          {"Top Failed Controls "}
          <Icon style={{ fontSize: "1.2rem", color: "#7F8FA4" }} type="info-circle" theme="filled" />
        </span>
      }
      actions={[<ViewDetails linkTo={`${getBaseUrl()}/results?product=${props.productId}`} />]}>
      <List
        dataSource={dataSource}
        itemLayout="vertical"
        size="small"
        split={false}
        renderItem={item => <List.Item extra={<SeverityTag severity={item.severity} />}>{item.text}</List.Item>}
      />
    </AntCard>
  );
};

export default TopFailedControls;
