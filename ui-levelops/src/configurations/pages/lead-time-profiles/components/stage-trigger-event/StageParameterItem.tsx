import React, { useCallback, useState } from "react";
import { uniq } from "lodash";
import { AntButton, AntInput, AntTag, AntIcon } from "shared-resources/components";

interface StageParameterItemProps {
  param: string;
  values: string[];
  onNameChange: (oldValue: any, newValue: any) => void;
  onValuesChange: (key: string, values: any) => void;
  onDelete: (key: string) => void;
}

const StageParameterItem: React.FC<StageParameterItemProps> = props => {
  const { param, values, onNameChange, onValuesChange, onDelete } = props;

  const [text, setText] = useState("");
  const [inputVisible, setInputVisible] = useState(false);

  const handleNameChange = useCallback(
    (e: any) => {
      const value = e.target.value;
      onNameChange(param, value);
    },
    [param, onNameChange]
  );

  const handleInputChange = useCallback((e: any) => {
    const value = e.target.value;
    setText(value);
  }, []);

  const handleTagAdd = useCallback(
    (e: any) => {
      const value = e.currentTarget.value;
      let newValues = values || [];
      if (value) {
        newValues.push(value);
        onValuesChange(param, uniq(newValues));
        setInputVisible(false);
        setText("");
      }
    },
    [param, values]
  );

  const handleTagClose = useCallback(
    (index: number) => {
      return () => {
        let newValues = values || [];
        newValues.splice(index, 1);
        onValuesChange(param, newValues);
      };
    },
    [param, values]
  );

  const handleTagClick = useCallback(() => setInputVisible(true), []);

  return (
    <div className="parameter-item">
      <div className="parameter-content">
        <AntInput value={param} className="parameter-type" placeholder="Select Parameter" onChange={handleNameChange} />
        <span>=</span>
        <div className="parameter-value">
          {values.map((value: any, index: number) => (
            <AntTag key={index} closable={true} onClose={handleTagClose(index)}>
              {value}
            </AntTag>
          ))}
          {!inputVisible && (
            <AntTag onClick={handleTagClick} style={{ background: "transparent", borderStyle: "dashed" }}>
              <AntIcon type={"plus"} /> New Value
            </AntTag>
          )}
          {inputVisible && <AntInput onPressEnter={handleTagAdd} value={text} onChange={handleInputChange} />}
        </div>
      </div>
      <AntButton type="ghost" className="delete-btn mx-5" icon="delete" onClick={() => onDelete(param)} />
    </div>
  );
};

export default StageParameterItem;
