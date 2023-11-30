import React, { useCallback, useEffect, useState, useMemo } from "react";
import { AntCol, EditableTag } from "../../../../components";
import { FilterHeader } from "./header/filter-header";
import { setFilterState } from "../../helper";

interface TagsFilterProps {
  filter: any;
  onTagsChange: (v: any) => void;
}

export const TagsFilter: React.FC<TagsFilterProps> = props => {
  const { filter } = props;

  const [value, setValue] = useState<any[]>([]);

  const handleTagsChange = useCallback(
    (tags: any[]) => {
      // setValue(tags);
      props.onTagsChange(tags);
    },
    [props.onTagsChange]
  );

  useEffect(() => {
    const val = filter.selected;
    setFilterState(value, val, setValue);
  }, [filter]);

  const style = useMemo(() => ({ width: "100%" }), []);

  return (
    <AntCol className="gutter-row" span={filter.span ? filter.span : 4}>
      <FilterHeader label={filter.label} />
      <EditableTag tagLabel={"Add Epic"} style={style} tags={value} onTagsChange={handleTagsChange} />
    </AntCol>
  );
};

export default React.memo(TagsFilter);
