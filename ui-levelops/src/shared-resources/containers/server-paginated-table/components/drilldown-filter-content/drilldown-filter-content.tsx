import React, { useMemo, useState } from "react";
import { AntText, SvgIcon } from "shared-resources/components";
import { Button, Dropdown, Tooltip } from "antd";
import "./drilldown-filter-content.scss";
import DrilldownColumnSelecor from "./DrilldownColumnSelector";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
interface DisplayColumnSelectorProps {
  availableColumns: Array<{ title: string; dataIndex: string }>;
  visibleColumns: Array<any>;
  widgetId: string;
  defaultColumns: Array<any>;
}

interface DrilldownFilterContentProps {
  drilldownHeaderProps: any;
  downloadCSV?: any;
  handleCSVDownload?: any;
  displayColumnSelector?: DisplayColumnSelectorProps;
  setSelectedColumns?: (selectedColumns: Array<string>) => void;
}

const DrillDownFilterContent: React.FC<DrilldownFilterContentProps> = (props: DrilldownFilterContentProps) => {
  const { downloadCSV, handleCSVDownload, drilldownHeaderProps, displayColumnSelector } = props;

  const { title, type, showTitle, onOpenReport, onDrilldownClose } = drilldownHeaderProps;
  const [showColumnList, setShowColumnList] = useState<boolean>(false);

  const drilldownColumnSelector = useMemo(() => {
    if (!displayColumnSelector) {
      return null;
    }
    const { visibleColumns, availableColumns, widgetId, defaultColumns } = displayColumnSelector;
    return (
      <Dropdown
        overlay={
          <DrilldownColumnSelecor
            visibleColumns={visibleColumns}
            availableColumns={availableColumns}
            widgetId={widgetId}
            closeDropdown={() => setShowColumnList(false)}
            defaultColumns={defaultColumns}
            onColumnsChange={props.setSelectedColumns}
          />
        }
        trigger={["click"]}
        placement="bottomRight"
        visible={showColumnList}
        onVisibleChange={setShowColumnList}>
        <Tooltip title="Select Columns">
          <Button className="drilldown-icon">
            <div className="icon-wrapper">
              <SvgIcon className="reports-btn-icon" icon="selectColumn" />
            </div>
          </Button>
        </Tooltip>
      </Dropdown>
    );
  }, [displayColumnSelector, showColumnList, setShowColumnList]);

  return (
    <div className="drilldown-filter-content">
      {showTitle && (
        <>
          <AntText className="drilldown-type">{type}</AntText>
          {typeof title === "string" && title.length > 0 && (
            <>
              <span className="circle-separator">{`● `}</span>
              <AntText className="drilldown-title">{title}</AntText>
            </>
          )}
        </>
      )}
      {drilldownColumnSelector}
      {downloadCSV && (
        <Tooltip title="Download">
          <Button onClick={handleCSVDownload} className="drilldown-icon">
            <div className="icon-wrapper">
              <SvgIcon className="reports-btn-icon" icon="download" />
            </div>
          </Button>
        </Tooltip>
      )}
      {onOpenReport && (
        <Tooltip title="Open Report">
          <Button onClick={onOpenReport} className="drilldown-icon">
            <div className="icon-wrapper">
              <SvgIcon className="reports-btn-icon" icon="externalLink" />
            </div>
          </Button>
        </Tooltip>
      )}
      <Tooltip title="Close">
        <Button onClick={onDrilldownClose} className="drilldown-icon close-button">
          <div className="icon-wrapper">
            <SvgIcon className="reports-btn-icon" icon="closeNew" />
          </div>
        </Button>
      </Tooltip>
    </div>
  );
};

export default DrillDownFilterContent;
