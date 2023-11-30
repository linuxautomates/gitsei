import { Form } from "antd";
import { useSCMGlobalSettings } from "custom-hooks/useSCMGlobalSettings";
import { get } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import React from "react";
import { AntInput, AntSelect, AntText } from "shared-resources/components";
import "./../../components/SCMCodeFilters/SCMCodeFilters.scss";

interface SCMCodeFiltersWrapperProps {
  report: string;
  metadata: any;
  handleMetadataChange?: (value: any, type: any, reportType?: String) => void;
  filterProps: LevelOpsFilter;
}
const scmCodeChangeSizeUnits = [
  { label: "Lines Of Code", value: "lines" },
  { label: "Files", value: "files" }
];
const ScmCodeChangeFiltersWrapper: React.FC<SCMCodeFiltersWrapperProps> = (props: SCMCodeFiltersWrapperProps) => {
  const { settings } = useSCMGlobalSettings();

  const { handleMetadataChange: onMetadataChange, filterProps, metadata: metaData } = props;
  const { label, beKey } = filterProps;
  const unit = get(metaData, ["code_change_size_unit"], settings.code_change_size_unit);
  const unitText = unit === "lines" ? " lines of code" : " files";

  return (
    <>
      <Form.Item key={beKey} label={label}>
        <div className="code-change-size-container">
          <Form.Item key="code_change_size_unit" label={"Units"}>
            <AntSelect
              showArrow
              allowClear
              value={unit}
              options={scmCodeChangeSizeUnits}
              mode="single"
              onChange={(value: any, options: any) => onMetadataChange?.(value, "code_change_size_unit")}
            />
          </Form.Item>
          <Form.Item key="code-change-values" label={"Options"}>
            <div className="code-change-options">
              <AntText className="code-change-option">
                {"Small - less than "}
                <AntInput
                  type="number"
                  value={get(metaData, ["code_change_size_small"], settings.code_change_size_small)}
                  onChange={(e: number) => onMetadataChange?.(e, "code_change_size_small")}
                />
                {unitText}
              </AntText>
              <AntText className="code-change-option">
                {"Medium - less than "}
                <AntInput
                  type="number"
                  value={get(metaData, ["code_change_size_medium"], settings.code_change_size_medium)}
                  onChange={(e: number) => onMetadataChange?.(e, "code_change_size_medium")}
                />
                {unitText}
              </AntText>
              <AntText className="code-change-option">{"Large - everything else"}</AntText>
            </div>
          </Form.Item>
        </div>
      </Form.Item>
    </>
  );
};

export default ScmCodeChangeFiltersWrapper;
