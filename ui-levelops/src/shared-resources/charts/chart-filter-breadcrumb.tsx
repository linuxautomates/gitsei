import * as React from "react";
import { useEffect } from "react";

interface FilterBreadcrumbProps {
  filters: any;
  setFilters: (id: string, filters: Object) => void;
  widgetId: string;
  setHeight: any;
}

export const FilterBreadcrumbs: React.FC<FilterBreadcrumbProps> = ({ filters, setFilters, widgetId, setHeight }) => {
  const ref = React.createRef<HTMLDivElement>();
  const localFilterClicked = (map_id: string) => {
    if (!map_id) {
      setFilters(widgetId, { localFilters: { parents: [], parent_cicd_job_ids: [] } });
    }
    const filterIndex = filters.parents.findIndex((f: any) => f.cicd_job_id === map_id);
    if (filterIndex !== -1) {
      setFilters(widgetId, {
        localFilters: { parents: filters.parents.slice(0, filterIndex + 1), parent_cicd_job_ids: [map_id] }
      });
    }
  };

  useEffect(() => {
    if (ref && ref.current) {
      setHeight(ref.current.clientHeight);
    }
  }, [ref]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div ref={ref}>
      {filters && filters.parents && filters.parents.length > 0 && (
        <>
          <span className="treemap-breadcrumb" onClick={() => localFilterClicked("")}>
            All
          </span>
          <span>&nbsp;/&nbsp;</span>
        </>
      )}
      {filters &&
        filters.parents &&
        filters.parents.map((parent: any) => (
          <>
            {parent.cicd_job_id !== filters.parents[filters.parents.length - 1].cicd_job_id && (
              <>
                <span className="treemap-breadcrumb" onClick={() => localFilterClicked(parent.cicd_job_id)}>
                  {parent.label}
                </span>
                <span>&nbsp;/&nbsp;</span>
              </>
            )}
            {parent.cicd_job_id === filters.parents[filters.parents.length - 1].cicd_job_id && (
              <span>{parent.label}</span>
            )}
          </>
        ))}
    </div>
  );
};
