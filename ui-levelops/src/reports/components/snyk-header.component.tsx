import React, { useMemo } from "react";
import { AntCol, AntRow, AntText, AntTitle } from "shared-resources/components";
import { tableCell } from "utils/tableUtils";

interface SnykHeaderProps {
  type?: string;
  products?: any;
  integrations?: any;
  created_at?: string | number;
}

const SnykHeaderComponent: React.FC<SnykHeaderProps> = (props: SnykHeaderProps) => {
  const { type, products, integrations, created_at } = props;

  return (
    <AntRow gutter={[10, 10]} justify={"space-between"}>
      <AntCol span={24}>
        <AntCol span={12}>
          <AntTitle level={4}>{`${type} REPORT`}</AntTitle>
          {created_at && <AntText type={"secondary"}>Created on {tableCell("created_at", created_at)}</AntText>}
        </AntCol>
        <AntCol span={12}>
          {products !== "UNKNOWN" && (
            <div className="text-end">
              <AntText strong>Project: {products}</AntText>
            </div>
          )}
          {integrations !== "UNKNOWN" && (
            <div className="text-end">
              <AntText type={"secondary"}>Integrations: {integrations}</AntText>
            </div>
          )}
        </AntCol>
      </AntCol>
    </AntRow>
  );
};

export default React.memo(SnykHeaderComponent);
