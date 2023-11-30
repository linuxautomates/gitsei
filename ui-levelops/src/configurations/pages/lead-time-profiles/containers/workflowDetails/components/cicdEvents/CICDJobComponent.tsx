import { Icon } from "antd";
import { CICDEvents } from "classes/RestWorkflowProfile";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import "./CICDFilterComponent.scss";
import EventJobSelectionModal from "./EventJobSelectionModal";
import "./CICDJobComponent.scss";
import { AntRadio, CustomSelect } from "shared-resources/components";
import { debounce, uniq } from "lodash";

interface CICDJobComponentProps {
  event: CICDEvents;
  titleName: string;
  handleJobsChange: (value: any, jobFlag: any) => void;
  options: any[];
  calculationType: string;
  allowIncludeAllJobs?: boolean;
  otherTabSelectionType?: string;
}

const CICDJobComponent: React.FC<CICDJobComponentProps> = ({
  event,
  titleName,
  handleJobsChange,
  options,
  calculationType,
  allowIncludeAllJobs,
  otherTabSelectionType
}) => {
  
  const [allJobs, setAllJobs] = useState(options.sort((a: { value: string; }, b: { value: string; }) => (event?.values || []).indexOf(b.value) - (event?.values || []).indexOf(a.value)));
  const [jobSelectModal, setjobSelectModal] = useState(false);
  const [selectdJobInModal, setSelectdJobInModal] = useState<any[]>([]);
  const [selectdAllJobFlag, setSelectdAllJobFlag] = useState<string>('MANUALLY');
  const [selectdJobSorting, setSelectdJobSorting] = useState<string>('ALL_JOBS');

  useEffect(() => {
    if (event?.values) setSelectdJobInModal(event?.values);
    if (event?.selectedJob) setSelectdAllJobFlag(event?.selectedJob);
  }, [event?.values, event?.selectedJob]);

  const handleSetJobSelectionModal = (e: any) => {
    setjobSelectModal(true);
  };

  const renderJobEdit = useMemo(() => {
    return (
      <>
        <span className="parameter-job-selected">
          {selectdAllJobFlag === 'ALL'
            ? `All ${titleName} included (${selectdJobInModal.length})`
            : allowIncludeAllJobs
              ? `${titleName} selected manually (${selectdJobInModal.length})`
              : `${selectdJobInModal.length} jobs selected`
          }

        </span>
        <span className="parameter-job-edit" onClick={handleSetJobSelectionModal}>
          <Icon type={"edit"} />
          <span className="parameter-job-edit-text">Edit Selection</span>
        </span>
      </>
    );
  }, [titleName, selectdJobInModal, selectdAllJobFlag, allowIncludeAllJobs, handleSetJobSelectionModal]);

  const handleManualSelectionChange = (value: string[]) => {
    setSelectdJobInModal([...value]);
  };

  const clearSelection = () => {
    setSelectdJobInModal([]);
  };

  const allSelectionHandler = useCallback((filteredData: any[]) => {
    const ids = (filteredData || []).map((option: { label: string; value: string }) => option?.value);
    setSelectdJobInModal([...ids]);
  }, []);

  const selectedPageHandler = useCallback((pageFilters: any, filteredData: any[], allreadySelected: any[]) => {
    const { page, pageSize } = pageFilters;
    const start = page * pageSize - pageSize;
    const end = page * pageSize;
    const ids = filteredData.slice(start, end).map((option: any) => option.value);
    setSelectdJobInModal(uniq([...allreadySelected,...ids]));
  }, [setSelectdJobInModal]);

  const renderJobAdd = useMemo(() => {
    return (
      <span className="parameter-job-color" onClick={handleSetJobSelectionModal}>
        <Icon type="plus-circle" /> Add {titleName}
      </span>
    );
  }, [titleName, handleSetJobSelectionModal]);

  const handleSaveSelection = useCallback(() => {
    handleJobsChange(uniq(selectdJobInModal) || [], selectdAllJobFlag);
    setjobSelectModal(false);
  }, [selectdJobInModal, handleJobsChange, selectdAllJobFlag, setjobSelectModal]);

  const handleCloseModal = useCallback(
    (value: boolean) => {
      setjobSelectModal(value);
      if (event?.values) setSelectdJobInModal([...event?.values]);
    },
    [event?.values, setjobSelectModal, setSelectdJobInModal]
  );

  const handleSaveAllJobFlag = useCallback((values: string) => {
    setSelectdAllJobFlag(values);
    const ids = (allJobs || []).map((option: { label: string; value: string }) => option?.value);
    setSelectdJobInModal([...ids]);
  }, [setSelectdAllJobFlag, allJobs, setSelectdJobInModal]);


  const jobSelectionOptions: any[] = [
    {
      label: `Include all ${titleName}`,
      value: 'ALL',
    },
    {
      label: `Select ${titleName} manually`,
      value: 'MANUALLY',
    },
  ];

  const renderSelectionOptions = useMemo(() => {
    return jobSelectionOptions.map((option: any) => {
      return (
        <AntRadio value={option.value}>{option.label}</AntRadio>
      );
    });
  }, [jobSelectionOptions]);

  const handleJobSelectionFIlterChange = useCallback((value: string) => {
    setSelectdJobSorting(value);
    if (value === 'SELECTED_JOBS') {
      const matchData = allJobs.filter((data) => selectdJobInModal.includes(data.value));
      setAllJobs(matchData);
    } else {
      setAllJobs(options);
    }
  },[setSelectdJobSorting, allJobs, selectdJobInModal, setAllJobs, options])
  
  const jobSelectionSortingOption: any[] = [
    {
      label: `Selected`,
      value: 'SELECTED_JOBS',
    },
    {
      label: `All`,
      value: 'ALL_JOBS',
    },
  ];

  const renderJobSortingOptions = useMemo(() => {
    return (
      <CustomSelect
        dataFilterNameDropdownKey={`job-filter-value-select`}
        className="filter-col-select"
        mode="default"
        style={{ width: 100 }}
        createOption={false}
        labelCase="none"
        labelKey={"label"}
        valueKey={"value"}
        options={jobSelectionSortingOption}
        value={selectdJobSorting || 'ALL_JOBS'}
        onChange={(value: string) => handleJobSelectionFIlterChange(value)}
      />
    )
  }, [jobSelectionSortingOption, selectdJobSorting, handleJobSelectionFIlterChange]);

  const renderJobSelectionModal = useMemo(() => {
    return (
      <EventJobSelectionModal
        selectdJobInModal={selectdJobInModal}
        handleSave={handleSaveSelection}
        handleManualSelection={handleManualSelectionChange}
        visible={jobSelectModal}
        handleCloseModal={handleCloseModal}
        allJobList={allJobs}
        calculationType={calculationType}
        titleName={titleName}
        clearSelection={clearSelection}
        allSelectionHandler={allSelectionHandler}
        selectedPageHandler={selectedPageHandler}
        renderSelectionOptions={renderSelectionOptions}
        handleSelectdAllJobFlag={handleSaveAllJobFlag}
        selectdAllJobFlag={selectdAllJobFlag}
        allowIncludeAllJobs={allowIncludeAllJobs}
        otherTabSelectionType={otherTabSelectionType}
        renderJobSortingOptions={renderJobSortingOptions}
      />
    );
  }, [
    jobSelectModal,
    allJobs,
    selectdJobInModal,
    titleName,
    handleSaveSelection,
    handleManualSelectionChange,
    handleCloseModal,
    calculationType,
    clearSelection,
    selectedPageHandler,
    allSelectionHandler,
    renderJobSortingOptions,
    renderSelectionOptions,
    handleSaveAllJobFlag,
    selectdAllJobFlag,
    allowIncludeAllJobs
  ]);

  return (
    <div className="parameter-job">
      {event?.values && event?.values.length > 0 ? renderJobEdit : renderJobAdd}
      {renderJobSelectionModal}
    </div>
  );
};

export default CICDJobComponent;
