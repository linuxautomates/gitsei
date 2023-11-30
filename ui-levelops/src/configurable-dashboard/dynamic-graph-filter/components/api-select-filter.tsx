import { DynamicGraphFilter } from "dashboard/constants/applications/levelops.application";
import { isEqual } from "lodash";
import React, { useEffect, useRef } from "react";
import { CustomFormItemLabel } from "shared-resources/components";
import { v1 as uuid } from "uuid";
import SelectRestapi from "shared-resources/helpers/select-restapi/select-restapi";

interface APISelectFilterProps {
  filter: DynamicGraphFilter;
  onChange: (v: any) => void;
  value: any;
}

export const APISelectFilter: React.FC<APISelectFilterProps> = props => {
  const { filter } = props;

  const uuidRef = useRef<string>(uuid());
  const labelRef = useRef<string>(filter.label);

  useEffect(() => {
    if (!isEqual(labelRef.current, filter.label)) {
      uuidRef.current = uuid();
      labelRef.current = filter.label;
    }
  }, [filter.label]);

  return (
    <div key={uuidRef.current}>
      <CustomFormItemLabel label={filter.label} />
      <SelectRestapi
        style={{ width: "100%" }}
        placeholder={filter.label}
        uri={filter.uri as any}
        searchField={filter.searchField as any}
        defaultValue={props.value || null}
        additionalOptions={filter.options || []}
        value={props.value}
        onChange={props.onChange}
        createOption={false}
        method="list"
        mode={filter.filterType === "apiMultiSelect" ? "multiple" : "default"}
      />
    </div>
  );
};

export default APISelectFilter;
