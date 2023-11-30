import { Form } from "antd";
import { useSCMGlobalSettings } from "custom-hooks/useSCMGlobalSettings";
import { get } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import React from "react";
import { AntInput, AntText } from "shared-resources/components";
import "./../../components/SCMCodeFilters/SCMCodeFilters.scss";

interface SCMCommentDensityFilterWrapperProps {
  metadata: any;
  handleMetadataChange?: (value: any, type: any) => void;
  filterProps: LevelOpsFilter;
}
const ScmCommentDensityFilterWrapper: React.FC<SCMCommentDensityFilterWrapperProps> = (
  props: SCMCommentDensityFilterWrapperProps
) => {
  const { settings } = useSCMGlobalSettings();

  const { handleMetadataChange: onMetadataChange, filterProps, metadata: metaData } = props;
  const { label, beKey } = filterProps;

  return (
    <>
      <Form.Item key={beKey} label={label}>
        <div className="comment-density-container">
          <AntText className="comment-density-item">
            {"Shallow - on average less than "}
            <AntInput
              type="number"
              value={get(metaData, ["comment_density_small"], settings.comment_density_small)}
              onChange={(e: number) => onMetadataChange?.(e, "comment_density_small")}
            />
            {" comment(s) per file"}
          </AntText>
          <AntText className="comment-density-item">
            {"Good - on average less than "}
            <AntInput
              type="number"
              value={get(metaData, ["comment_density_medium"], settings.comment_density_medium)}
              onChange={(e: number) => onMetadataChange?.(e, "comment_density_medium")}
            />
            {" comment(s) per file"}
          </AntText>
          <AntText className="comment-density-item">{"Heavy - everything else"}</AntText>
        </div>
      </Form.Item>
    </>
  );
};

export default ScmCommentDensityFilterWrapper;
