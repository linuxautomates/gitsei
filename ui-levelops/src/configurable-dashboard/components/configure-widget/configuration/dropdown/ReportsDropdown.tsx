import React, { useCallback, useMemo } from "react";

import { AntSelect } from "../../../../../shared-resources/components";
import { useIntegrationList } from "../../../../../custom-hooks/useIntegrationList";
import { reportOptionList } from "../../../../../utils/reportListUtils";
import Loader from "../../../../../components/Loader/Loader";

interface ReportsDropdownProps {
  mode?: string;
  dashboardId: string;
  widgetType: string;
  className: string;
  value: any;
  onChange: any;
  placeHolder?: string;
}

const ReportsDropdown: React.FC<ReportsDropdownProps> = ({
  mode,
  className,
  value,
  onChange,
  widgetType,
  dashboardId,
  placeHolder = "Select A Report"
}) => {
  const [integrationsLoading, integrationList] = useIntegrationList({ dashboardId }, [dashboardId]);

  const handleReportFilter = useCallback((value: string, option: { label: string; value: string }) => {
    return (option?.label as string)?.toLowerCase().includes(value?.toLowerCase());
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // @ts-ignore
  const reportOptions = useMemo(() => reportOptionList(widgetType, integrationList), [widgetType, integrationList]); // eslint-disable-line react-hooks/exhaustive-deps

  if (integrationsLoading) {
    return <Loader />;
  }

  return (
    <AntSelect
      autoFocus
      mode={mode ? mode : "default"}
      className={className}
      value={value ? value : undefined}
      placeholder={placeHolder}
      options={reportOptions}
      showSearch
      showArrow
      onOptionFilter={handleReportFilter}
      onChange={onChange}
    />
  );
};

export default ReportsDropdown;
