import React, { useEffect, useState } from "react";
import { RestVelocityConfigStage } from "../../../../../classes/RestVelocityConfigs";
import { AntCol, AntForm, AntRow } from "shared-resources/components";
import APIFiltersComponent from "dashboard/graph-filters/components/APIFilters";
import { useDispatch } from "react-redux";
import { restApiSelectGenericList } from "reduxConfigs/actions/restapi";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { get } from "lodash";
import { StageEndOptions } from "classes/StageEndOptions";

interface StageDescriptionProps {
  stage: RestVelocityConfigStage;
  onChange: (stage: any) => void;
}
const StageAdditionalInformation: React.FC<StageDescriptionProps> = props => {
  const { stage, onChange } = props;
  const [partialFilterError, setPartialFilterError] = useState<any>({});
  const [activePopKey, setActivePopKey] = useState<string | undefined>(undefined);
  const [list, setList] = useState<[]>([]);
  const customProps = {
    label: "Destination branch",
    classNames: "custom-width-popup",
    showRegex: true,
    partialMatchKey: "target_branches_regex",
    key: "target_branches",
    hidePartialSelect: true
  };
  const uri = "github_prs_filter_values";
  const dispatch = useDispatch();

  const genericSelector = useParamSelector(getGenericRestAPISelector, {
    uri: uri,
    method: "list",
    uuid: "0"
  });

  const data = get(genericSelector, ["data", "records"], undefined);

  useEffect(() => {
    dispatch(
      restApiSelectGenericList(
        false,
        uri,
        "list",
        { filter: { integration_ids: [] }, fields: ["target_branch"] },
        null,
        "0"
      )
    );
    if (stage.event?.type === StageEndOptions.SCM_PR_MERGED) {
      if (!stage?.event?.params?.[customProps.key] && !stage?.event?.params?.[customProps.partialMatchKey]) {
        stage.event.params = {};
      }
      onChange(stage.json);
    }
  }, []);

  useEffect(() => {
    setList(data);
  }, [data]);

  const handleFilterValueChange = (...params: any) => {
    (stage as any).event.params = {
      [customProps.key]: params[0]
    };
    onChange(stage.json);
  };

  const handlePartialFilters = (key: string, value: any) => {
    // Note: Below changes will get removed once BE added partial match support for velocity configs
    // const { filters, error } = buildPartialQuery(
    //   stage?.event?.params.partial_match || {},
    //   key,
    //   value,
    //   "github_prs_report"
    // );
    // if (!!error) {
    //   setPartialFilterError((prev: any) => ({ ...prev, [key]: error }));
    // } else {
    //   setPartialFilterError((prev: any) => ({ ...prev, [key]: undefined }));
    // }
    if (value === undefined) {
      (stage as any).event.params = {
        [customProps.key]: []
      };
    } else {
      (stage as any).event.params = {
        [customProps.partialMatchKey]: [value[customProps.partialMatchKey]] || []
      };
    }
    onChange(stage.json);
  };

  return stage?.event?.params ? (
    <AntRow>
      <AntCol span={24} className="stage-description-wrapper">
        {stage.event?.type === StageEndOptions.SCM_PR_MERGED && (
          <AntForm layout="vertical">
            <APIFiltersComponent
              data={list}
              filters={stage?.event?.params}
              supportExcludeFilters={false}
              supportPartialStringFilters={true}
              handlePartialValueChange={handlePartialFilters}
              handleFilterValueChange={handleFilterValueChange}
              handleSwitchValueChange={handleFilterValueChange}
              partialFilterError={partialFilterError}
              hasNext={false}
              reportType={""}
              activePopkey={activePopKey}
              handleActivePopkey={key => setActivePopKey(key)}
              customProps={customProps}
            />
          </AntForm>
        )}
      </AntCol>
    </AntRow>
  ) : null;
};

export default StageAdditionalInformation;
