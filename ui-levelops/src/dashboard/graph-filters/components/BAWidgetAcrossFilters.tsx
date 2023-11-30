import { useCategoryOrEpicAcross } from "custom-hooks";
import {
  EPIC_FILTER_KEY,
  TICKET_CATEGORIZATION_SCHEMES_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { get } from "lodash";
import React, { useEffect, useMemo } from "react";
import { useDispatch } from "react-redux";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import CustomSelectWrapper from "shared-resources/components/custom-select/CustomSelectWrapper";

interface BAWidgetAcrossFiltersProps {
  filters: any;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
}

const BAWidgetAcrossFilters: React.FC<BAWidgetAcrossFiltersProps> = (props: BAWidgetAcrossFiltersProps) => {
  const { filters, onFilterValueChange } = props;
  const across = filters["across"];
  const { apiData, apiLoading } = useCategoryOrEpicAcross(filters);
  const dispatch = useDispatch();

  useEffect(() => {
    return () => {
      dispatch(genericRestAPISet({}, "ticket_categorization_scheme", "list", "-1"));
    };
  }, []);

  const getLabel = useMemo(() => {
    if (across === "epic") return "Epic Priority List";
    return "Effort Investment Profile";
  }, [across]);

  const getOptions = useMemo(() => {
    if (across === "epic") {
      return apiData.map(data => {
        if (data?.key) return { label: data?.key, value: data?.key };
      });
    } else {
      return apiData.map(data => {
        if (data?.name && data?.id) return { label: data?.name, value: data?.id };
      });
    }
  }, [across, apiData]);

  const getFilterKey = useMemo(() => {
    if (across === "epic") return EPIC_FILTER_KEY;
    return TICKET_CATEGORIZATION_SCHEMES_KEY;
  }, [across]);

  return (
    <>
      {across === "ticket_category" && (
        <>
          <CustomSelectWrapper
            showArrow={true}
            selectLabel={getLabel}
            value={get(filters, [getFilterKey], undefined)}
            options={getOptions}
            mode={"default"}
            allowClear={true}
            required
            onChange={(value: any) => onFilterValueChange(value, getFilterKey)}
          />
        </>
      )}
    </>
  );
};

export default BAWidgetAcrossFilters;
