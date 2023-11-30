import { SOMETHING_BAD_HAPPEN } from "constants/formWarnings";
import { get } from "lodash";
import React, { useMemo } from "react";
import { AntTable } from "shared-resources/components";
import { SCM_INTEGRATIONS_COLUMNS_CONFIGS } from "../../constants";

interface SCMIntegrationErrorProps {
  error: any;
}

const SCMIntegrationErrorComponent: React.FC<SCMIntegrationErrorProps> = ({ error }) => {
  const preflightChecks = useMemo(() => get(error, ["checks"]), [error]);
  return preflightChecks ? (
    <AntTable
      dataSource={preflightChecks}
      columns={SCM_INTEGRATIONS_COLUMNS_CONFIGS}
      pagination={false}
      bordered={true}
    />
  ) : (
    <div className="flex align-center justify-center">
      <h3>{get(error, ["exception"], SOMETHING_BAD_HAPPEN)}</h3>
    </div>
  );
};

export default SCMIntegrationErrorComponent;
