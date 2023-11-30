import React, { useCallback } from "react";
import { uniq } from "lodash";
import { AntButton, AntInput, AntSelect } from "shared-resources/components";
import "./parameterItem.scss";

interface ParameterItemProps {
  param: string;
  values: string[];
  onNameChange: (oldValue: any, newValue: any) => void;
  onValuesChange: (key: string, values: any) => void;
  onDelete: (key: string) => void;
}

const ParameterItem: React.FC<ParameterItemProps> = props => {
  const { param, values, onNameChange, onValuesChange, onDelete } = props;

  const handleNameChange = useCallback(
    (e: any) => {
      const value = e.target.value;
      onNameChange(param, value);
    },
    [param, onNameChange]
  );

  const handleTagAdd = useCallback(
    (values: string[]) => {
      onValuesChange(param, uniq(values));
    },
    [param, values]
  );

  return (
    <div className="new-parameter-item">
      <div className="parameter-content">
        <AntInput value={param} className="parameter-type" placeholder="Select Parameter" onChange={handleNameChange} />
        <span>=</span>
        <div className="parameter-value">
          <AntSelect
            mode="tags"
            className="values-selector"
            value={values}
            placeholder={"Add values"}
            options={[]}
            onChange={handleTagAdd}
            maxTagCount={2}
          />
        </div>
      </div>
      <AntButton type="ghost" className="delete-btn mx-5" icon="delete" onClick={() => onDelete(param)} />
    </div>
  );
};

export default ParameterItem;
