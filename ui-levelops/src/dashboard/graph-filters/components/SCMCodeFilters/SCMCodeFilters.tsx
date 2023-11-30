import React from "react";
import { Form } from "antd";
import { AntInput, AntSelect, AntText } from "shared-resources/components";
import { get } from "lodash";
import { scmCodeChangeSizeUnits } from "../Constants";
import "./SCMCodeFilters.scss";

interface SCMCodeFiltersProps {
  report: string;
  metaData: any;
  onMetadataChange?: (value: any, type: any, reportType?: String) => void;
  scmGlobalSettings?: any;
}

const SCMCodeFilters: React.FC<SCMCodeFiltersProps> = (props: SCMCodeFiltersProps) => {
  const { metaData, onMetadataChange, scmGlobalSettings } = props;
  const unit = get(metaData, ["code_change_size_unit"], scmGlobalSettings.code_change_size_unit);
  const unitText = unit === "lines" ? " lines of code" : " files";

  return (
    <>
      <Form.Item key="code_change_size" label={"Code Change Size"}>
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
                  value={get(metaData, ["code_change_size_small"], scmGlobalSettings.code_change_size_small)}
                  onChange={(e: number) => onMetadataChange?.(e, "code_change_size_small")}
                />
                {unitText}
              </AntText>
              <AntText className="code-change-option">
                {"Medium - less than "}
                <AntInput
                  type="number"
                  value={get(metaData, ["code_change_size_medium"], scmGlobalSettings.code_change_size_medium)}
                  onChange={(e: number) => onMetadataChange?.(e, "code_change_size_medium")}
                />
                {unitText}
              </AntText>
              <AntText className="code-change-option">{"Large - everything else"}</AntText>
            </div>
          </Form.Item>
        </div>
      </Form.Item>
      {!["github_commits_report", "github_commits_single_stat"].includes(props.report) && (
        <Form.Item key="comment-density" label={"PR Code Density"}>
          <div className="comment-density-container">
            <AntText className="comment-density-item">
              {"Shallow - on average less than "}
              <AntInput
                type="number"
                value={get(metaData, ["comment_density_small"], scmGlobalSettings.comment_density_small)}
                onChange={(e: number) => onMetadataChange?.(e, "comment_density_small")}
              />
              {" comment(s) per file"}
            </AntText>
            <AntText className="comment-density-item">
              {"Good - on average less than "}
              <AntInput
                type="number"
                value={get(metaData, ["comment_density_medium"], scmGlobalSettings.comment_density_medium)}
                onChange={(e: number) => onMetadataChange?.(e, "comment_density_medium")}
              />
              {" comment(s) per file"}
            </AntText>
            <AntText className="comment-density-item">{"Heavy - everything else"}</AntText>
          </div>
        </Form.Item>
      )}
    </>
  );
};

export default SCMCodeFilters;
