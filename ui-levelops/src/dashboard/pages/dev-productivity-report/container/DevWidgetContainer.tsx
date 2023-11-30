import React, { useMemo } from "react";
import { ouScoreWidgetsConfig } from "dashboard/dashboard-types/engineerScoreCard.types";
import { AntButton, AntCard, AntInput } from "shared-resources/components";
import { Icon } from "antd";
import { scoreWidgetType } from "dashboard/pages/scorecard/constants";
import OUScoreTableComponent from "../component/OUScoreTableComponent";
import { useState } from "react";
import { useDispatch } from "react-redux";
import { genericTableCSVDownload } from "reduxConfigs/actions/restapi";
import { basicMappingType } from "dashboard/dashboard-types/common-types";

interface DevWidgetContainerProps {
  widget: ouScoreWidgetsConfig;
  filters: basicMappingType<any>;
  ou_uuid: string;
}

const DevWidgetContainer: React.FC<DevWidgetContainerProps> = ({ widget, filters, ou_uuid }) => {
  const [searchValue, setSearchvalue] = useState<string>("");
  const dispatch = useDispatch();

  const handleExportCSV = () => {
    dispatch(
      genericTableCSVDownload(
        widget?.uri ?? "",
        widget?.method ?? "",
        {
          columns: widget?.columns ?? [],
          transformer: widget?.csvTransformer,
          filters: filters as any
        },
        { ou_id: ou_uuid }
      )
    );
  };

  const renderContent = useMemo(() => {
    switch (widget?.widget_type) {
      case scoreWidgetType?.SCORE_TABLE:
        return <OUScoreTableComponent searchValue={searchValue} interval={filters?.filter?.interval} />;
    }
  }, [widget, searchValue]);

  const rendreWidgetExtra = useMemo(() => {
    return (
      <div className="dev-widget-extra">
        <AntInput
          placeholder="Search For Name"
          value={searchValue}
          onChange={(e: any) => setSearchvalue(e.target.value)}
        />
        <AntButton onClick={handleExportCSV}>
          <Icon type="download" />
        </AntButton>
      </div>
    );
  }, [searchValue]);

  return (
    <AntCard
      className="dev-widget-container"
      title={widget?.name}
      style={{
        height: widget?.height,
        width: widget?.width
      }}
      extra={rendreWidgetExtra}>
      {renderContent}
    </AntCard>
  );
};

export default DevWidgetContainer;
