import { Form, Icon, Popconfirm } from "antd";
import Checkbox, { CheckboxChangeEvent } from "antd/lib/checkbox";
import { get } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { AntIcon, AntText, AntTooltip, NewCustomFormItemLabel } from "shared-resources/components";

interface GroupByModuleFilterProps {
  report: string;
  filterProps: LevelOpsFilter;
  handleMetadataChange?: (value: any, key: any) => void;
}
const GroupByModuleFilterComponent: React.FC<GroupByModuleFilterProps> = props => {
  const { filterProps, handleMetadataChange, report } = props;
  const { beKey, metadata, apiFilterProps } = filterProps;

  const groupByRootFolderValue = useMemo(() => {
    const filterKey = `${beKey}_${report || ""}`;
    return get(metadata, [filterKey], true);
  }, [metadata, report]);

  const handleChange = (checked: boolean) => {
    const filterKey = `${beKey}_${report || ""}`;
    handleMetadataChange?.(checked, filterKey);
  };

  const handleFilterDelete = () => {
    const filterKey = `${beKey}_${report || ""}`;
    handleMetadataChange?.(undefined, filterKey);
  };

  const deleteIconStyle = useMemo(
    () => ({
      fontSize: "15px",
      margin: "0 0.5rem"
    }),
    []
  );
  return (
    <Form.Item>
      <div className="flex align-center justify-space-between">
        <div>
          <Checkbox
            checked={groupByRootFolderValue}
            onChange={(e: CheckboxChangeEvent) => handleChange(e.target.checked)}
            className="text-uppercase"
            style={{
              fontSize: "12px",
              fontWeight: 700,
              color: "#575757"
            }}>
            <AntText style={{ marginRight: "4px" }}>{"Group By Modules"}</AntText>
            <AntTooltip title="Root folders are the top most folders in a file system">
              <Icon type="info-circle" />
            </AntTooltip>
          </Checkbox>
        </div>
      </div>
    </Form.Item>
  );
};

export default GroupByModuleFilterComponent;
