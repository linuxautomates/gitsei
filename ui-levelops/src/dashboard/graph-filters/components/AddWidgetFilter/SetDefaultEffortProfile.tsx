import { useTicketCategorizationFilters } from "custom-hooks";
import { get } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useEffect } from "react";

interface SetDefaultEffortProfileProps {
  report: string;
  filters: any;
  filterConfig: LevelOpsFilter;
  onFilterValueChange: (value: any, type?: any) => void;
}

const SetDefaultEffortProfile: React.FC<SetDefaultEffortProfileProps> = (props: SetDefaultEffortProfileProps) => {
  const { report, filters, filterConfig, onFilterValueChange } = props;
  const { beKey } = filterConfig;
  const { apiData } = useTicketCategorizationFilters(report, [report]);
  const ticketCategorizationSchemesValue = get(filters, [beKey], undefined);

  useEffect(() => {
    const defaultScheme = apiData.filter((data: any) => data?.default_scheme)[0];
    if (defaultScheme && !ticketCategorizationSchemesValue) {
      onFilterValueChange(defaultScheme.id, beKey);
    }
  }, [apiData, ticketCategorizationSchemesValue]);
  return null;
};

export default React.memo(SetDefaultEffortProfile);
