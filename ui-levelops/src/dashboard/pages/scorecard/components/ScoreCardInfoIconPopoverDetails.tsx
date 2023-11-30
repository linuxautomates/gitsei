import React, { useEffect, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Collapse, Icon } from "antd";
import cx from "classnames";
import queryString from "query-string";
import { useLocation } from "react-router-dom";

import { trellisProfilesListReadAction } from "reduxConfigs/actions/restapi/trellisProfileActions";
import { TrellisProfilesListState } from "reduxConfigs/reducers/trellisProfileReducer";
import { trellisProfileListSelector } from "reduxConfigs/selectors/trellisProfileSelectors";
import Loader from "components/Loader/Loader";
import "./ScoreCardInfoIconPopoverDetails.scss";
import ScorePopoverLegends from "./ScorePopoverLegends";

const { Panel } = Collapse;

interface ScoreCardInfoIconPopoverDetailsProps {
  section?: any;
  profile?: Record<string, any>;
  isUseQueryParamsOU?: boolean;
}

const customPanelStyle = {
  background: "#f7f7f7",
  borderRadius: 4,
  marginBottom: 16,
  border: 0,
  overflow: "hidden"
};

const ScoreCardInfoIconPopoverDetails: React.FC<ScoreCardInfoIconPopoverDetailsProps> = (
  props: ScoreCardInfoIconPopoverDetailsProps
) => {
  const { section, profile, isUseQueryParamsOU } = props;
  const dispatch = useDispatch();
  const location = useLocation();
  const params = queryString.parse(location.search);
  const trellisProfilesState: TrellisProfilesListState = useSelector(trellisProfileListSelector);

  useEffect(() => {
    // If data already present don't call API
    if (!trellisProfilesState?.data) {
      dispatch(trellisProfilesListReadAction());
    }
  }, [profile?.id]);

  const currentFeature = useMemo(() => {
    // If isUseQueryParamsOU flag true then get profile from associated_ou_ref_ids of all profile list used in widget showing widget level info hover information
    if (isUseQueryParamsOU) {
      return trellisProfilesState?.data?.records
        .find((sec: any) => (sec?.associated_ou_ref_ids || []).indexOf(params?.OU) !== -1)
        ?.sections?.find(feature => feature?.name === section?.name);
    }
    return trellisProfilesState?.data?.records
      .find(sec => sec.id === profile?.id)
      ?.sections?.find(feature => feature?.name === section?.name);
  }, [section?.name, trellisProfilesState, isUseQueryParamsOU]);

  return (
    <div className="dev-section-info-popup-container">
      {profile?.name && <div className="title">Profile : {profile?.name}</div>}
      <div className="sub-title">Enabled factors and weights</div>
      <Collapse
        bordered={false}
        defaultActiveKey={[0]}
        expandIcon={({ isActive }) => <Icon type="caret-right" rotate={isActive ? 90 : 0} />}>
        {trellisProfilesState?.isLoading && <Loader />}
        {trellisProfilesState?.data &&
          (currentFeature?.features || []).map((sectionData: any, index: number) => {
            const value = [sectionData?.lower_limit_percentage || 0, sectionData?.upper_limit_percentage || 0];
            const maxScore = sectionData?.max_value || 0;
            const asc = sectionData?.slow_to_good_is_ascending || false;
            const unit = sectionData?.feature_unit;
            return (
              sectionData.enabled && (
                <Panel key={index} header={sectionData?.name} style={customPanelStyle}>
                  <ScorePopoverLegends value={value} maxScore={maxScore} asc={asc} unit={unit} />
                </Panel>
              )
            );
          })}
      </Collapse>
    </div>
  );
};

export default ScoreCardInfoIconPopoverDetails;
