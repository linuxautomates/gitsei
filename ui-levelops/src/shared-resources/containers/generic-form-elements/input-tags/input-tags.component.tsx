import React, { useState } from "react";
import { AntInputComponent as AntInput } from "shared-resources/components/ant-input/ant-input.component";
import { AntTagComponent as AntTag } from "shared-resources/components/ant-tag/ant-tag.component";
import { default as AntIcon } from "shared-resources/components/ant-icon/ant-icon.component";

interface InputTagsComponentProps {
  onChange: (tags: string[]) => void;
  value: string[];
}

const InputTagsComponent: React.FC<InputTagsComponentProps> = (props: InputTagsComponentProps) => {
  const [showInputField, setShowInputField] = useState(false);
  const [newTagValue, setNewTagValue] = useState<undefined | string>(undefined);

  const handleClose = (tag: string) => {
    const filteredTags = props.value.filter((v: string) => v !== tag);
    props.onChange(filteredTags);
  };

  const handleInputChange = (e: any) => {
    setNewTagValue(e.target.value);
  };

  const handleInputConfirm = () => {
    if (!newTagValue) {
      return;
    }
    const value = props.value;
    value.push(newTagValue);
    setNewTagValue(undefined);
    setShowInputField(false);
    props.onChange(value);
  };

  const showInput = () => setShowInputField(true);

  const renderAddTag = () => {
    if (!showInputField) {
      return (
        <AntTag className="site-tag-plus" onClick={showInput}>
          <AntIcon type="plus" /> New Tag
        </AntTag>
      );
    }
    return (
      <AntInput
        type="text"
        size="small"
        className="tag-input"
        data-testid="tag-input"
        value={newTagValue}
        onChange={handleInputChange}
        onBlur={handleInputConfirm}
        onPressEnter={handleInputConfirm}
      />
    );
  };

  const renderTag = (tag: string) => (
    <AntTag key={tag} closable onClose={() => handleClose(tag)}>
      <span>{tag}</span>
    </AntTag>
  );

  const tags = props.value || [];
  return (
    <div>
      {tags.map((tag: string) => renderTag(tag))}
      {renderAddTag()}
    </div>
  );
};

export default React.memo(InputTagsComponent);
