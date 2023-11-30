import React, { useMemo } from "react";
import { Col, Row, Table, Typography } from "antd";

interface TriageTotalsProps {
  dataSource: any;
  columns: any;
}

const { Text } = Typography;

const TriageTotals: React.FC<TriageTotalsProps> = (props: TriageTotalsProps) => {
  const scroll = useMemo(() => {
    return { x: "fit-content" };
  }, []);

  return (
    <Row>
      <Col span={24} className="my-10">
        <Text strong>Daily Totals</Text>
      </Col>
      <Col span={24}>
        <Table dataSource={props.dataSource} columns={props.columns} scroll={scroll} bordered pagination={false} />
      </Col>
    </Row>
  );
};

export default React.memo(TriageTotals);
